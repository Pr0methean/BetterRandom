package io.github.pr0methean.betterrandom;

public final class StackTraceHolder extends Throwable {
  private static final long serialVersionUID = -2630425445144895193L;

  public StackTraceHolder(final String name, final StackTraceElement[] stackTrace) {
    super(name, null, false, true);
    setStackTrace(stackTrace);
  }

  @Override public Throwable fillInStackTrace() {
    // No-op: we only use the stack trace that's in our constructor parameter
    return this;
  }
}
