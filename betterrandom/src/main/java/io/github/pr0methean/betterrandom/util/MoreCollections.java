package io.github.pr0methean.betterrandom.util;

import com.google.common.collect.MapMaker;
import java.util.Collections;
import java.util.Set;

public enum MoreCollections {
  ;

  /**
   * Creates and returns a thread-safe {@link Set} with only weak references to its members.
   *
   * @param <T> the set element type
   * @return an empty mutable thread-safe {@link Set} that holds only weak references to its members
   */
  public static <T> Set<T> createSynchronizedWeakHashSet() {
    return Collections.newSetFromMap(new MapMaker().weakKeys().concurrencyLevel(1)
        .initialCapacity(1).makeMap());
  }
}
