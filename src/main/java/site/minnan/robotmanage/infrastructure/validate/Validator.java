package site.minnan.robotmanage.infrastructure.validate;


import site.minnan.robotmanage.infrastructure.annotation.Validate;

/**
 * 参数校验器
 *
 * @author Minnan on 2021/03/31
 */
public abstract class Validator {

    public abstract ValidateResult validate(Object param, Validate validate) throws Throwable;

    public abstract ValidateResult identityValidate(Object param,String paramName, Validate validate) throws Throwable;

    public static Validator getInstance(){
        return null;
    }
}
