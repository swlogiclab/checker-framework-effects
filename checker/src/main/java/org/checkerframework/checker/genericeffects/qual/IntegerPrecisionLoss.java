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
 * This annotation is for any situation where an integer is being cast to a floating point number
 * and precision is lost. Ex. long to float, integer to float, long to double
 */
public @interface IntegerPrecisionLoss {}
