package site.minnan.robotmanage.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import site.minnan.robotmanage.entity.dto.DetailsQueryDTO;
import site.minnan.robotmanage.entity.dto.GetBossListDTO;
import site.minnan.robotmanage.entity.dto.SaveBossDTO;
import site.minnan.robotmanage.entity.response.ResponseEntity;
import site.minnan.robotmanage.entity.vo.ListQueryVO;
import site.minnan.robotmanage.entity.vo.boss.BossInfoVO;
import site.minnan.robotmanage.infrastructure.annotation.ParamValidate;
import site.minnan.robotmanage.infrastructure.annotation.Validate;
import site.minnan.robotmanage.infrastructure.validate.NotBlankValidator;
import site.minnan.robotmanage.infrastructure.validate.NotNullValidator;
import site.minnan.robotmanage.service.BossService;

@RestController
@RequestMapping("/robot/api/boss")
public class BossController {

    private BossService bossService;

    public BossController(BossService bossService) {
        this.bossService = bossService;
    }

    @ParamValidate(validates = @Validate(validator = NotNullValidator.class, fields = {"pageIndex", "pageSize"}))
    @PostMapping("getBossList")
    public ResponseEntity<ListQueryVO<BossInfoVO>> getBossList(@RequestBody GetBossListDTO dto) {
        ListQueryVO<BossInfoVO> vo = bossService.getBossList(dto);
        return ResponseEntity.success(vo);
    }

    @ParamValidate(validates = @Validate(validator = NotNullValidator.class, fields = "id"))
    @PostMapping("getBossInfo")
    public ResponseEntity<BossInfoVO> getBossInfo(@RequestBody DetailsQueryDTO dto) {
        BossInfoVO vo = bossService.getBossInfo(dto);
        return ResponseEntity.success(vo);
    }

    @ParamValidate(validates = @Validate(validator = NotBlankValidator.class, fields = "bossName"))
    @PostMapping("addBoss")
    public ResponseEntity<?> addBoss(@RequestBody SaveBossDTO dto) {
        bossService.addBoss(dto);
        return ResponseEntity.success("新增BOSS成功");
    }

    @ParamValidate(validates = {
            @Validate(validator = NotNullValidator.class, fields = "id"),
            @Validate(validator = NotBlankValidator.class, fields = "bossName")
    })
    @PostMapping("updBoss")
    public ResponseEntity<?> updateBoss(@RequestBody SaveBossDTO dto) {
        bossService.updateBoss(dto);
        return ResponseEntity.success("更新BOSS成功");
    }

    @ParamValidate(validates = @Validate(validator = NotNullValidator.class, fields = "id"))
    @PostMapping("delBoss")
    public ResponseEntity<?> deleteBoss(@RequestBody DetailsQueryDTO dto) {
        bossService.deleteBoss(dto);
        return ResponseEntity.success("删除BOSS成功");
    }
}
