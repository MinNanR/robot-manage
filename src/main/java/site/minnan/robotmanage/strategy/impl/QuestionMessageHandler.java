package site.minnan.robotmanage.strategy.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.util.ReUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import com.aliyun.oss.OSS;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import site.minnan.robotmanage.entity.aggregate.Answer;
import site.minnan.robotmanage.entity.aggregate.Auth;
import site.minnan.robotmanage.entity.aggregate.Question;
import site.minnan.robotmanage.entity.dao.AnswerRepository;
import site.minnan.robotmanage.entity.dao.AuthRepository;
import site.minnan.robotmanage.entity.dao.QuestionGroupRepository;
import site.minnan.robotmanage.entity.dao.QuestionRepository;
import site.minnan.robotmanage.entity.dto.MessageDTO;
import site.minnan.robotmanage.infrastructure.exception.EntityNotExistException;
import site.minnan.robotmanage.infrastructure.utils.BotSessionUtil;
import site.minnan.robotmanage.strategy.MessageHandler;

import java.io.InputStream;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 词条消息处理
 *
 * @author Minnan on 2024/01/18
 */
@Component("question")
public class QuestionMessageHandler implements MessageHandler {


    public QuestionMessageHandler(OSS oss, QuestionRepository questionRepository, AnswerRepository answerRepository, QuestionGroupRepository questionGroupRepository, BotSessionUtil botSessionUtil) {
        this.oss = oss;
        this.questionRepository = questionRepository;
        this.answerRepository = answerRepository;
        this.questionGroupRepository = questionGroupRepository;
        this.botSessionUtil = botSessionUtil;
    }

    private OSS oss;

    private QuestionRepository questionRepository;

    private AnswerRepository answerRepository;

    private QuestionGroupRepository questionGroupRepository;

    private BotSessionUtil botSessionUtil;

    @Autowired
    private AuthRepository authRepository;


    private static final String bucketName = "link-server";

    public static final String baseUrl = "https://minnan.site:2005/";

    @Value("${groups}")
    private String[] serviceGroups;

    /**
     * 处理消息
     *
     * @param dto
     * @return
     */
    @Override
    @Transactional
    public Optional<String> handleMessage(MessageDTO dto) {
        String message = dto.getRawMessage();
        if (message.startsWith("添加问题")) {
            return addQuestion(dto);
        } else if (message.startsWith("删除问题")) {
            return deleteQuestion(dto);
        } else if (message.startsWith("删除答案")) {
            return deleteAnswer(dto);
        } else if (message.startsWith("模糊查询问题")) {
            return fuzzyQueryQuestion(dto);
        } else if (message.startsWith("查询问题")) {
            return queryQuestion(dto);
        }
        return Optional.empty();
    }

    private Optional<String> addQuestion(MessageDTO dto) {
        String now = DateTime.now().toString("yyyy-MM-dd HH:mm:ss");
        String message = dto.getRawMessage();
        String[] split = message.substring(4).split("答");
        String questionContent = split[0].strip();
        String answerContent = Arrays.stream(split).skip(1).collect(Collectors.joining("答"));
        String groupId = dto.getGroupId();
        String userId = dto.getSender().userId();
        Question question = questionRepository.findByContentIgnoreCaseAndGroupIdAndWhetherDeleteIs(questionContent, groupId, 0);
        if (question == null) {
            question = new Question();
            question.setGroupId(groupId);
            question.setShare(0);
            question.setContent(questionContent);
            question.setWhetherDelete(0);
            int groupNumber = 1;
            for (String group : serviceGroups) {
                if (Objects.equals(group, groupId)) {
                    break;
                }
                groupNumber = groupNumber << 1;
            }
            //问题默认的掩码为群编码
            question.setGroupMask(groupNumber);
            question.setUpdater("QQ:" + userId);
            questionRepository.save(question);
            question = questionRepository.findByContentIgnoreCaseAndGroupIdAndWhetherDeleteIs(questionContent, groupId, 0);
        }
        question.setUpdater("QQ:" + userId);
        question.setUpdateTime(now);
        Integer questionId = question.getId();
        answerContent = enhanceAnswer(answerContent);

        Answer answer = new Answer();
        answer.setQuestionId(questionId);
        answer.setContent(answerContent);
        answer.setWhetherDelete(0);
        answer.setUpdater("QQ:" + userId);
        answer.setUpdateTime(now);
        answerRepository.save(answer);
        String reply = "添加问题成功，问题id：%d，\n如词条需要共享到其他群，请输入展示码(所有群都展示请输入127)，输入-1结束添加问题操作".formatted(questionId);

        botSessionUtil.startSession(groupId, userId, messageDTO -> updateQuestionGroupMask(messageDTO, questionId));
        return Optional.of(reply);
    }

