package io.github.pr0methean.betterrandom;

import java.io.Serializable;
import java.util.function.Function;

public final class NamedFunction<T, R> implements Function<T, R>, Serializable {

  private static final long serialVersionUID = 8151994795231615138L;
  private final Function<T, R> function;
  private final String name;

  public NamedFunction(final Function<T, R> function, final String name) {
    this.function = function;
    this.name = name;
  }

  @Override public R apply(final T t) {
    return function.apply(t);
  }

  @Override public String toString() {
    return name;
  }
}
