package site.minnan.robotmanage.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import jakarta.persistence.criteria.Predicate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import site.minnan.robotmanage.entity.aggregate.Answer;
import site.minnan.robotmanage.entity.aggregate.Question;
import site.minnan.robotmanage.entity.dao.AnswerRepository;
import site.minnan.robotmanage.entity.dao.QuestionGroupRepository;
import site.minnan.robotmanage.entity.dao.QuestionRepository;
import site.minnan.robotmanage.entity.dto.DetailsQueryDTO;
import site.minnan.robotmanage.entity.dto.GetQuestionListDTO;
import site.minnan.robotmanage.entity.dto.ModifyQuestionDTO;
import site.minnan.robotmanage.entity.vo.ListQueryVO;
import site.minnan.robotmanage.entity.vo.question.QuestionGroupCheck;
import site.minnan.robotmanage.entity.vo.question.QuestionInfoVO;
import site.minnan.robotmanage.entity.vo.question.QuestionListVO;
import site.minnan.robotmanage.infrastructure.exception.EntityNotExistException;
import site.minnan.robotmanage.service.QuestionService;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 词条服务
 *
 * @author Minnan on 2023/06/09
 */
@Service
@Slf4j
public class QuestionServiceImpl implements QuestionService {

    private QuestionRepository questionRepository;

    private AnswerRepository answerRepository;

    private QuestionGroupRepository questionGroupRepository;

    @Value("${groups}")
    private String[] serviceGroups;

    public QuestionServiceImpl(QuestionRepository questionRepository, AnswerRepository answerRepository, QuestionGroupRepository questionGroupRepository) {
        this.questionRepository = questionRepository;
        this.answerRepository = answerRepository;
        this.questionGroupRepository = questionGroupRepository;
    }

    @Override
    public ListQueryVO<QuestionListVO> getQuestionList(GetQuestionListDTO dto) {
        Specification<Question> specification = (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (StrUtil.isNotBlank(dto.getContent())) {
                predicates.add(builder.like(root.get("content"), "%%%s%%".formatted(dto.getContent())));
            }
            if (StrUtil.isNotBlank(dto.getGroupId())) {
                predicates.add(builder.equal(root.get("groupId"), dto.getGroupId()));
            }
            predicates.add(builder.equal(root.get("whetherDelete"), 0));
            return query.where(predicates.toArray(new Predicate[predicates.size()])).getRestriction();
        };
        PageRequest page = PageRequest.of(dto.getPageIndex() - 1, dto.getPageSize(), Sort.by(Sort.Direction.DESC, "updateTime"));

        Page<Question> queryResult = questionRepository.findAll(specification, page);
        List<Integer> questionIds = queryResult.stream().map(e -> e.getId()).collect(Collectors.toList());
        Map<Integer, Integer> answerCountMap;
        if (!questionIds.isEmpty()) {
            List<Answer> answers = answerRepository.findAnswerByQuestionIdInAndWhetherDeleteIs(questionIds, 0);
            answerCountMap = answers.stream().collect(Collectors.groupingBy(e -> e.getQuestionId(),
                    Collectors.collectingAndThen(Collectors.toList(), e -> e.size())));
        } else {
            answerCountMap = new HashMap<>();
        }


        List<QuestionListVO> list = queryResult.stream()
                .map(QuestionListVO::assemble)
                .peek(e -> e.setAnswerCount(answerCountMap.getOrDefault(e.getId(), 0)))
                .toList();

        return new ListQueryVO<>(list, queryResult.getTotalElements(), queryResult.getTotalPages());
    }


