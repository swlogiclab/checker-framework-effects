package org.checkerframework.checker.genericeffects.qual;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation is meant to be used by classes so the developer may specify which effect to
 * default to.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface DefaultEffect {
  /**
   * The default effect for the annotated scope
   *
   * @return An annotation
   */
  Class<? extends Annotation> value() default SafeCast.class;
}
