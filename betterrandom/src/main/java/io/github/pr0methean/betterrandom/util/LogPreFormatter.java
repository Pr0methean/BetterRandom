package io.github.pr0methean.betterrandom.util;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Wrapper around {@link Logger} that calls {@link String#format(String, Object...)}, but only if
 * the log statement is actually eligible to go to an appender.
 *
 * @author Chris Hennick
 */
public class LogPreFormatter {

  private final Logger logger;

  /**
   * <p>Constructor for LogPreFormatter.</p>
   *
   * @param clazz a {@link Class} object.
   */
  public LogPreFormatter(final Class<?> clazz) {
    logger = Logger.getLogger(clazz.getName());
  }

  /**
   * <p>format.</p>
   *
   * @param level a {@link Level} object.
   * @param formatString a {@link String} object.
   * @param args a {@link Object} object.
   */
  public void format(final Level level, final String formatString, final Object... args) {
    if (logger.isLoggable(level)) {
      logger.log(level, String.format(formatString, (Object[]) args));
    }
  }

  /**
   * <p>error.</p>
   *
   * @param formatString a {@link String} object.
   * @param args a {@link Object} object.
   */
  public void error(final String formatString, final Object... args) {
    format(Level.SEVERE, formatString, (Object[]) args);
  }

  /**
   * <p>warn.</p>
   *
   * @param formatString a {@link String} object.
   * @param args a {@link Object} object.
   */
  public void warn(final String formatString, final Object... args) {
    format(Level.WARNING, formatString, (Object[]) args);
  }

  /**
   * <p>info.</p>
   *
   * @param formatString a {@link String} object.
   * @param args a {@link Object} object.
   */
  public void info(final String formatString, final Object... args) {
    format(Level.INFO, formatString, (Object[]) args);
  }

  /**
   * Log the stack trace of the current thread.
   *
   * @param level the level to log at.
   */
  public void logStackTrace(Level level, StackTraceElement[] stackTrace) {
    if (logger.isLoggable(level)) {
      for (StackTraceElement element : stackTrace) {
        logger.log(level, "  " + element);
      }
    }
  }
}
