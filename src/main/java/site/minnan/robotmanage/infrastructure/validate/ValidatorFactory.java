package site.minnan.robotmanage.infrastructure.validate;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

/**
 * 校验器工厂
 *
 * @author Minnan on 2021/4/1
 */
public class ValidatorFactory {

    public final static Map<Class<? extends Validator>, Validator> cache = new HashMap<>();

    public static Validator getValidator(Class<? extends Validator> validatorClass) throws IllegalAccessException,
            InstantiationException, NoSuchMethodException, InvocationTargetException {
        if (!cache.containsKey(validatorClass)) {
            Constructor<? extends Validator> constructor = validatorClass.getConstructor();
            constructor.setAccessible(true);
            Validator validator = constructor.newInstance();
            cache.put(validatorClass, validator);
        }
        return cache.get(validatorClass);

    }
}
