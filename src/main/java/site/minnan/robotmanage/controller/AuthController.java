package site.minnan.robotmanage.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import site.minnan.robotmanage.entity.dto.LoginDTO;

/**
 * 权限控制器
 *
 * @author Minnan on 2023/06/08
 */
@Slf4j
@Controller
@RequestMapping("/robot/api/auth")
public class AuthController {


    @RequestMapping("login")
    public String login(LoginDTO dto, HttpServletRequest request) {
        log.info("username = {}, password = {}", dto.getUsername(), dto.getPassword());
        HttpSession session = request.getSession();
        session.setAttribute("nickName", dto.getUsername());
        return "redirect:/question";
    }
}
