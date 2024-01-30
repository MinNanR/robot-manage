package site.minnan.robotmanage.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import site.minnan.robotmanage.entity.aggregate.HandlerStrategy;
import site.minnan.robotmanage.entity.dto.GetStrategyListDTO;
import site.minnan.robotmanage.entity.dto.ModifyStrategyOrdinalDTO;
import site.minnan.robotmanage.entity.dto.UpdateStrategyDTO;
import site.minnan.robotmanage.entity.response.ResponseEntity;
import site.minnan.robotmanage.entity.vo.ListQueryVO;
import site.minnan.robotmanage.infrastructure.annotation.ParamValidate;
import site.minnan.robotmanage.infrastructure.annotation.Validate;
import site.minnan.robotmanage.infrastructure.validate.NotBlankValidator;
import site.minnan.robotmanage.infrastructure.validate.NotNullValidator;
import site.minnan.robotmanage.service.StrategyService;

import java.util.List;

@RestController
@RequestMapping("/robot/api/strategy")
public class StrategyController {

    public StrategyController(StrategyService service) {
        this.strategyService = service;
    }

    private StrategyService strategyService;


    /**
     * 查询消息处理组件下拉框
     *
     * @return
     */
    @PostMapping("getComponentDropDown")
    public ResponseEntity<List<String>> getComponentDropDown() {
        List<String> componentList = strategyService.getComponentDropDown();
        return ResponseEntity.success(componentList);
    }


    /**
     * 查询消息消息处理策略列表
     *
     * @param dto
     * @return
     */
    @ParamValidate(validates = @Validate(validator = NotNullValidator.class, fields = {"pageIndex", "pageSize"}))
    @PostMapping("getStrategyList")
    public ResponseEntity<ListQueryVO<HandlerStrategy>> getStrategyList(@RequestBody GetStrategyListDTO dto) {
        ListQueryVO<HandlerStrategy> vo = strategyService.getStrategyList(dto);
        return ResponseEntity.success(vo);
    }

    /**
     * 更新消息处理策略
     *
     * @param dto
     * @return
     */
    @ParamValidate(validates = {
            @Validate(validator = NotNullValidator.class, fields = {"id", "expressionType", "authMask"}),
            @Validate(validator = NotBlankValidator.class, fields = {"strategyName", "expression", "componentName"})
    })
    @PostMapping("updStrategy")
    public ResponseEntity<?> updateStrategy(@RequestBody UpdateStrategyDTO dto) {
        strategyService.modifyStrategy(dto);
        return ResponseEntity.success();
    }

    /**
     * 设置策略启用或停用
     *
     * @return
     */
    @ParamValidate(validates = @Validate(validator = NotNullValidator.class, fields = {"id", "enabled"}))
    @PostMapping("updateStrategyEnable")
    public ResponseEntity<?> updStrategyAble(@RequestBody UpdateStrategyDTO dto) {
        strategyService.updateStrategyEnable(dto);
        return ResponseEntity.success();
    }


    /**
     * 修改策略顺序
     *
     * @param dto
     * @return
     */
    @ParamValidate(validates = @Validate(validator = NotNullValidator.class, fields = {"id", "modifyType"}))
    @PostMapping("modifyStrategyOrdinal")
    public ResponseEntity<?> modifyStrategyOrdinal(@RequestBody ModifyStrategyOrdinalDTO dto) {
        strategyService.modifyStrategyOrdinal(dto);
        return ResponseEntity.success();
    }

    /**
     * 添加消息处理策略
     * @param strategy
     * @return
     */
    @ParamValidate(validates = {
            @Validate(validator = NotNullValidator.class, fields = {"expressionType", "authMask"}),
            @Validate(validator = NotBlankValidator.class, fields = {"strategyName", "expression", "componentName"})
    })
    @PostMapping("addStrategy")
    public ResponseEntity<?> addStrategy(@RequestBody HandlerStrategy strategy) {
        strategyService.addStrategy(strategy);
        return ResponseEntity.success();
    }
}
