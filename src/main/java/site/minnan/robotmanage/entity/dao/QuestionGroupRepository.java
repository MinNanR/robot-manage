package site.minnan.robotmanage.entity.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import site.minnan.robotmanage.entity.aggregate.QuestionGroup;

import java.util.List;

/**
 *
 *
 * @author Minnan on 2024/01/25
 */
@Repository
public interface QuestionGroupRepository extends JpaRepository<QuestionGroup, Integer>, JpaSpecificationExecutor<QuestionGroup> {

    List<QuestionGroup> findByQuestionIdIs(Integer questionId);

    List<QuestionGroup> findByQuestionIdIn(List<Integer> questionIdList);

    void deleteByQuestionIdIs(Integer questionId);
}
