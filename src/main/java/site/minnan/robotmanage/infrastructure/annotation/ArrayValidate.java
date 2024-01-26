package site.minnan.robotmanage.infrastructure.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 数组类型校验
 *
 * @author Minnan on 2021/3/29
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ArrayValidate {

    //字段名称
    String param();

    //数组的类型
    Class<?> clazz();

    //是否做深层校验
    boolean deep() default false;

    //深层校验时的非空字段
    String[] notNull() default {};

    //深层校验时的非空字符串
    String[] notBlank() default {};

    //当数组类型为String类型时要求不能为空串
    boolean fieldNotBlank() default true;
}
