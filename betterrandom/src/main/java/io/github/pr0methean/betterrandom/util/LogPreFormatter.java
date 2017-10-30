package io.github.pr0methean.betterrandom.util;

import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Wrapper around {@link Logger} that calls {@link String#format(String, Object...)}, but only if
 * the log statement is actually eligible to go to an appender.
 * @author Chris Hennick
 */
public class LogPreFormatter {

  // An estimate of the average length of StackTraceElement.toString()
  private static final int ESTIMATED_STE_SIZE = 50;
  private final Logger logger;

  /**
   * <p>Creates a LogPreFormatter for the given class.</p>
   * @param clazz The class that will use this LogPreFormatter.
   */
  public LogPreFormatter(final Class<?> clazz) {
    logger = Logger.getLogger(clazz.getName());
  }

  public void format(final Level level, final int depthFromRealSource, final String formatString,
      final Object... args) {
    for (int i = 0; i < args.length; i++) {
      if (args[i] instanceof byte[]) {
        args[i] = BinaryUtils.convertBytesToHexString((byte[]) args[i]);
      }
    }
    if (logger.isLoggable(level)) {
      final StackTraceElement realSource =
          Thread.currentThread().getStackTrace()[depthFromRealSource];
      logger.logp(level, realSource.getClassName(), realSource.getMethodName(),
          String.format(formatString, (Object[]) args));
    }
  }

  /**
   * <p>Log a formatted string at {@link Level#SEVERE}.</p>
   * @param formatString The format string.
   * @param args The values to be formatted.
   */
  public void error(final String formatString, final Object... args) {
    format(Level.SEVERE, 3, formatString, (Object[]) args);
  }

  /**
   * <p>Log a formatted string at {@link Level#WARNING}.</p>
   * @param formatString The format string.
   * @param args The values to be formatted.
   */
  public void warn(final String formatString, final Object... args) {
    format(Level.WARNING, 3, formatString, (Object[]) args);
  }

  /**
   * <p>Log a formatted string at {@link Level#INFO}.</p>
   * @param formatString The format string.
   * @param args The values to be formatted.
   */
  public void info(final String formatString, final Object... args) {
    format(Level.INFO, 3, formatString, (Object[]) args);
  }

  /**
   * Log the given stack trace.
   * @param level the level to log at.
   * @param stackTrace the stack trace to log.
   */
  public void logStackTrace(final Level level, final StackTraceElement[] stackTrace) {
    if (logger.isLoggable(level)) {
      final StringBuilder stackTraceBuilder =
          new StringBuilder(stackTrace.length * ESTIMATED_STE_SIZE);
      for (final StackTraceElement element : stackTrace) {
        stackTraceBuilder.append(String.format("  %s%n", element));
      }
      logger.logp(level, stackTrace[2].getClassName(), stackTrace[2].getMethodName(),
          stackTraceBuilder.toString());
    }
  }
}
