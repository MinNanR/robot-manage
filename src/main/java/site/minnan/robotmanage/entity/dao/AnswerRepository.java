package site.minnan.robotmanage.entity.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import site.minnan.robotmanage.entity.aggregate.Answer;

import java.util.Collection;
import java.util.List;

@Repository
public interface AnswerRepository extends JpaRepository<Answer, Integer>, JpaSpecificationExecutor<Answer> {

    /**
     * 根据词条id查找
     * @param questions
     * @return
     */
    List<Answer> findAnswerByQuestionIdIn(Collection<Integer> questions);

    void deleteByQuestionId(Integer questionId);
}