    /**
     * 查询问题详情
     *
     * @param dto
     * @return
     */
    @Override
    public QuestionInfoVO getQuestionInfo(DetailsQueryDTO dto) {
        Integer id = dto.getId();
        List<Answer> answerList = answerRepository.findAnswerByQuestionIdAndWhetherDeleteIs(id, 0);
        Pattern pattern = Pattern.compile("\\[CQ:image,file=(.*?)(,subType=0)?\\]");
        answerList.stream()
                .filter(e -> {
                    List<String> match = ReUtil.findAll(pattern, e.getContent(), 0);
                    return CollectionUtil.isNotEmpty(match);
                })
                .forEach(e -> {
                    String replacement = ReUtil.replaceAll(e.getContent(), pattern, m -> "<img style='width: 100%%' src='%s' />".formatted(m.group(1)));
                    e.setContent(replacement);
                });

        Optional<Question> questionOpt = questionRepository.findById(id);
        Question question = questionOpt.get();

        List<QuestionGroupCheck> checkList = new ArrayList<>();
        Integer groupMask = question.getGroupMask();
        for (int i = 0; i < serviceGroups.length; i++) {
            QuestionGroupCheck check = new QuestionGroupCheck(serviceGroups[i]);
            if ((groupMask & (1 << i)) != 0) {
                check.setChecked(1);
            }
            checkList.add(check);
        }

        return new QuestionInfoVO(checkList, answerList, groupMask);
    }

    /**
     * 修改在哪个群展示
     *
     * @param dto
     */
    @Override
    @Transactional
    public void modifyShowGroup(ModifyQuestionDTO dto) {
        Integer questionId = dto.getId();

        Optional<Question> questionOpt = questionRepository.findById(questionId);
        Question question = questionOpt.orElseThrow(() -> new EntityNotExistException("词条不存在"));

        question.setGroupMask(dto.getGroupMask());
        String now = DateTime.now().toString("yyyy-MM-dd HH:mm:ss");
        question.setUpdateTime(now);
        // TODO: 2024/01/26 设置更新人，后续增加权限后补充
        question.setUpdater("平台:" + dto.getOperatorName());
        questionRepository.save(question);
    }

    /**
     * 删除答案
     *
     * @param dto
     */
    @Override
    @Transactional
    public void deleteAnswer(DetailsQueryDTO dto) {
        Integer answerId = dto.getId();
        String now = DateTime.now().toString("yyyy-MM-dd HH:mm:ss");
        Optional<Answer> answerOpt = answerRepository.findById(answerId);
        Answer answer = answerOpt.orElseThrow(() -> new EntityNotExistException("答案不存在"));
        answer.setWhetherDelete(1);
        answer.setUpdateTime(now);
        // TODO: 2024/01/26 设置更新人，后续增加权限后补充
        String operatorName = dto.getOperatorName();
        answer.setUpdater("平台:" + operatorName);
        Integer questionId = answer.getQuestionId();
        Optional<Question> questionOpt = questionRepository.findById(questionId);
        Question question = questionOpt.orElseThrow(() -> new EntityNotExistException("词条不存在"));
        question.setUpdateTime(now);
        question.setUpdater("平台:" + operatorName);

        answerRepository.save(answer);

        Integer answerCount = answerRepository.countByQuestionIdIsAndWhetherDeleteIs(questionId, 0);
        //词条关联的答案数量降至0则删除词条
        if (answerCount <= 0) {
            question.setWhetherDelete(1);
        }

        questionRepository.save(question);

    }

    /**
     * 删除问题
     *
     * @param dto
     */
    @Override
    @Transactional
    public void deleteQuestion(DetailsQueryDTO dto) {
        String now = DateTime.now().toString("yyyy-MM-dd HH:mm:ss");
        Integer questionId = dto.getId();
        Optional<Question> questionOpt = questionRepository.findById(questionId);
        Question question = questionOpt.orElseThrow(() -> new EntityNotExistException("词条不存在"));
        question.setWhetherDelete(1);
        question.setUpdateTime(now);
        // TODO: 2024/01/26 设置更新人，后续增加权限后补充
        question.setUpdater("平台:" + dto.getOperatorName());
        questionRepository.save(question);
        questionRepository.save(question);
    }

    /**
     * 获取服务的群号
     *
     * @return
     */
    @Override
    public List<String> getServiceGroup() {
        return Arrays.stream(serviceGroups).toList();
    }
}
