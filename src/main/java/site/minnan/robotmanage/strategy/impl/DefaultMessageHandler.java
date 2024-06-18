package site.minnan.robotmanage.strategy.impl;

import cn.hutool.core.util.RandomUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import site.minnan.robotmanage.entity.aggregate.Answer;
import site.minnan.robotmanage.entity.aggregate.Question;
import site.minnan.robotmanage.entity.aggregate.QuestionGroup;
import site.minnan.robotmanage.entity.dao.AnswerRepository;
import site.minnan.robotmanage.entity.dao.QuestionGroupRepository;
import site.minnan.robotmanage.entity.dao.QuestionRepository;
import site.minnan.robotmanage.entity.dto.MessageDTO;
import site.minnan.robotmanage.infrastructure.utils.RedisUtil;
import site.minnan.robotmanage.strategy.MessageHandler;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component("default")
public class DefaultMessageHandler implements MessageHandler {

    private final QuestionRepository questionRepository;
    private final QuestionGroupRepository questionGroupRepository;

    private final AnswerRepository answerRepository;

    private final RedisUtil redisUtil;

    private final ObjectMapper objectMapper;

    public DefaultMessageHandler(QuestionRepository questionRepository, QuestionGroupRepository questionGroupRepository, AnswerRepository answerRepository, RedisUtil redisUtil) {
        this.questionRepository = questionRepository;
        this.questionGroupRepository = questionGroupRepository;
        this.answerRepository = answerRepository;
        this.redisUtil = redisUtil;
        objectMapper = new ObjectMapper();
    }

    private record QuestionAndGroupPair(String question, String groupId){}

    private static class AnswerTime{
        private String content;
        private int time;

        private AnswerTime(String content) {
            this.content = content;
            this.time = 0;
        }

        private void refer() {
            time++;
        }
    }

    private static class AnswerContainer extends ArrayList<AnswerTime> {
        private AnswerContainer next;
    }

    private static final Map<QuestionAndGroupPair, AnswerContainer> answerContainerMap = new ConcurrentHashMap<>();

    /**
     * 处理消息
     *
     * @param dto
     * @return
     */
    @Override
    public Optional<String> handleMessage(MessageDTO dto) {
        String message = dto.getRawMessage();
        String groupId = dto.getGroupId();
        Specification<Question> specification = (root, query, builder) -> {
            Predicate contentPredicate = builder.equal(root.get("content"), message);
            Predicate whetherDeletePredicate = builder.equal(root.get("whetherDelete"), 0);
            return query.where(contentPredicate, whetherDeletePredicate).getRestriction();
        };
        //查询词条列表
        List<Question> questionList = questionRepository.findAll(specification);
        //查询不到词条则返回空
        if (questionList.isEmpty()) {
            return Optional.empty();
        }

        QuestionAndGroupPair pair = new QuestionAndGroupPair(message, groupId);
        AnswerContainer answerContainer;
        String redisKey = "question:%s::%s".formatted(pair.groupId, pair.question);
        if (!answerContainerMap.containsKey(pair)) {
            //所有词条id
            List<Integer> questionIdList = questionList.stream().map(Question::getId).toList();
            //查询这些词条在哪些群展示
            List<QuestionGroup> questionGroupList = questionGroupRepository.findByQuestionIdIn(questionIdList);
            //去除不在这个群展示的词条
            questionGroupList.removeIf(questionGroup -> !questionGroup.getGroupId().equals(groupId));
            //筛选出可以展示的词条id
            List<Integer> showQuestionIdList = questionGroupList.stream().map(QuestionGroup::getQuestionId).toList();
            //去除不在这个群展示的词条
            questionList.removeIf(e -> !showQuestionIdList.contains(e.getId()));

            List<Integer> questionIds = questionList.stream().map(e -> e.getId()).collect(Collectors.toList());

            List<Answer> answerList = answerRepository.findAnswerByQuestionIdInAndWhetherDeleteIs(questionIds, 0);
            List<AnswerTime> answerTimeList = answerList.stream().map(e -> new AnswerTime(e.getContent())).collect(Collectors.toList());
            answerContainer = new AnswerContainer();
            answerContainer.addAll(answerTimeList);
            answerContainerMap.put(pair, answerContainer);
            redisUtil.valueSet(redisKey, "1", Duration.ofMinutes(10));

        } else {
            answerContainer = answerContainerMap.get(pair);
        }
        //无词条答案返回空
        if (answerContainer.isEmpty()) {
            return Optional.empty();
        }
        //随机一个答案
//        Answer randomAnswer = RandomUtil.randomEle(answers);
        AnswerTime randomAnswer = RandomUtil.randomEle(answerContainer);
        String result = randomAnswer.content;
        randomAnswer.refer();
        if (randomAnswer.time >= 3) {
            if (answerContainer.next == null) {
                answerContainer.next = new AnswerContainer();
            }
            AnswerTime newAnswerTime = new AnswerTime(randomAnswer.content);
            answerContainer.add(newAnswerTime);
            answerContainer.remove(randomAnswer);
            if (answerContainer.isEmpty()) {
                answerContainerMap.put(pair, answerContainer.next);
            }
            redisUtil.valueSet(redisKey, "1", Duration.ofMinutes(10));
        }
//        String result = randomAnswer.getContent();
        return Optional.of(result);
    }

    public void removeContainer(String expireKey) {
        String[] keySplit = expireKey.split("::");
        QuestionAndGroupPair pair = new QuestionAndGroupPair(keySplit[1], keySplit[0]);
        answerContainerMap.remove(pair);
    }
}
