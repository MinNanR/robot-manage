package site.minnan.robotmanage.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import site.minnan.robotmanage.entity.aggregate.Nick;
import site.minnan.robotmanage.entity.aggregate.QueryMap;
import site.minnan.robotmanage.entity.dto.GetNickListDTO;
import site.minnan.robotmanage.entity.dto.GetQueryMapListDTO;
import site.minnan.robotmanage.entity.response.ResponseEntity;
import site.minnan.robotmanage.entity.vo.ListQueryVO;
import site.minnan.robotmanage.infrastructure.annotation.ParamValidate;
import site.minnan.robotmanage.infrastructure.annotation.Validate;
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

    @ParamValidate(validates = @Validate(validator = NotNullValidator.class, fields =  {"pageSize","pageIndex"}))
    @PostMapping("getQueryMapList")
    public ResponseEntity<ListQueryVO<QueryMap>> getQueryMapList(@RequestBody GetQueryMapListDTO dto) {
        ListQueryVO<QueryMap> queryMapList = characterSupportService.getQueryMapList(dto);
        return ResponseEntity.success(queryMapList);
    }
}
