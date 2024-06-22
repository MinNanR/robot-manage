package site.minnan.robotmanage.strategy.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.lang.Console;
import cn.hutool.core.util.EscapeUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import com.aliyun.oss.OSS;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import site.minnan.robotmanage.entity.aggregate.Answer;
import site.minnan.robotmanage.entity.aggregate.Question;
import site.minnan.robotmanage.entity.aggregate.QuestionGroup;
import site.minnan.robotmanage.entity.dao.AnswerRepository;
import site.minnan.robotmanage.entity.dao.QuestionGroupRepository;
import site.minnan.robotmanage.entity.dao.QuestionRepository;
import site.minnan.robotmanage.entity.dto.MessageDTO;
import site.minnan.robotmanage.infrastructure.exception.EntityNotExistException;
import site.minnan.robotmanage.strategy.MessageHandler;

import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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


    public QuestionMessageHandler(OSS oss, QuestionRepository questionRepository, AnswerRepository answerRepository, QuestionGroupRepository questionGroupRepository) {
        this.oss = oss;
        this.questionRepository = questionRepository;
        this.answerRepository = answerRepository;
        this.questionGroupRepository = questionGroupRepository;
    }

    private OSS oss;

    private QuestionRepository questionRepository;

    private AnswerRepository answerRepository;

    private QuestionGroupRepository questionGroupRepository;


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
        String now = DateTime.now().toString("yyyy-MM-dd HH:mm:ss");
        String message = dto.getRawMessage();
        String[] split = message.substring(4).split("答");
        String questionContent = split[0].strip();
        String answerContent = Arrays.stream(split).skip(1).collect(Collectors.joining("答"));
        String groupId = dto.getGroupId();
        Question question = questionRepository.findByContentIgnoreCaseAndGroupIdAndWhetherDeleteIs(questionContent, groupId, 0);
        if (question == null) {
            question = new Question();
            question.setGroupId(groupId);
            question.setShare(0);
            question.setContent(questionContent);
            question.setWhetherDelete(0);
            questionRepository.save(question);
            question = questionRepository.findByContentIgnoreCaseAndGroupIdAndWhetherDeleteIs(questionContent, groupId, 0);
        }
        question.setUpdater("QQ:" + dto.getSender().userId());
        question.setUpdateTime(now);
        Integer questionId = question.getId();
        answerContent = enhanceAnswer(answerContent);

        Answer answer = new Answer();
        answer.setQuestionId(questionId);
        answer.setContent(answerContent);
        answer.setWhetherDelete(0);
        answer.setUpdater("QQ:" + dto.getSender().userId());
        answer.setUpdateTime(now);
        answerRepository.save(answer);

        Specification<QuestionGroup> questionGroupSpecification = (root, query, builder) -> {
            Predicate groupIdPredicate = builder.equal(root.get("groupId"), dto.getGroupId());
            Predicate questionIdPredicate = builder.equal(root.get("questionId"), questionId);
            return query.where(groupIdPredicate, questionIdPredicate).getRestriction();
        };
        Optional<QuestionGroup> relevanceOpt = questionGroupRepository.findOne(questionGroupSpecification);
        if (relevanceOpt.isEmpty()) {
            QuestionGroup relevance = new QuestionGroup();
            relevance.setQuestionId(questionId);
            relevance.setGroupId(dto.getGroupId());
            questionGroupRepository.save(relevance);
        }

        String reply = "添加问题成功，问题id：%d".formatted(questionId);
        return Optional.of(reply);
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
        questions.removeIf(e -> Objects.equals(e.getWhetherDelete(), 0));

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
