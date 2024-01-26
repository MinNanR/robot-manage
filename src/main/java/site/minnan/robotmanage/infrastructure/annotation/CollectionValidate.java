package site.minnan.robotmanage.infrastructure.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 校验集合类型参数
 *
 * @author Minnan on 2021/03/29
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CollectionValidate {

    //字段名称
    String param();

    //collection的类型
    Class<?> clazz();

    //是否做深层校验
    boolean deep() default false;

    //深层校验时的非空字段
    String[] notNull() default {};

    //深层校验时的非空字符串
    String[] notBlank() default {};

    boolean fieldNotBlank() default true;
}
