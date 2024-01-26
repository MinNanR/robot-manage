package site.minnan.robotmanage.infrastructure.validate;

import lombok.extern.slf4j.Slf4j;
import site.minnan.robotmanage.infrastructure.annotation.Validate;

import java.lang.reflect.Method;

/**
 * 校验数组类型对象不能为空
 *
 * @author Minnan on 2021/03/31
 */
@Slf4j
public class ObjectArrayNotNullValidator extends Validator {


    private static ObjectArrayNotNullValidator instance;

    public static Validator getInstance() {
        if (instance == null) {
            instance = new ObjectArrayNotNullValidator();
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
                String methodName = "get" + field.substring(0, 1).toUpperCase() + field.substring(1);
                try {
                    Method getMethod = paramType.getMethod(methodName);
                    Class<?> returnType = getMethod.getReturnType();
                    //判断返回类型是否为Object数组
                    if (!Object[].class.isAssignableFrom(returnType)) {
                        log.warn("there is no getter method returning array of java.lang.String for field {} in class" +
                                " {}", fields, paramType.getName());
                        continue;
                    }
                    Object[] values = (Object[]) getMethod.invoke(param);
                    if (values == null || values.length == 0) {
                        result.add(field, String.format("field %s requires an nonempty array", field));
                        continue;
                    }
                    for (int i = 0; i < values.length; i++) {
                        Object value = values[i];
                        if (value == null) {
                            result.add(String.format("%s[%d]", field, i), String.format("field %s[%d] requires not " +
                                    "null", field, i));
                        }
                    }
                } catch (NoSuchMethodException ex) {
                    log.warn("there is no getter method for field {} in class {}", fields, paramType.getName());
                }

            }
        }
        return result;
    }

    @Override
    public ValidateResult identityValidate(Object param, String paramName, Validate validate) throws Throwable {
        return null;
    }
}
