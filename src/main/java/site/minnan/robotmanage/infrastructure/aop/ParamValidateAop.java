package site.minnan.robotmanage.infrastructure.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import site.minnan.robotmanage.entity.response.ResponseCode;
import site.minnan.robotmanage.entity.response.ResponseEntity;
import site.minnan.robotmanage.infrastructure.annotation.ParamValidate;
import site.minnan.robotmanage.infrastructure.annotation.Validate;
import site.minnan.robotmanage.infrastructure.validate.ValidateResult;
import site.minnan.robotmanage.infrastructure.validate.Validator;
import site.minnan.robotmanage.infrastructure.validate.ValidatorFactory;


import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

@Component
@Aspect
@Slf4j
public class ParamValidateAop {

    @Pointcut("execution(public * site.minnan.robotmanage.controller..*..*(..))")
    public void validateParam() {
    }

    @Around("validateParam()")
    public Object validateParam(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
        proceedingJoinPoint.getSignature();
        MethodSignature methodSignature = (MethodSignature) proceedingJoinPoint.getSignature();
        Method method = methodSignature.getMethod();


        Annotation[][] paramAnnotations = method.getParameterAnnotations();
        Object[] args = proceedingJoinPoint.getArgs();
        if (args == null || args.length == 0) {
            return proceedingJoinPoint.proceed();
        }

        //执行方法参数的校验器
        ValidateResult result = new ValidateResult();
        Class<Validate> validateClass = Validate.class;
        Parameter[] parameters = method.getParameters();
        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            Validate validate = parameter.getAnnotation(validateClass);
            if (validate == null) {
                continue;
            }
            Class<? extends Validator> validatorClass = validate.validator();
            Validator validator = ValidatorFactory.getValidator(validatorClass);
            Method validateMethod = validatorClass.getMethod("identityValidate", Object.class, String.class,
                    validateClass);
            ValidateResult segmentResult = (ValidateResult) validateMethod.invoke(validator, args[i],
                    parameter.getName(), validate);
            result.addAll(segmentResult);
        }

        //执行方法参数上的校验器
        ParamValidate annotation = method.getAnnotation(ParamValidate.class);
        if (annotation == null) {
            return result.size() > 0 ? result : proceedingJoinPoint.proceed();
        }
        int paramIndex = annotation.paramIndex();
        if (paramIndex > args.length - 1) {
            return result.size() > 0 ? result : proceedingJoinPoint.proceed();
        }
        Object param = args[paramIndex];

        Validate[] validates = annotation.validates();
        for (Validate validate : validates) {
            Class<? extends Validator> validatorClass = validate.validator();
            Validator validator = ValidatorFactory.getValidator(validatorClass);
            Method validateMethod = validatorClass.getMethod("validate", Object.class, Validate.class);
            ValidateResult segmentResult = (ValidateResult) validateMethod.invoke(validator, param, validate);
            result.addAll(segmentResult);
        }
        if (result.size() > 0) {
            return ResponseEntity.fail(ResponseCode.INVALID_PARAM, result);
        }
        return proceedingJoinPoint.proceed();
    }

}
