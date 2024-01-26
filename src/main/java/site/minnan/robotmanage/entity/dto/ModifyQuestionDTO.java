package site.minnan.robotmanage.entity.dto;

import lombok.Getter;
import lombok.Setter;
import site.minnan.robotmanage.entity.vo.question.QuestionGroupCheck;

import java.util.List;

/**
 * 修改词条参数
 *
 * @author MInnan on 2024/01/26
 */
@Getter
@Setter
public class ModifyQuestionDTO {

    //词条id
    private Integer id;

    //选中的群号，上传所有
    private List<QuestionGroupCheck> checkList;
}
