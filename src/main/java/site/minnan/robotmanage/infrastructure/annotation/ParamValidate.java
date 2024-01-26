package site.minnan.robotmanage.infrastructure.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ParamValidate {

    //参数在列表中的位置
    int paramIndex() default 0;

    Validate[] validates() default {};
}
