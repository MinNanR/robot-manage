package site.minnan.robotmanage.service;

import site.minnan.robotmanage.entity.dto.DetailsQueryDTO;
import site.minnan.robotmanage.entity.dto.GetQuestionListDTO;
import site.minnan.robotmanage.entity.dto.ModifyQuestionDTO;
import site.minnan.robotmanage.entity.vo.ListQueryVO;
import site.minnan.robotmanage.entity.vo.question.QuestionInfoVO;
import site.minnan.robotmanage.entity.vo.question.QuestionListVO;

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

    /**
     * 查询问题详情
     *
     * @param dto
     * @return
     */
    QuestionInfoVO getQuestionInfo(DetailsQueryDTO dto);

    /**
     * 修改在哪个群展示
     *
     * @param dto
     */
    void modifyShowGroup(ModifyQuestionDTO dto);

    /**
     * 删除答案
     *
     * @param dto
     */
    void deleteAnswer(DetailsQueryDTO dto);

    /**
     * 删除问题
     *
     * @param dto
     */
    void deleteQuestion(DetailsQueryDTO dto);

    /**
     * 获取服务的群号
     *
     * @return
     */
    List<String> getServiceGroup();
}
