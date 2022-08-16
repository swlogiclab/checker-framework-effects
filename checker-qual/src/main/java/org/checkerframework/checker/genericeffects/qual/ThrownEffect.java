package org.checkerframework.checker.genericeffects.qual;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Annotation to specify the effect of a method when a specific exception is thrown. */
@Documented
@Repeatable(ThrownEffects.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface ThrownEffect {
  /**
   * The exception whose pre-throw effect is being specified.
   *
   * @return The annotated exception
   */
  Class<? extends Exception> exception();

  /**
   * The annotation indicating the effect of the method when the indicated exception is thrown.
   *
   * @return An annotation
   */
  Class<? extends Annotation> behavior();
}
