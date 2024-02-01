package site.minnan.robotmanage.infrastructure.exception;

import cn.hutool.core.map.MapBuilder;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.method.HandlerMethod;
import site.minnan.robotmanage.entity.response.ResponseCode;
import site.minnan.robotmanage.entity.response.ResponseEntity;


import javax.security.sasl.AuthenticationException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 全局统一异常处理器（仅处理在controller内的异常）
 *
 * @author Minnan on 2020/12/17
 */
@ControllerAdvice
@Slf4j
public class ControllerExceptionHandler {

    /**
     * 登录时的非法用户异常
     *
     * @param ex 异常
     * @return
     */
    @ExceptionHandler(AuthenticationException.class)
    @ResponseBody
    public ResponseEntity<?> handleAuthenticationException(AuthenticationException  ex) {
        log.error(StrUtil.format("fail to login {}", ex.getMessage()), ex);
        if (ex.getMessage() != null) {
            return ResponseEntity.fail(ResponseCode.INVALID_USER,
                    MapBuilder.create().put("details", ex.getMessage()).build());
        }
        return ResponseEntity.fail(ResponseCode.INVALID_USER);
    }

    /**
     * 参数非法或缺失时的异常
     *
     * @param ex 异常
     * @return
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseBody
    public ResponseEntity<?> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        log.error(StrUtil.format("Parameter Error,execute in : {},target : {}", ex.getParameter().getExecutable(),
                ex.getBindingResult().getTarget()), ex);
        List<Map<Object, Object>> details = ex.getBindingResult().getAllErrors().stream()
                .map(error -> (FieldError) error)
                .map(error -> MapBuilder.create().put("field", error.getField()).put("message",
                        error.getDefaultMessage()).build())
                .collect(Collectors.toList());
        return ResponseEntity.fail(ResponseCode.INVALID_PARAM, MapBuilder.create().put("details", details).build());
    }

    /**
     * 实体已存在异常（唯一约束不通过）
     *
     * @param ex 异常
     * @return
     */
    @ExceptionHandler(EntityAlreadyExistException.class)
    @ResponseBody
    public ResponseEntity<?> handleEntityAlreadyExistException(EntityAlreadyExistException ex,
                                                               HandlerMethod method) {
        log.error("", ex);
        return ResponseEntity.fail(ex.getMessage());
    }

    /**
     * 处理实体不存在异常，通常发生在查询详情或更新实体时
     *
     * @param ex 异常
     * @return
     */
    @ExceptionHandler(EntityNotExistException.class)
    @ResponseBody
    public ResponseEntity<?> handleEntityNotExistException(EntityNotExistException ex, HandlerMethod method) {
        log.error("", ex);
        return ResponseEntity.fail(ex.getMessage());
    }

//    /**
//     * 处理无权限访问接口异常
//     *
//     * @param ex 异常
//     * @param method
//     * @return
//     */
//    @ExceptionHandler(AccessDeniedException.class)
//    @ResponseBody
//    public ResponseEntity<?> handleAccessDeniedException(AccessDeniedException ex, HandlerMethod method) {
//        String uri = method.getMethod().getAnnotation(PostMapping.class).value()[0];
//        log.error("无权限访问:" + uri, ex);
//        return ResponseEntity.invalid(ex.getMessage());
//    }

    @ExceptionHandler(UnmodifiableException.class)
    @ResponseBody
    public ResponseEntity<?> handleUnmodifiableException(UnmodifiableException ex){
        log.error("实体不可修改", ex);
        return ResponseEntity.fail(ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    @ResponseBody
    public ResponseEntity<?> handleUnknownException(Exception ex) {
        log.error("unknown error", ex);
        return ResponseEntity.fail(ResponseCode.UNKNOWN_ERROR);
    }
}
