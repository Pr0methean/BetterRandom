package io.github.pr0methean.betterrandom.util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method or constructor as an entry point, so that IntelliJ's code inspections won't decide
 * it's unused. Once test coverage for BetterRandom is complete, this will only be used in
 * benchmarks and debugging tools.
 */
@Inherited
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
public @interface EntryPoint {

}
