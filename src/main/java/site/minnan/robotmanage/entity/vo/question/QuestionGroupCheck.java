package site.minnan.robotmanage.entity.vo.question;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 词条在该群号中是否勾选上
 *
 * @author Minnan on 2024/01/25
 */
@Getter
@Setter
@NoArgsConstructor
public class QuestionGroupCheck {

    private String groupId;

    private Integer checked;

    public QuestionGroupCheck(String groupId) {
        this.groupId = groupId;
        this.checked = 0;
    }
}
