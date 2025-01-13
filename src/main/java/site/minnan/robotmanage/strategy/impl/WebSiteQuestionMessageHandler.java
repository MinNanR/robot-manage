package site.minnan.robotmanage.strategy.impl;

import cn.hutool.core.date.DateTime;
import com.aliyun.oss.OSS;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import site.minnan.robotmanage.entity.aggregate.Answer;
import site.minnan.robotmanage.entity.aggregate.Question;
import site.minnan.robotmanage.entity.dao.AnswerRepository;
import site.minnan.robotmanage.entity.dao.QuestionRepository;
import site.minnan.robotmanage.entity.dto.MessageDTO;
import site.minnan.robotmanage.infrastructure.utils.BotSessionUtil;
import site.minnan.robotmanage.strategy.MessageHandler;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 网站词条消息处理器
 *
 * @author Minnan on 2024/12/27
 */
@Component("webSiteQuestion")
@Slf4j
public class WebSiteQuestionMessageHandler implements MessageHandler {

    @Value("${groups}")
    private String[] serviceGroups;

    public WebSiteQuestionMessageHandler(QuestionRepository questionRepository, AnswerRepository answerRepository, BotSessionUtil botSessionUtil) {
        this.questionRepository = questionRepository;
        this.answerRepository = answerRepository;
        this.botSessionUtil = botSessionUtil;
    }

    private QuestionRepository questionRepository;

    private AnswerRepository answerRepository;

    private BotSessionUtil botSessionUtil;


    /**
     * 处理消息
     *
     * @param dto
     * @return
     */
    @Override
    public Optional<String> handleMessage(MessageDTO dto) {
        String now = DateTime.now().toString("yyyy-MM-dd HH:mm:ss");
        String message = dto.getRawMessage();
        String[] split = message.substring(4).split("答");
        String questionContent = split[0].strip();
        String url = Arrays.stream(split).skip(1).collect(Collectors.joining("答"));
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

        url = url.replaceAll("\\.", "点");
        Answer answer = new Answer();
        answer.setQuestionId(questionId);
        answer.setContent(url);
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
}
