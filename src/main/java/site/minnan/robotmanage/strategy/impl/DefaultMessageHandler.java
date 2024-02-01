package site.minnan.robotmanage.strategy.impl;

import cn.hutool.core.util.RandomUtil;
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
import site.minnan.robotmanage.strategy.MessageHandler;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component("default")
public class DefaultMessageHandler implements MessageHandler {

    private final QuestionRepository questionRepository;
    private final QuestionGroupRepository questionGroupRepository;

    private final AnswerRepository answerRepository;

    public DefaultMessageHandler(QuestionRepository questionRepository, QuestionGroupRepository questionGroupRepository, AnswerRepository answerRepository) {
        this.questionRepository = questionRepository;
        this.questionGroupRepository = questionGroupRepository;
        this.answerRepository = answerRepository;
    }

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

            Predicate groupIdPredicate = builder.equal(root.get("groupId"), groupId);
//            Predicate sharePredicate = builder.equal(root.get("share"), 1);
//            Predicate shownPredicate = builder.or(groupIdPredicate, sharePredicate);

            return query.where(contentPredicate).getRestriction();
        };
        //查询词条列表
        List<Question> questionList = questionRepository.findAll(specification);
        //查询不到词条则返回空
        if (questionList.isEmpty()) {
            return Optional.empty();
        }

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

        List<Answer> answers = answerRepository.findAnswerByQuestionIdIn(questionIds);
        //无词条答案返回空
        if (answers.isEmpty()) {
            return Optional.empty();
        }
        //随机一个答案
        Answer randomAnswer = RandomUtil.randomEle(answers);
        String result = randomAnswer.getContent();
        return Optional.of(result);
    }
}
