package site.minnan.robotmanage.infrastructure.validate;

import lombok.extern.slf4j.Slf4j;
import site.minnan.robotmanage.infrastructure.annotation.Validate;

import java.lang.reflect.Method;

/**
 * 深层校验Object数组
 *
 * @author Minnan on 2021/03/31
 */
@Slf4j
public class ObjectArrayNotNullDeepValidator extends Validator {

    private static ObjectArrayNotNullDeepValidator instance;

    public static Validator getInstance() {
        if (instance == null) {
            instance = new ObjectArrayNotNullDeepValidator();
        }
        return instance;
    }


    @Override
    public ValidateResult validate(Object param, Validate validate) throws Throwable {
        ValidateResult result = new ValidateResult();
        String[] fields = validate.fields();
        if (fields.length == 0) {
            return result;
        }
        String targetField = fields[0];
        Class<?> paramType = param.getClass();
        String getTargetMethodName = "get" + targetField.substring(0, 1).toUpperCase() + targetField.substring(1);
        try {
            Method getTargetMethod = paramType.getMethod(getTargetMethodName);
            Class<?> returnType = getTargetMethod.getReturnType();
            if (!returnType.isArray()) {
                log.warn("there is no pulic getter method returning array for field {} in class {}", fields,
                        paramType.getName());
                return result;
            }
            Object[] values = (Object[]) getTargetMethod.invoke(param);
            if (values == null || values.length == 0) {
                result.add(targetField, String.format("field %s require a nonempty array", targetField));
                return result;
            }
            String[] deepFields = validate.deepFields();
            if (deepFields.length == 0) {
                return result;
            }
            Class<?> elementType = validate.targetClass();

            for (String field : deepFields) {
                String methodName = "get" + field.substring(0, 1).toUpperCase() + field.substring(1);
                Method getMethod;
                try {
                    getMethod = elementType.getMethod(methodName);
                } catch (NoSuchMethodException e) {
                    log.warn("there is no public getter method for filed {}.{} in class {}", targetField, field,
                            elementType.getName());
                    continue;
                }

                for (int i = 0; i < values.length; i++) {
                    Object value = values[i];
                    if (value != null) {
                        Object deepValue = getMethod.invoke(value);
                        if (deepValue == null) {
                            result.add(String.format("%s[%d].%s", targetField, i, field),
                                    String.format("field %s[%d].%s requires not null", targetField, i, field));
                        }
                    } else {
                        result.add(String.format("%s[%d]", targetField, i),
                                String.format("field %s[%d] requires not null", targetField, i));
                    }
                }
            }
        } catch (NoSuchMethodException ex) {
            log.warn("there is no getter method for field {} in class {}", fields, paramType.getName());
        }
        return result;
    }

    @Override
    public ValidateResult identityValidate(Object param, String paramName, Validate validate) throws Throwable {
        return null;
    }
}
