/**
 * Implementations of {@link java.util.Random} designed to be used concurrently from multiple
 * threads, backed by instances of {@link java.util.SplittableRandom} or
 * {@link java.lang.ThreadLocal}{@code <?>}. An instance in a given state may give different
 * pseudorandom numbers depending on which thread is calling, and the mapping of threads to seeds
 * cannot in general be deserialized (since threads do not have a serializable identity).
 * {@link io.github.pr0methean.betterrandom.prng.BaseRandom#setSeed(byte[])} may be thread-local.
 */
package io.github.pr0methean.betterrandom.prng.adapter;