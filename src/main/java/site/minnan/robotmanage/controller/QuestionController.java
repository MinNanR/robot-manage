package site.minnan.robotmanage.controller;

import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import site.minnan.robotmanage.entity.dto.DetailsQueryDTO;
import site.minnan.robotmanage.entity.dto.GetQuestionListDTO;
import site.minnan.robotmanage.entity.dto.ModifyQuestionDTO;
import site.minnan.robotmanage.entity.response.ResponseEntity;
import site.minnan.robotmanage.entity.vo.ListQueryVO;
import site.minnan.robotmanage.entity.vo.question.QuestionGroupCheck;
import site.minnan.robotmanage.entity.vo.question.QuestionInfoVO;
import site.minnan.robotmanage.entity.vo.question.QuestionListVO;
import site.minnan.robotmanage.infrastructure.annotation.ParamValidate;
import site.minnan.robotmanage.infrastructure.annotation.Validate;
import site.minnan.robotmanage.infrastructure.validate.NotNullValidator;
import site.minnan.robotmanage.infrastructure.validate.ObjectCollectionNotNullDeepValidator;
import site.minnan.robotmanage.service.QuestionService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/robot/api/question")
public class QuestionController {

    @Autowired
    private QuestionService questionService;

    @RequestMapping("index")
    public ModelAndView questionPage(GetQuestionListDTO dto) {
        log.info(JSONUtil.toJsonStr(dto));
        if (dto.getPageIndex() == null || dto.getPageSize() == null) {
            dto.setPageIndex(1);
            dto.setPageSize(50);
        }
        ListQueryVO<QuestionListVO> vo = questionService.getQuestionList(dto);
        ModelAndView mv = new ModelAndView("question");
        mv.addObject("list", vo.list());
        mv.addObject("totalCount", vo.totalCount());
        mv.addObject("pageCount", vo.pageCount());
        return mv;
    }

    /**
     * 查询词条列表
     *
     * @param dto
     * @return
     */
    @ParamValidate(validates = @Validate(validator = NotNullValidator.class, fields = {"pageIndex", "pageSize"}))
    @PostMapping("getQuestionList")
    public ResponseEntity<ListQueryVO<QuestionListVO>> getQuestionList(@RequestBody GetQuestionListDTO dto) {
        ListQueryVO<QuestionListVO> vo = questionService.getQuestionList(dto);
        return ResponseEntity.success(vo);
    }

    @ParamValidate(validates = @Validate(validator = NotNullValidator.class, fields = "id"))
    @PostMapping("getQuestionInfo")
    public ResponseEntity<QuestionInfoVO> getQuestionInfo(@RequestBody DetailsQueryDTO dto) {
        QuestionInfoVO vo = questionService.getQuestionInfo(dto);
        return ResponseEntity.success(vo);
    }


    /**
     *
     * @param dto
     * @return
     */
    @ParamValidate(validates = {
            @Validate(validator = NotNullValidator.class, fields = {"id", "groupMask"}),
//            @Validate(validator = ObjectCollectionNotNullDeepValidator.class, targetClass = QuestionGroupCheck.class, fields = "checkList",
//                    deepFields = {"groupId", "checked"})
    })
    @PostMapping("updateCheckGroup")
    public ResponseEntity<?> updateCheckGroup(@RequestBody ModifyQuestionDTO dto) {
        questionService.modifyShowGroup(dto);
        return ResponseEntity.success("更新关联群号成功");
    }

    /**
     * 删除答案
     * @param dto
     * @return
     */
    @ParamValidate(validates = @Validate(validator = NotNullValidator.class, fields = "id"))
    @PostMapping("delAnswer")
    public ResponseEntity<?> deleteAnswer(@RequestBody DetailsQueryDTO dto) {
        questionService.deleteAnswer(dto);
        return ResponseEntity.success("删除答案成功");
    }

    /**
     * 删除词条
     *
     * @param dto
     * @return
     */
    @ParamValidate(validates = @Validate(validator = NotNullValidator.class, fields = "id"))
    @PostMapping("delQuestion")
    public ResponseEntity<?> deleteQuestion(@RequestBody DetailsQueryDTO dto) {
        questionService.deleteQuestion(dto);
        return ResponseEntity.success("删除答案成功");
    }

    @PostMapping("getServiceGroup")
    public ResponseEntity<List<String>> getServiceGroup() {
        List<String> serviceGroup = questionService.getServiceGroup();
        return ResponseEntity.success(serviceGroup);
    }
}
