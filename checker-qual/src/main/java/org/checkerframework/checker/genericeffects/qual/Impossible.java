package org.checkerframework.checker.genericeffects.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A marker annotation used only internally to the framework, for code that does not return normally
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({})
public @interface Impossible {}
