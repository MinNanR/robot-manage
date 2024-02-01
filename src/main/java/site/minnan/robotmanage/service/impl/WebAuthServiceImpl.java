package site.minnan.robotmanage.service.impl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import site.minnan.robotmanage.entity.aggregate.WebAuthMenu;
import site.minnan.robotmanage.entity.aggregate.WebAuthUser;
import site.minnan.robotmanage.entity.dao.WebAuthMenuRepository;
import site.minnan.robotmanage.entity.dao.WebAuthUserRepository;
import site.minnan.robotmanage.entity.dto.LoginDTO;
import site.minnan.robotmanage.entity.dto.OperateDTO;
import site.minnan.robotmanage.entity.vo.LoginVO;
import site.minnan.robotmanage.infrastructure.utils.JwtUtil;
import site.minnan.robotmanage.service.WebAuthService;

import javax.security.sasl.AuthenticationException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * 系统权限服务
 *
 * @author Minnan on 2024/01/31
 */
@Service
public class WebAuthServiceImpl implements WebAuthService {

    @Value("${jwt.scret}")
    private String scret;

    private JwtUtil jwtUtil;

    private WebAuthUserRepository webAuthUserRepository;

    private WebAuthMenuRepository webAuthMenuRepository;

    public WebAuthServiceImpl(JwtUtil jwtUtil, WebAuthUserRepository webAuthUserRepository, WebAuthMenuRepository webAuthMenuRepository) {
        this.jwtUtil = jwtUtil;
        this.webAuthUserRepository = webAuthUserRepository;
        this.webAuthMenuRepository = webAuthMenuRepository;
    }

    /**
     * 登陆权限校验
     *
     * @param loginDTO
     * @return
     * @throws AuthenticationException
     */
    @Override
    public LoginVO login(LoginDTO loginDTO) throws AuthenticationException {
        String username = loginDTO.getUsername();
        Optional<WebAuthUser> userOpt = getUserByUsername(username);
        WebAuthUser user = userOpt.orElseThrow(() -> new AuthenticationException("用户名不存在"));

        String submitPassword = loginDTO.getPassword();
        String password = user.getPassword();

        if (!Objects.equals(password, submitPassword)) {
            throw new AuthenticationException("密码错误");
        }

        String token =  jwtUtil.generateToken(user);
        return LoginVO.builder()
                .id(user.getId())
                .nickName(user.getNickName())
                .token(token)
                .build();
    }

    /**
     * 根据用户名查找用户信息
     *
     * @param username
     * @return
     */
    @Override
    public Optional<WebAuthUser> getUserByUsername(String username) {
        Optional<WebAuthUser> userOpt = webAuthUserRepository.findFirstByUsername(username);
        return userOpt;
    }

    /**
     * 获取菜单
     *
     * @param dto
     * @return
     */
    @Override
    public List<WebAuthMenu> getMenuList(OperateDTO dto) {
        Integer operatorId = dto.getOperatorId();
        Optional<WebAuthUser> userOpt = webAuthUserRepository.findById(operatorId);
        WebAuthUser user = userOpt.get();
        Integer role = user.getRole();
        List<WebAuthMenu> menuList = webAuthMenuRepository.findByRole(role);
        return menuList;
    }
}
