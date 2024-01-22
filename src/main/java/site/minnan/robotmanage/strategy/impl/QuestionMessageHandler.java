package site.minnan.robotmanage.strategy.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.lang.Console;
import cn.hutool.core.util.ReUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import com.aliyun.oss.OSS;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import site.minnan.robotmanage.entity.aggregate.Answer;
import site.minnan.robotmanage.entity.aggregate.Question;
import site.minnan.robotmanage.entity.dao.AnswerRepository;
import site.minnan.robotmanage.entity.dao.QuestionRepository;
import site.minnan.robotmanage.entity.dto.MessageDTO;
import site.minnan.robotmanage.strategy.MessageHandler;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 词条消息处理
 *
 * @author Minnan on 2024/01/18
 */
@Service("question")
public class QuestionMessageHandler implements MessageHandler {


    public QuestionMessageHandler(OSS oss, QuestionRepository questionRepository, AnswerRepository answerRepository) {
        this.oss = oss;
        this.questionRepository = questionRepository;
        this.answerRepository = answerRepository;
    }

    private OSS oss;

    private QuestionRepository questionRepository;

    private AnswerRepository answerRepository;


    private static final String bucketName = "link-server";

    public static final String baseUrl = "https://minnan.site:2005/";

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
        }
        return Optional.empty();
    }

    private Optional<String> addQuestion(MessageDTO dto) {
        String message = dto.getRawMessage();
        String[] split = message.substring(4).split("答");
        String questionContent = split[0].strip();
        String answerContent = Arrays.stream(split).skip(1).collect(Collectors.joining("答"));
        String groupId = dto.getGroupId();
        Question question = questionRepository.findByContentIgnoreCaseAndGroupId(questionContent, groupId);
        if (question == null) {
            question = new Question();
            question.setGroupId(groupId);
            question.setShare(0);
            question.setContent(questionContent);
            questionRepository.save(question);
            question = questionRepository.findByContentIgnoreCaseAndGroupId(questionContent, groupId);
        }
        Integer questionId = question.getId();
        answerContent = enhanceAnswer(answerContent);

        Answer answer = new Answer();
        answer.setQuestionId(questionId);
        answer.setContent(answerContent);
        answerRepository.save(answer);

        String reply = "添加问题成功，问题id：%d".formatted(questionId);
        return Optional.of(reply);
    }

    private Optional<String> deleteQuestion(MessageDTO dto) {
        String message = dto.getRawMessage();
        String content = message.substring(4).strip();

        Integer questionId = Integer.parseInt(content);
        questionRepository.deleteById(questionId);
        answerRepository.deleteByQuestionId(questionId);

        return Optional.of("删除成功");
    }

    private Optional<String> deleteAnswer(MessageDTO dto) {
        String message = dto.getRawMessage();
        String content = message.substring(4).strip();

        Integer answerId = Integer.parseInt(content);
        answerRepository.deleteById(answerId);

        return Optional.of("删除成功");
    }

    private Optional<String> fuzzyQueryQuestion(MessageDTO dto) {
        String content = dto.getRawMessage().substring(6).strip();
        String groupId = dto.getGroupId();

        String queryContent = "%" + content + "%";
        List<Question> questions = questionRepository.findAllByContentLikeIgnoreCaseAndGroupId(queryContent, groupId);

        if (CollectionUtil.isEmpty(questions)) {
            return Optional.of("无匹配问题");
        }
        String replyContent = questions.stream().map(e -> "问题id：%d，词条：%s".formatted(e.getId(), e.getContent())).collect(Collectors.joining("\n"));
        String reply = "匹配到以下问题：\n" + replyContent;
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
            HttpRequest request = HttpUtil.createGet(url);
            HttpResponse response = request.execute();
            InputStream stream = response.bodyStream();
            oss.putObject(bucketName, ossKey, stream);
            answer = answer.replace(imageResult, "[CQ:image,file=%s,subType=0]".formatted(newUrl));
        }

        return answer;
    }

    public static void main(String[] args) {
        String content = "11[CQ:image,file=save/1e155d2506c44e04a11c6b0f5cfdc685.jpg]22[CQ:image,file=save/851e6e33cd054556947e330fef22e2f3.jpg]33[CQ:image,file=save/460f1ef645824fd8a0333d1cc05dde22.jpg]44[CQ:image,file=save/013986dae1e14a73b65b6e5e6436f0ef.jpg]55";
//        String content ="111";
        Pattern pattern = Pattern.compile("(.*)?+(\\[CQ:.*?])");
        List<String> allGroups = ReUtil.getAllGroups(pattern, content);
        for (String group : allGroups) {
            Console.log(group);
        }


    }
}
