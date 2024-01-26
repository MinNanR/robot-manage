package site.minnan.robotmanage.infrastructure.validate;

import lombok.extern.slf4j.Slf4j;
import site.minnan.robotmanage.infrastructure.annotation.Validate;

import java.lang.reflect.Method;

/***
 * 参数非空校验器
 * @author Minnan on 2021/3/31
 */
@Slf4j
public class NotNullValidator extends Validator {

    private static NotNullValidator instance;

    public static Validator getInstance() {
        if (instance == null) {
            instance = new NotNullValidator();
        }
        return instance;
    }

    @Override
    public ValidateResult validate(Object param, Validate validate) throws Throwable {
        ValidateResult result = new ValidateResult();
        String[] fields = validate.fields();
        Class<?> paramType = param.getClass();
        if (fields.length > 0) {
            for (String field : fields) {
                if (field != null && field.length() > 0) {
                    String methodName = "get" + field.substring(0, 1).toUpperCase() + field.substring(1);
                    try {
                        Method getMethod = paramType.getMethod(methodName);
                        Object value = getMethod.invoke(param);
                        if (value == null) {
                            result.add(field, "field " + field + " requires not null");
                        }
                    } catch (NoSuchMethodException ex) {
                        log.warn("there is no public getter method for field '{}' in class {}", field,
                                paramType.getName());
                    }
                }
            }
        }
        return result;
    }

    @Override
    public ValidateResult identityValidate(Object param, String paramName, Validate validate) throws Throwable {
        ValidateResult result = new ValidateResult();
        if (param == null) {
            result.add(paramName, "field " + paramName + " requires not null");
        }
        return result;
    }

    public NotNullValidator() {
    }
}
