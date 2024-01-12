package site.minnan.robotmanage.service.impl;

import cn.hutool.core.util.StrUtil;
import jakarta.persistence.criteria.Predicate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import site.minnan.robotmanage.entity.aggregate.Answer;
import site.minnan.robotmanage.entity.aggregate.Question;
import site.minnan.robotmanage.entity.dao.AnswerRepository;
import site.minnan.robotmanage.entity.dao.QuestionRepository;
import site.minnan.robotmanage.entity.dto.GetQuestionListDTO;
import site.minnan.robotmanage.entity.vo.ListQueryVO;
import site.minnan.robotmanage.entity.vo.QuestionListVO;
import site.minnan.robotmanage.service.QuestionService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 词条服务
 *
 * @author Minnan on 2023/06/09
 */
@Service
@Slf4j
public class QuestionServiceImpl implements QuestionService {

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private AnswerRepository answerRepository;


    @Override
    public ListQueryVO<QuestionListVO> getQuestionList(GetQuestionListDTO dto) {
        Specification<Question> specification = (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if(StrUtil.isNotBlank(dto.getContent())) {
                predicates.add(builder.like(root.get("content"), dto.getContent()));
            }
            if(StrUtil.isNotBlank(dto.getGroupId())) {
                predicates.add(builder.equal(root.get("groupId"), dto.getGroupId()));
            }
            return query.where(predicates.toArray(new Predicate[predicates.size()])).getRestriction();
        };
        PageRequest page = PageRequest.of(dto.getPageIndex() - 1, dto.getPageSize(), Sort.by(Sort.Direction.DESC, "id"));
        
        Page<Question> queryResult = questionRepository.findAll(specification, page);
        List<Integer> questionIds = queryResult.stream().map(e -> e.getId()).collect(Collectors.toList());
        Map<Integer, Integer> answerCountMap;
        if (!questionIds.isEmpty()) {
            List<Answer> answers = answerRepository.findAnswerByQuestionIdIn(questionIds);
            answerCountMap = answers.stream().collect(Collectors.groupingBy(e -> e.getQuestionId(),
                    Collectors.collectingAndThen(Collectors.toList(), e -> e.size())));
        } else {
            answerCountMap = new HashMap<>();
        }


        List<QuestionListVO> list = queryResult.stream()
                .map(QuestionListVO::assemble)
                .peek(e -> e.setAnswerCount( answerCountMap.getOrDefault(e.getId(), 0)))
                .toList();

        return new ListQueryVO<>(list, queryResult.getTotalElements(), queryResult.getTotalPages());
    }

}
