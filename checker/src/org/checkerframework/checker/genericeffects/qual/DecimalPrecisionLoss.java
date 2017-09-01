package org.checkerframework.checker.genericeffects.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR, ElementType.FIELD})
/**
 * This annotation is used for situations where there is a double that is being cast to a float.
 */
public @interface DecimalPrecisionLoss {}
