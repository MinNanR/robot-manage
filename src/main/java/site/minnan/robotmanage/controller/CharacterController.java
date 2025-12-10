package site.minnan.robotmanage.controller;

import cn.hutool.core.thread.ThreadUtil;
import org.springframework.web.bind.annotation.*;
import site.minnan.robotmanage.entity.aggregate.Nick;
import site.minnan.robotmanage.entity.aggregate.QueryMap;
import site.minnan.robotmanage.entity.dto.GetNickListDTO;
import site.minnan.robotmanage.entity.dto.GetQueryMapListDTO;
import site.minnan.robotmanage.entity.dto.UpdateQueryMapDTO;
import site.minnan.robotmanage.entity.response.ResponseEntity;
import site.minnan.robotmanage.entity.vo.ListQueryVO;
import site.minnan.robotmanage.infrastructure.annotation.ParamValidate;
import site.minnan.robotmanage.infrastructure.annotation.Validate;
import site.minnan.robotmanage.infrastructure.validate.NotBlankValidator;
import site.minnan.robotmanage.infrastructure.validate.NotNullValidator;
import site.minnan.robotmanage.service.CharacterSupportService;

@RestController
@RequestMapping("/robot/api/character")
public class CharacterController {

    private CharacterSupportService characterSupportService;

    public CharacterController(CharacterSupportService characterSupportService) {
        this.characterSupportService = characterSupportService;
    }


    /**
     * 查询昵称列表
     *
     * @return
     */
    @ParamValidate(validates = @Validate(validator = NotNullValidator.class, fields = {"pageIndex", "pageSize"}))
    @PostMapping("getNickList")
    public ResponseEntity<ListQueryVO<Nick>> getNickList(@RequestBody GetNickListDTO dto) {
        ListQueryVO<Nick> nickList = characterSupportService.getNickList(dto);
        return ResponseEntity.success(nickList);
    }

    @ParamValidate(validates = @Validate(validator = NotNullValidator.class, fields = {"pageSize", "pageIndex"}))
    @PostMapping("getQueryMapList")
    public ResponseEntity<ListQueryVO<QueryMap>> getQueryMapList(@RequestBody GetQueryMapListDTO dto) {
        ListQueryVO<QueryMap> queryMapList = characterSupportService.getQueryMapList(dto);
        return ResponseEntity.success(queryMapList);
    }

    /**
     * 更新快捷查询
     *
     * @param dto
     * @return
     */
    @ParamValidate(validates = {
            @Validate(validator = NotNullValidator.class, fields = "id"),
            @Validate(validator = NotBlankValidator.class, fields = {"queryContent", "queryUrl"})
    })
    @PostMapping("updQueryMap")
    public ResponseEntity<?> updateQueryMap(@RequestBody UpdateQueryMapDTO dto) {
        characterSupportService.updateQueryMap(dto);
        return ResponseEntity.success();
    }

    /**
     * 添加快捷查询
     *
     * @param dto
     * @return
     */
    @ParamValidate(validates = @Validate(validator = NotBlankValidator.class, fields = {"queryContent", "queryUrl"}))
    @PostMapping("addQueryMap")
    public ResponseEntity<?> addQueryMap(@RequestBody UpdateQueryMapDTO dto) {
        characterSupportService.addQueryMap(dto);
        return ResponseEntity.success();
    }

    @PostMapping("triggerDailyTask")
    public ResponseEntity<?> triggerDailyTask(@RequestParam("pageCount") Integer pageCount) {
        ThreadUtil.execAsync(() -> {
            for (int i = 0; i < pageCount; i++) {
                characterSupportService.expDailyTask(0);
                ThreadUtil.safeSleep(1000 * 60 * 3);
            }
        });
        return ResponseEntity.success();
    }
}
