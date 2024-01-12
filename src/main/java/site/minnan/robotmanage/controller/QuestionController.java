package site.minnan.robotmanage.controller;

import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;
import site.minnan.robotmanage.entity.dto.GetQuestionListDTO;
import site.minnan.robotmanage.entity.vo.ListQueryVO;
import site.minnan.robotmanage.entity.vo.QuestionListVO;
import site.minnan.robotmanage.service.QuestionService;

@Slf4j
@Controller
@RequestMapping("/api/question")
public class QuestionController {

    @Autowired
    private QuestionService questionService;

    @RequestMapping("index")
    public ModelAndView questionPage(GetQuestionListDTO dto) {
        log.info(JSONUtil.toJsonStr(dto));
        if(dto.getPageIndex() == null || dto.getPageSize() == null) {
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

    @RequestMapping("getQuestionList")
    @ResponseBody
    public ListQueryVO<QuestionListVO> getQuestionList(GetQuestionListDTO dto) {
        log.info(JSONUtil.toJsonStr(dto));
        if (dto.getPageIndex() == null || dto.getPageSize() == null) {
            dto.setPageSize(10);
            dto.setPageIndex(1);
        }
        ListQueryVO<QuestionListVO> vo = questionService.getQuestionList(dto);
        return vo;
    }
}
