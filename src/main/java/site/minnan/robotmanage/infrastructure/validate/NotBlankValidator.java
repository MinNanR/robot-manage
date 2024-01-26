package site.minnan.robotmanage.infrastructure.validate;

import lombok.extern.slf4j.Slf4j;
import site.minnan.robotmanage.infrastructure.annotation.Validate;

import java.lang.reflect.Method;
import java.util.Objects;

@Slf4j
public class NotBlankValidator extends Validator {

    @Override
    public ValidateResult validate(Object param, Validate validate) throws Throwable {
        String[] fields = validate.fields();
        Class<?> paramType = param.getClass();
        ValidateResult result = new ValidateResult();
        if (fields.length > 0) {
            for (String field : fields) {
                if (field != null && field.length() > 0) {
                    String methodName = "get" + field.substring(0, 1).toUpperCase() + field.substring(1);
                    try {
                        Method getMethod = paramType.getMethod(methodName);
                        Class<?> returnType = getMethod.getReturnType();
                        if (!Objects.equals(String.class, returnType)) {
                            log.warn("there is no public getter method returning java.lang.String for " +
                                    "filed '{}' in class {}", field, paramType.getName());
                            continue;
                        }
                        String value = (String) getMethod.invoke(param);
                        if (value == null || value.length() == 0) {
                            result.add(field, "field " + field + " requires nonempty");
                        }
                    } catch (NoSuchMethodException ex) {
                        log.warn("there is no public getter method for filed '{}' in class {}",
                                field, paramType.getName());
                    }
                }
            }
        }
        return result;
    }

    @Override
    public ValidateResult identityValidate(Object param, String paramName, Validate validate) throws Throwable {
        ValidateResult result = new ValidateResult();
        if (param instanceof String) {
            String value = (String) param;
            if (value.length() == 0) {
                result.add(paramName, "field " + paramName + " requires nonempty");
            }
        } else {
            log.warn("can not cast {} to java.lang.String for field '{}'", param.getClass().getName(), paramName);
        }
        return result;
    }
}
