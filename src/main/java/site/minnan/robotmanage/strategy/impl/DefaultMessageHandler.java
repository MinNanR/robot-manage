package site.minnan.robotmanage.strategy.impl;

import cn.hutool.core.util.RandomUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.criteria.Predicate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import site.minnan.robotmanage.entity.aggregate.Answer;
import site.minnan.robotmanage.entity.aggregate.Question;
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

    @Value("${groups}")
    private String[] serviceGroups;

    public DefaultMessageHandler(QuestionRepository questionRepository, QuestionGroupRepository questionGroupRepository, AnswerRepository answerRepository, RedisUtil redisUtil) {
        this.questionRepository = questionRepository;
        this.questionGroupRepository = questionGroupRepository;
        this.answerRepository = answerRepository;
        this.redisUtil = redisUtil;
        objectMapper = new ObjectMapper();
    }

    private record QuestionAndGroupPair(String question, String groupId){}

    private static class AnswerTime {
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

        private boolean hasAnswer(String s) {
            return stream().anyMatch(e -> Objects.equals(s, e.content));
        }
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

        //获取当前群编码
        int groupNumber = 1;
        for (String group : serviceGroups) {
            if (Objects.equals(group, groupId)) {
                break;
            }
            groupNumber = groupNumber << 1;
        }
        //Predicate内需要使用final变量，所以这里用一个新变量储存结果
        int finalGroupNumber = groupNumber;
        //groupMask是这个词条的群掩码，用于控制在哪个群展示
        //groupMask与群编码相与为0表示不在这个群展示，非0表示可以在这个群展示
        questionList.removeIf(e -> {
            Integer groupMask = e.getGroupMask();
            return (groupMask & finalGroupNumber) == 0;
        });

        List<Integer> questionIds = questionList.stream().map(Question::getId).collect(Collectors.toList());
        List<Answer> answerList = answerRepository.findAnswerByQuestionIdInAndWhetherDeleteIs(questionIds, 0);

        if (answerList.isEmpty()) {
            return Optional.empty();
        }

        String result;
        if (answerList.size() > 1) {
            //答案多于1个时需要使用循环策略
            if (!answerContainerMap.containsKey(pair)) {
                //容器中不存在记录，则创建新的容器
                answerContainer = createAnswerContainer(pair, answerList);
            } else {
                AnswerContainer temp = answerContainerMap.get(pair);
                //比对缓存中的内容，不一致时重新生成
                if (answerList.stream().anyMatch(e -> !temp.hasAnswer(e.getContent()))) {
                    answerContainer = createAnswerContainer(pair, answerList);
                } else {
                    answerContainer = temp;
                }
            }
            AnswerTime randomAnswer = RandomUtil.randomEle(answerContainer);
            result = randomAnswer.content;
            randomAnswer.refer();
            if (randomAnswer.time >= 3) {
                //答案回复大于三次，则将答案放到下一个队列中
                if (answerContainer.next == null) {
                    answerContainer.next = new AnswerContainer();
                }
                AnswerTime newAnswerTime = new AnswerTime(randomAnswer.content);
                answerContainer.next.add(newAnswerTime);
                answerContainer.remove(randomAnswer);
                if (answerContainer.isEmpty()) {
                    answerContainerMap.put(pair, answerContainer.next);
                }
            }
            //将回复次数缓存保存十分钟
            redisUtil.valueSet(redisKey, "1", Duration.ofMinutes(10));
        } else {
            result = answerList.get(0).getContent();
        }
        return Optional.of(result);
    }

    private AnswerContainer createAnswerContainer(QuestionAndGroupPair pair, List<Answer> answerList) {
        AnswerContainer answerContainer;
        List<AnswerTime> answerTimeList = answerList.stream().map(e -> new AnswerTime(e.getContent())).toList();
        answerContainer = new AnswerContainer();
        answerContainer.addAll(answerTimeList);
        answerContainerMap.put(pair, answerContainer);
        return answerContainer;
    }

    public void removeContainer(String expireKey) {
        String[] keySplit = expireKey.split("::");
        QuestionAndGroupPair pair = new QuestionAndGroupPair(keySplit[1], keySplit[0]);
        answerContainerMap.remove(pair);
    }
}
