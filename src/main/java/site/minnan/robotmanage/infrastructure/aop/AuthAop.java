package site.minnan.robotmanage.infrastructure.aop;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.SignatureException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import site.minnan.robotmanage.entity.aggregate.WebAuthUser;
import site.minnan.robotmanage.entity.dto.OperateDTO;
import site.minnan.robotmanage.entity.response.ResponseEntity;
import site.minnan.robotmanage.infrastructure.utils.JwtUtil;
import site.minnan.robotmanage.service.WebAuthService;

import java.util.Arrays;
import java.util.Date;
import java.util.Objects;
import java.util.Optional;

@Component
@Aspect
@Slf4j
public class AuthAop {

    @Value("${jwt.header}")
    private String authenticationHeader;

    @Value("${jwt.route.authentication.path}")
    private String[] authenticationPath;

    @Autowired
    private WebAuthService webAuthService;

    private JwtUtil jwtUtil;

    @Autowired
    public void setJwtUtil(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Pointcut("execution(public * site.minnan.robotmanage.controller..*..*(..))")
    public void web() {
    }

    @Pointcut("execution(public * site.minnan.robotmanage.controller.AuthController.login(..))")
    public void auth(){}

    @Pointcut("execution(public * site.minnan.robotmanage.controller.BotController..*(..))")
    public void bot(){}

    @Pointcut("execution(public * site.minnan.robotmanage.controller.JmsController..*(..))")
    public void jms(){}

    @Around("web() && !auth() && !bot() && !jms()")
    public Object filter(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
        ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = requestAttributes.getRequest();
        HttpServletResponse response = requestAttributes.getResponse();

        String requestTokenHeader = request.getHeader(authenticationHeader);

        if (requestTokenHeader==null || !requestTokenHeader.startsWith("Bearer ")) {
            response.setStatus(401);
            return ResponseEntity.invalid("非法用户");
        }

        String jwtToken = requestTokenHeader.substring(7);
        String username = null;
        try {
            username = jwtUtil.getUsernameFromToken(jwtToken);
        } catch (IllegalArgumentException | SignatureException e) {
            log.error("获取token信息失败", e);
            response.setStatus(401);
            return ResponseEntity.invalid("非法用户");
        } catch (ExpiredJwtException e) {
            log.warn("token已过期");
            response.setStatus(401);
            return ResponseEntity.invalid("用户信息已过期");
        }

        Optional<WebAuthUser> userOpt = webAuthService.getUserByUsername(username);
        if (userOpt.isEmpty()) {
            return ResponseEntity.invalid("非法用户");
        }
        WebAuthUser user = userOpt.get();
        if (!jwtUtil.validateToken(jwtToken, user)) {
            return ResponseEntity.invalid("非法用户");
        }

        Object[] args = proceedingJoinPoint.getArgs();
        for (Object arg : args) {
            if (arg instanceof OperateDTO operateDTO) {
                operateDTO.setOperatorId(user.getId());
                operateDTO.setOperatorName(user.getNickName());
            }
        }

        Date expireDate = jwtUtil.getExpirationDateFromToken(jwtToken);
        long leftTime = DateUtil.between(expireDate, DateTime.now(), DateUnit.MINUTE);
        if (leftTime < 5L) {
            String token = jwtUtil.generateToken(user);

            response.addHeader("newToken", token);
        }

        return proceedingJoinPoint.proceed();
    }
}
