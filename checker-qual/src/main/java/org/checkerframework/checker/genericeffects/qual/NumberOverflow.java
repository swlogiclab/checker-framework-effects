package org.checkerframework.checker.genericeffects.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation is for situations involving any sort of overflow/underflow where a value is being
 * cast to an integer. Ex. double to integer, long to byte
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR, ElementType.FIELD})
public @interface NumberOverflow {}
