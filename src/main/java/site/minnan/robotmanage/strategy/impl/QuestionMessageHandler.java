package site.minnan.robotmanage.strategy.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.util.ReUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import com.aliyun.oss.OSS;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import site.minnan.robotmanage.entity.aggregate.Answer;
import site.minnan.robotmanage.entity.aggregate.Question;
import site.minnan.robotmanage.entity.dao.AnswerRepository;
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
        String queryContent = message.replace("查询问题", "").strip();
        String groupId = dto.getGroupId();

        Question question = questionRepository.findByContentIgnoreCaseAndGroupIdAndWhetherDeleteIs(queryContent, groupId, 0);

        if (question == null) {
            return Optional.of("无匹配问题");
        }

        Integer questionId = question.getId();

        List<Answer> answerList = answerRepository.findAnswerByQuestionIdInAndWhetherDeleteIs(Collections.singleton(questionId), 0);

        StringBuilder content = new StringBuilder();
        content.append("**问题：**").append(question.getContent()).append('\n')
                .append("问题ID：**").append(questionId).append("**\n")
                .append("答案数量：**").append(answerList.size()).append("**\n")
                .append("展示码：**").append(question.getGroupMask()).append("**");

        if (CollectionUtil.isNotEmpty(answerList)) {
            for (int i = 0; i < answerList.size(); i++) {
                Answer answer = answerList.get(i);
                content.append("\n\n")
                        .append("**答案").append(i + 1).append("**（答案ID：").append(answer.getId()).append("）\n\n")
                        .append(toMarkdownContent(answer.getContent()));
            }
        } else {
            content.append("\n\n暂无答案");
        }

        content.append("\n\n---\n")
                .append("删除问题请发送：`删除问题").append(questionId).append("`\n")
                .append("删除答案请发送：`删除答案{答案id}`");

        return Optional.of(markdownWrapper(content.toString()));
    }

    /**
     * 将答案内容中的[CQ:image]图片片段转换为markdown图片语法，
     * 使多个图片能够正常排版展示
     *
     * @param answer 答案内容
     * @return 转换后的内容
     */
    private String toMarkdownContent(String answer) {
        Pattern imagePattern = Pattern.compile("\\[CQ:image,.*?]");
        List<String> imageResultList = ReUtil.findAllGroup0(imagePattern, answer);
        for (String imageResult : imageResultList) {
            String imageUrl = extractImageUrl(imageResult);
            if (imageUrl != null) {
                answer = answer.replace(imageResult, "![text#500px #500px](%s)".formatted(imageUrl));
            }
        }
        return answer;
    }

    /**
     * 从[CQ:image]片段中提取图片地址，优先取url参数，其次取http开头的file参数
     *
     * @param cqImage CQ图片片段
     * @return 图片地址，无法解析时返回null
     */
    private String extractImageUrl(String cqImage) {
        List<String> urlResult = ReUtil.findAllGroup1("url=(.*?)[;\\]]", cqImage);
        if (CollectionUtil.isNotEmpty(urlResult)) {
            return urlResult.get(0);
        }
        List<String> fileResult = ReUtil.findAllGroup1("file=(.*?)[;\\]]", cqImage);
        if (CollectionUtil.isNotEmpty(fileResult)) {
            String file = fileResult.get(0);
            if (file.startsWith("http")) {
                return file;
            }
        }
        return null;
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

}
