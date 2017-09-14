package io.github.pr0methean.betterrandom;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates a test case or test-configuring methods that is currently disabled or has a
 * test-weakening override due to a bug and can be re-enabled once it's fixed. This is just a marker
 * annotation for IDEs; no compiler or annotation processor cares about it.
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface TestingDeficiency {

}
