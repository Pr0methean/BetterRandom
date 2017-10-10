package io.github.pr0methean.betterrandom.util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method or constructor, or the implicit constructor of the marked class, as an entry
 * point, so that IntelliJ's code inspections won't decide it's unused. Once test coverage for
 * BetterRandom is complete, this will only be used in benchmarks and debugging tools, and for
 * constructors that are tested reflectively.
 */
@Inherited
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR, ElementType.TYPE})
public @interface EntryPoint {

}
