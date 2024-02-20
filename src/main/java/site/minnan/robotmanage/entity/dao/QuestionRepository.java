package site.minnan.robotmanage.entity.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import site.minnan.robotmanage.entity.aggregate.Question;

import java.util.List;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Integer>, JpaSpecificationExecutor<Question> {

    List<Question> findAllByContentLikeIgnoreCase(String content);

    List<Question> findAllByContentLikeIgnoreCaseAndGroupId(String content, String groupId);

    Question findByContentIgnoreCaseAndGroupIdAndWhetherDeleteIs(String content, String groupId, Integer whetherDelete);

}
