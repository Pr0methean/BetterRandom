package io.github.pr0methean.betterrandom.seed;

import com.google.common.cache.CacheBuilder;
import io.github.pr0methean.betterrandom.ByteArrayReseedableRandom;
import io.github.pr0methean.betterrandom.util.LooperThread;
import java.util.Collections;
import java.util.Random;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.locks.Condition;

/**
 * This class is used to hold some final transient fields for {@link RandomSeederThread}, so that
 * they are initialized by the last-non-Serializable-ancestor constructor during deserialization.
 */
abstract class RandomSeederThreadTransients extends LooperThread {
  protected final transient Set<ByteArrayReseedableRandom> byteArrayPrngs = Collections.newSetFromMap(
      CacheBuilder.newBuilder().weakKeys().initialCapacity(1)
          .<ByteArrayReseedableRandom, Boolean>build().asMap());
  protected final transient Set<Random> otherPrngs = Collections.newSetFromMap(
      CacheBuilder.newBuilder().weakKeys().initialCapacity(1)
          .<Random, Boolean>build().asMap());
  protected final transient Set<ByteArrayReseedableRandom> byteArrayPrngsThisIteration
      = Collections.newSetFromMap(new WeakHashMap<>(1));
  protected final transient Set<Random> otherPrngsThisIteration
      = Collections.newSetFromMap(new WeakHashMap<>(1));
  protected final transient Condition waitWhileEmpty = lock.newCondition();
  protected final transient Condition waitForEntropyDrain = lock.newCondition();

  RandomSeederThreadTransients(ThreadFactory threadFactory) {
    super(threadFactory);
  }
}
