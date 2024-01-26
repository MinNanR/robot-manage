package site.minnan.robotmanage.entity.vo.question;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import site.minnan.robotmanage.entity.aggregate.Answer;
import site.minnan.robotmanage.entity.aggregate.QuestionGroup;

import java.util.List;

/**
 * 词条信息数据
 *
 * @author Minnan on 2024/01/25
 */
@Getter
@AllArgsConstructor
public class QuestionInfoVO {

    List<QuestionGroupCheck> checkList;

    List<Answer> answerList;

}
