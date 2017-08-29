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
 * This annotation is used in situations where there is an unsafe cast from a floating point number.
 * Ex. double to float, float to integer, float to byte, double to long
 */
public @interface UnsafeDecimalCast {}
