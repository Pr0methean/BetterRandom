package io.github.pr0methean.betterrandom.util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method or constructor as an entry point, so that IntelliJ's code inspections won't decide
 * it's unused. Needed for benchmarks, debugging tools, and constructor overloads that are tested
 * only reflectively. When applied to a class, the implicit constructor is an entry point. Should
 * not be applied to a type with no implicit constructor, and has no effect if that happens.
 */
@Inherited @Retention(RetentionPolicy.SOURCE)
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR, ElementType.TYPE})
public @interface EntryPoint {

}
