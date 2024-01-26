package site.minnan.robotmanage.infrastructure.validate;

import lombok.extern.slf4j.Slf4j;
import site.minnan.robotmanage.infrastructure.annotation.Validate;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Iterator;

/**
 * 校验字符串Collection非空
 *
 * @author Minnan on 2021/03/31
 */
@Slf4j
public class StringCollectionNotBlankValidator extends Validator {

    private static StringCollectionNotBlankValidator instance;

    public static Validator getInstance() {
        if (instance == null) {
            instance = new StringCollectionNotBlankValidator();
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
                    //判断返回类型是否为Collection类型
                    if (!Collection.class.isAssignableFrom(returnType)) {
                        log.warn("there is no getter method returning array of java.lang.String for field {} in class" +
                                " {}", fields, paramType.getName());
                        continue;
                    }
                    Collection<String> values = (Collection<String>) getMethod.invoke(param);
                    if (values == null || values.size() == 0) {
                        result.add(field, String.format("field %s requires a collection of String", field));
                        continue;
                    }
                    Iterator<String> iterator = values.iterator();
                    for (int i = 0; i < values.size(); i++) {
                        String value = iterator.next();
                        if (value == null || value.length() == 0) {
                            result.add(String.format("%s[%d]", field, i), String.format("field %s[%d] requires " +
                                    "nonempty", field, i));
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
