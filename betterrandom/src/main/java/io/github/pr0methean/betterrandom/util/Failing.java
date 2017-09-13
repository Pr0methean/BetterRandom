package io.github.pr0methean.betterrandom.util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates a test case or test-configuring methods that is currently disabled or has a
 * test-weakening override due to a bug and can be re-enabled once it's fixed.
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface Failing {

}
