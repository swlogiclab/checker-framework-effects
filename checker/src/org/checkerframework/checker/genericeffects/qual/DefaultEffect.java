package org.checkerframework.checker.genericeffects.qual;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface DefaultEffect {
    //this may need to be changed back to strings later on
    Class<? extends Annotation> value() default SafeCast.class;
}