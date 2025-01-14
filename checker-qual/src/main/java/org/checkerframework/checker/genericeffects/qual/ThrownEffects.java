package org.checkerframework.checker.genericeffects.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** A container annotation, which is required for <code>@ThrownEffect</code> to be repeatable. */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface ThrownEffects {
  /**
   * Automatically constructed array of <code>ThrownEffect</code> annotations
   *
   * @return The individual thrown effect annotations
   */
  ThrownEffect[] value();
}
