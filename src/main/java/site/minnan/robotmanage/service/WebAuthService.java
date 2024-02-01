package site.minnan.robotmanage.service;

import site.minnan.robotmanage.entity.aggregate.WebAuthMenu;
import site.minnan.robotmanage.entity.aggregate.WebAuthUser;
import site.minnan.robotmanage.entity.dto.LoginDTO;
import site.minnan.robotmanage.entity.dto.OperateDTO;
import site.minnan.robotmanage.entity.vo.ListQueryVO;
import site.minnan.robotmanage.entity.vo.LoginVO;

import javax.security.sasl.AuthenticationException;
import java.util.List;
import java.util.Optional;

/**
 * 系统权限服务
 *
 * @author Minnan on 2024/01/31
 */
public interface WebAuthService {

    LoginVO login(LoginDTO loginDTO) throws AuthenticationException;

    /**
     * 根据用户名查找用户信息
     *
     * @param username
     * @return
     */
    Optional<WebAuthUser> getUserByUsername(String username);

    /**
     * 获取菜单
     *
     * @param dto
     * @return
     */
    List<WebAuthMenu> getMenuList(OperateDTO dto);
}