    /**
     * 修改问题展示群掩码
     *
     * @param dto
     * @param questionId
     * @return
     */
    private Optional<String> updateQuestionGroupMask(MessageDTO dto, Integer questionId) {
        String message = dto.getRawMessage();
        try {
            int groupMask = Integer.parseInt(message);
            Optional<Question> questionOpt = questionRepository.findById(questionId);
            Question question = questionOpt.get();
            question.setGroupMask(groupMask);
            question.setUpdater("QQ:" + dto.getSender().userId());
            question.setUpdateTime(DateTime.now().toString("yyyy-MM-dd HH:mm:ss"));
            questionRepository.save(question);
            return Optional.of("更新展示码成功，如需更改请重新输入，无需更改输入-1");
        } catch (NumberFormatException e) {
            return Optional.of("请输入正确的展示码");
        }
    }

    private Optional<String> deleteQuestion(MessageDTO dto) {
        String message = dto.getRawMessage();
        String content = message.substring(4).strip();

        Integer questionId = Integer.parseInt(content);

        Optional<Question> questionOpt = questionRepository.findById(questionId);
        Question question = questionOpt.orElseThrow(() -> new EntityNotExistException("问题不存在"));
        question.setWhetherDelete(1);
        questionRepository.save(question);

//        questionRepository.deleteById(questionId);

        List<Answer> answers = answerRepository.findAnswerByQuestionIdIn(Collections.singleton(questionId));
        if (answers.isEmpty()) {
            return Optional.of("删除成功");
        }
        answers.forEach(e -> e.setWhetherDelete(1));
        answerRepository.saveAll(answers);
//        answerRepository.deleteByQuestionId(questionId);

        return Optional.of("删除成功");
    }

    private Optional<String> deleteAnswer(MessageDTO dto) {
        String message = dto.getRawMessage();
        String content = message.substring(4).strip();

        Integer answerId = Integer.parseInt(content);

        Optional<Answer> answerOpt = answerRepository.findById(answerId);
        Answer answer = answerOpt.get();
        answer.setWhetherDelete(1);
//        answerRepository.deleteById(answerId);
        answerRepository.save(answer);

        return Optional.of("删除成功");
    }

    private Optional<String> fuzzyQueryQuestion(MessageDTO dto) {
        String content = dto.getRawMessage().substring(6).strip();
        String groupId = dto.getGroupId();

        String queryContent = "%" + content + "%";
        List<Question> questions = questionRepository.findAllByContentLikeIgnoreCaseAndGroupId(queryContent, groupId);
        questions.removeIf(e -> Objects.equals(e.getWhetherDelete(), 1));

        if (CollectionUtil.isEmpty(questions)) {
            return Optional.of("无匹配问题");
        }
        String replyContent = questions.stream().map(e -> "问题id：%d，词条：%s".formatted(e.getId(), e.getContent())).collect(Collectors.joining("\n"));
        String reply = "匹配到以下问题：\n" + replyContent;
        return Optional.of(reply);
    }

    /**
     * 查询问题
     *
     * @param dto
     * @return
     */
    private Optional<String> queryQuestion(MessageDTO dto) {
        String message = dto.getRawMessage();
        String queryContent = message.replace("查询问题", "");
        String groupId = dto.getGroupId();
        String userId = dto.getSender().userId();

        Question question = questionRepository.findByContentIgnoreCaseAndGroupIdAndWhetherDeleteIs(queryContent, groupId, 0);

        if (question == null) {
            return Optional.of("无匹配问题");
        }

        Integer questionId = question.getId();

        List<Answer> answerList = answerRepository.findAnswerByQuestionIdInAndWhetherDeleteIs(Collections.singleton(questionId), 0);
        int answerCount = answerList.size();
        String reply = """
                问题内容：%s
                问题id：%d
                关联答案数量：%d
                展示码：%d,
                查看答案输入1，删除问题输入2，修改展示码输入3:{展示码}，输入-1结束
                """.formatted(question.getContent(), questionId, answerCount, question.getGroupMask());

        botSessionUtil.startSession(groupId, userId, e -> questionQueryOperate(e, questionId, answerList));

        return Optional.of(reply);
    }

    private String enhanceAnswer(String answer) {
        Pattern imagePattern = Pattern.compile("\\[CQ:image,.*?]");
        Pattern urlPattern = Pattern.compile("url=(.*?)[;\\]]");
        Pattern namePattern = Pattern.compile("file=(.*?)\\.image");
        String today = DateTime.now().toString("yyyyMMdd");

        List<String> imageResultList = ReUtil.findAllGroup0(imagePattern, answer);
        for (String imageResult : imageResultList) {
            List<String> urlResult = ReUtil.findAllGroup1(urlPattern, imageResult);
            List<String> nameResult = ReUtil.findAllGroup1(namePattern, imageResult);
            String url = urlResult.get(0);
            String name = nameResult.get(0);
            String ossKey = "rot/%s/%s.png".formatted(today, name);
            String newUrl = baseUrl + ossKey;
            url = ReUtil.replaceAll(url, "%([^0-9A-Za-z])", "%25$1");
            HttpRequest request = HttpUtil.createGet(url);
            HttpResponse response = request.execute();
            InputStream stream = response.bodyStream();
            oss.putObject(bucketName, ossKey, stream);
            answer = answer.replace(imageResult, "[CQ:image,file=%s,subType=0]".formatted(newUrl));
        }


        return answer;
    }

