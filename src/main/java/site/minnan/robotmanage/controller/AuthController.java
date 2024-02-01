package site.minnan.robotmanage.controller;

import cn.hutool.crypto.digest.BCrypt;
import cn.hutool.crypto.digest.MD5;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import site.minnan.robotmanage.entity.aggregate.WebAuthMenu;
import site.minnan.robotmanage.entity.dto.LoginDTO;
import site.minnan.robotmanage.entity.dto.OperateDTO;
import site.minnan.robotmanage.entity.response.ResponseEntity;
import site.minnan.robotmanage.entity.vo.LoginVO;
import site.minnan.robotmanage.infrastructure.annotation.ParamValidate;
import site.minnan.robotmanage.infrastructure.annotation.Validate;
import site.minnan.robotmanage.infrastructure.validate.NotBlankValidator;
import site.minnan.robotmanage.service.WebAuthService;

import javax.security.sasl.AuthenticationException;
import java.util.List;

/**
 * 权限控制器
 *
 * @author Minnan on 2023/06/08
 */
@Slf4j
@RestController
@RequestMapping("/robot/api/auth")
public class AuthController {

    private WebAuthService webAuthService;

    public AuthController(WebAuthService webAuthService) {
        this.webAuthService = webAuthService;
    }


    @ParamValidate(validates = @Validate(validator = NotBlankValidator.class, fields = {"username", "password"}))
    @PostMapping("login")
    public ResponseEntity<LoginVO> login(@RequestBody LoginDTO dto) throws AuthenticationException {
        LoginVO vo = webAuthService.login(dto);
        return ResponseEntity.success(vo);
    }

    /**
     * 获取菜单
     * @param dto
     * @return
     */
    @PostMapping("getMenuList")
    public ResponseEntity<List<WebAuthMenu>> getMenuList(OperateDTO dto) {
        List<WebAuthMenu> menuList = webAuthService.getMenuList(dto);
        return ResponseEntity.success(menuList);
    }
}
