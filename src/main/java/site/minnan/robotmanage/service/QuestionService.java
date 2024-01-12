package site.minnan.robotmanage.service;

import site.minnan.robotmanage.entity.aggregate.Answer;
import site.minnan.robotmanage.entity.dto.GetQuestionListDTO;
import site.minnan.robotmanage.entity.vo.ListQueryVO;
import site.minnan.robotmanage.entity.vo.QuestionListVO;

import java.util.List;

/**
 * 词条服务
 *
 * @author Minnan on 2023/06/09
 */
public interface QuestionService {

    /**
     * 查询词条列表
     *
     * @param dto
     * @return
     */
    ListQueryVO<QuestionListVO> getQuestionList(GetQuestionListDTO dto);

}
