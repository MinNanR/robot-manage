package site.minnan.robotmanage.entity.aggregate;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 词条与群号关联，用于控制词条是否能在该群展示
 *
 * @author Minnan on 2024/01/25
 */
@Entity
@Table(name = "question_group")
@Data
@NoArgsConstructor
public class QuestionGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    //词条id
    private Integer questionId;

    //群号
    private String groupId;

    public QuestionGroup(Integer questionId, String groupId) {
        this.questionId = questionId;
        this.groupId = groupId;
    }
}
