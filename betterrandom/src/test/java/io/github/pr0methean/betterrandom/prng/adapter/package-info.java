/**
 * Classes in this package extend {@link io.github.pr0methean.betterrandom.prng.BaseRandom} but
 * delegate the actual random-number generation. Some use a thread-local delegate to emulate a
 * single concurrent PRNG.
 */
package io.github.pr0methean.betterrandom.prng.adapter;