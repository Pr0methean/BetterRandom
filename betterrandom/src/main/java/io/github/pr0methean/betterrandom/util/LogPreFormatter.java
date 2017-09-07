package io.github.pr0methean.betterrandom.util;

import java.util.logging.Level;
import java.util.logging.Logger;

public class LogPreFormatter {

  private final Logger logger;

  public LogPreFormatter(final Class<?> clazz) {
    this.logger = Logger.getLogger(clazz.getName());
  }

  public void format(final Level level, final String formatString, final Object... args) {
    if (logger.isLoggable(level)) {
      logger.log(level, String.format(formatString, (Object[]) args));
    }
  }

  public void error(final String formatString, final Object... args) {
    format(Level.SEVERE, formatString, (Object[]) args);
  }

  public void warn(final String formatString, final Object... args) {
    format(Level.WARNING, formatString, (Object[]) args);
  }

  public void info(final String formatString, final Object... args) {
    format(Level.INFO, formatString, (Object[]) args);
  }
}
