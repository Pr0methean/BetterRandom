package io.github.pr0methean.betterrandom;

import java.io.Serializable;
import java8.util.function.Consumer;
import java8.util.function.Function;

public abstract class NamedFunction<T, R> implements Function<T, R>, Consumer<T>, Serializable {

  private static final long serialVersionUID = 8151994795231615138L;
  private final String name;

  public NamedFunction(final String name) {
    this.name = name;
  }

  @Override public void accept(T t) {
    apply(t);
  }

  @Override public String toString() {
    return name;
  }
}