    private static final int OPERATE_REFER = 1;//查看操作
    private static final int OPERATE_DELETE = 2;//删除操作

    private Optional<String> questionQueryOperate(MessageDTO dto, Integer questionId, List<Answer> answerList) {
        String message = dto.getRawMessage();
        String groupId = dto.getGroupId();
        String userId = dto.getSender().userId();
        if (message.equals("1")) {
            Answer answer = answerList.get(0);
            String reply = """
                    答案id：%d,
                    答案内容：%s,
                    查看下一条输入1，删除答案输入2，输入-1结束
                    """.formatted(answer.getId(), answer.getContent());

            botSessionUtil.updateSessionMessageHandler(groupId, userId, e -> iterateAnswer(e, answerList, 0, OPERATE_REFER));
            return Optional.of(reply);
        } else if ("2".equals(message)) {
            Auth authObj = authRepository.findByUserIdAndGroupId(userId, groupId);
            int auth = authObj == null ? 0 : authObj.getAuthNumber();
            if ((auth & 0b10000) == 0) {
                return Optional.of("当前用户无删除问题权限");
            }
            //删除问题
            Optional<Question> questionOpt = questionRepository.findById(questionId);
            questionOpt.ifPresent(e -> {
                e.setWhetherDelete(1);
                e.setUpdater("QQ:" + userId);
                e.setUpdateTime(DateTime.now().toString("yyyy-MM-dd HH:mm:ss"));
                questionRepository.save(e);
            });
            //删除问题后没有后续操作
            botSessionUtil.endSession(groupId, userId);
            return Optional.of("删除问题成功");
        } else if (ReUtil.isMatch("3:(.*)", message)) {
            //修改展示掩码
            String[] messageSplit = message.split("[:：]");
            if (messageSplit.length < 2) {
                return Optional.of("请输入正确的展示码");
            }
            try {
                int groupMask = Integer.parseInt(messageSplit[1]);
                Optional<Question> question = questionRepository.findById(questionId);
                question.ifPresent(e -> {
                    e.setGroupMask(groupMask);
                    e.setUpdater("QQ:" + userId);
                    e.setUpdateTime(DateTime.now().toString("yyyy-MM-dd HH:mm:ss"));
                    questionRepository.save(e);
                });
                String replay = """
                        问题id:%d，展示码已修改为：%d
                        查看答案输入1，删除问题输入2，修改展示码输入3:{展示码}，输入-1结束
                        """.formatted(questionId, groupMask);
                return Optional.of(replay);
            } catch (NumberFormatException e) {
                return Optional.of("请输入正确的展示码");
            }
        }

        return Optional.empty();
    }

    /**
     * 答案迭代回复
     *
     * @param messageDTO      消息
     * @param answerList      答案列表
     * @param indexCur        游标
     * @param lastOperateFlag 上一个操作标记，
     * @see QuestionMessageHandler#OPERATE_REFER
     * @see QuestionMessageHandler#OPERATE_DELETE
     * @return
     */
    private Optional<String> iterateAnswer(MessageDTO messageDTO, List<Answer> answerList, int indexCur, int lastOperateFlag) {
        String message = messageDTO.getRawMessage();
        String groupId = messageDTO.getGroupId();
        String userId = messageDTO.getSender().userId();

        if ("1".equals(message)) {
            if (indexCur == answerList.size() - 1) {
                return Optional.of("已是最后一个答案");
            }
            //上一个是查看操作，则游标下移，如果是删除操作，则游标不下移（删除时游标已经下移）
            int currentIndex = lastOperateFlag == OPERATE_REFER ? indexCur + 1 : indexCur;
            Answer answer = answerList.get(currentIndex);
            String reply = """
                    答案id：%d
                    答案内容：%s
                    查看下一条输入1，删除答案输入2，输入-1结束
                    """.formatted(answer.getId(), answer.getContent());
            botSessionUtil.updateSessionMessageHandler(groupId, userId, dto -> iterateAnswer(dto, answerList, currentIndex, OPERATE_REFER));
            return Optional.of(reply);
        } else if ("2".equals(message)) {
            Auth authObj = authRepository.findByUserIdAndGroupId(userId, groupId);
            int auth = authObj == null ? 0 : authObj.getAuthNumber();
            if ((auth & 0b10000) == 0) {
                return Optional.of("当前用户无删除答案权限");
            }
            Answer answer = answerList.get(indexCur);
            answer.setWhetherDelete(1);
            answer.setUpdater("QQ:" + userId);
            answer.setUpdateTime(DateTime.now().toString("yyyy-MM-dd HH:mm:ss"));
            answerRepository.save(answer);
            //游标下移
            int currentIndex = indexCur + 1;
            botSessionUtil.updateSessionMessageHandler(groupId, userId, dto -> iterateAnswer(dto, answerList, currentIndex, OPERATE_DELETE));
            String reply = """
                    答案%d已删除
                    查看下一条输入1，删除答案输入2，输入-1结束
                    """.formatted(answer.getId());

            return Optional.of(reply);
        } else {
            return Optional.of("请输入正确的操作码");
        }

    }


}
