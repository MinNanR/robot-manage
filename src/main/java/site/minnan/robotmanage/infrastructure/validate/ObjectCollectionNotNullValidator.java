package site.minnan.robotmanage.infrastructure.validate;

import lombok.extern.slf4j.Slf4j;
import site.minnan.robotmanage.infrastructure.annotation.Validate;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Iterator;

/**
 * Object类型非空校验
 *
 * @author Minnan on 2021/3/31
 */
@Slf4j
public class ObjectCollectionNotNullValidator extends Validator {

    private static ObjectCollectionNotNullValidator instance;


    public static Validator getInstance() {
        if (instance == null) {
            instance = new ObjectCollectionNotNullValidator();
        }
        return instance;
    }


    @Override
    public ValidateResult validate(Object param, Validate validate) throws Throwable {
        ValidateResult result = new ValidateResult();
        Class<?> paramType = param.getClass();
        String[] fields = validate.fields();
        if (fields.length > 0) {
            for (String field : fields) {
                String methodName = "get" + field.substring(0, 1).toUpperCase() + field.substring(1);
                try {
                    Method getMethod = paramType.getMethod(methodName);
                    Class<?> returnType = getMethod.getReturnType();
                    //判断返回类型是否为String数组
                    if (!Collection.class.isAssignableFrom(returnType)) {
                        log.warn("there is no getter method returning collection for field {} in class" +
                                " {}", fields, paramType.getName());
                        continue;
                    }
                    Collection<?> values = (Collection<?>) getMethod.invoke(param);
                    if (values == null || values.size() == 0) {
                        result.add(field, String.format("field %s requires an nonempty collection", field));
                        continue;
                    }
                    Iterator<?> iterator = values.iterator();
                    for (int i = 0; i < values.size(); i++) {
                        Object value = iterator.next();
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
