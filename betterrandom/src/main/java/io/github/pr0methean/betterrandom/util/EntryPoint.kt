package io.github.pr0methean.betterrandom.util

import java.lang.annotation.Inherited
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

/**
 * Marks a method or constructor, or the implicit constructor of the marked class, as an entry
 * point, so that IntelliJ's code inspections won't decide it's unused. Once test coverage for
 * BetterRandom is complete, this will only be used in benchmarks and debugging tools, and for
 * constructors that are tested reflectively.
 */
@Inherited
@Retention(RetentionPolicy.SOURCE)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER, AnnotationTarget.CONSTRUCTOR, AnnotationTarget.CLASS, AnnotationTarget.FILE)
annotation class EntryPoint
