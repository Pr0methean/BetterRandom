/**
 * Implementations of {@link io.github.pr0methean.betterrandom.prng.BaseRandom} that are backed by
 * instances of {@link java.util.SplittableRandom} or {@link java.util.Random}, many of them wrapped
 * in {@link java.lang.ThreadLocal}{@code <?>} to allow concurrent use of a shared instance by
 * multiple threads. The latter is not in general reproducible, since threads start in a
 * nondeterministic order and do not have a serializable identity.
 * {@link io.github.pr0methean.betterrandom.prng.BaseRandom#setSeed(byte[])} may affect only the
 * calling thread.
 */
@ParametersAreNonnullByDefault
package io.github.pr0methean.betterrandom.prng.adapter;

import javax.annotation.ParametersAreNonnullByDefault;