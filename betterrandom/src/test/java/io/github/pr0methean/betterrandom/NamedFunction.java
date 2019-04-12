package io.github.pr0methean.betterrandom;

import java.io.Serializable;
import java8.util.function.Function;

public abstract class NamedFunction<T, R> implements Function<T, R>, Serializable {

  private final String name;

  public NamedFunction(final String name) {
    this.name = name;
  }

  @Override public String toString() {
    return name;
  }
}
