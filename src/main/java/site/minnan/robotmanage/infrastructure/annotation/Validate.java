package site.minnan.robotmanage.infrastructure.annotation;


import site.minnan.robotmanage.infrastructure.validate.Validator;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER})
public @interface Validate {

    String[] fields() default {};

    String[] deepFields() default {};

    Class<?> targetClass() default Object.class;

    Class<? extends Validator> validator();


}
