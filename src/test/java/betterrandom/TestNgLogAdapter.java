package betterrandom;

import java.util.WeakHashMap;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import org.testng.log4testng.Logger;

public class TestNgLogAdapter extends Handler {

  private static final WeakHashMap<Class<?>, TestNgLogAdapter> INSTANCES = new WeakHashMap<>();
  private final Logger testNgLogger;

  public TestNgLogAdapter(Class<?> clazz) {
    testNgLogger = Logger.getLogger(clazz);
  }

  public static void ensureClassLoaded() {
  }

  public static TestNgLogAdapter getHandler(String name) {
    try {
      return INSTANCES.computeIfAbsent(Class.forName(name), TestNgLogAdapter::new);
    } catch (ClassNotFoundException ignored) {
      return INSTANCES.computeIfAbsent(TestNgLogAdapter.class, TestNgLogAdapter::new);
    }
  }

  @Override
  public void publish(LogRecord record) {
    String message = record.getMessage();
    Throwable thrown = record.getThrown();
    int level = record.getLevel().intValue();
    if (thrown != null) {
      if (level >= Level.SEVERE.intValue()) {
        testNgLogger.error(message, thrown);
      } else if (level >= Level.WARNING.intValue()) {
        testNgLogger.warn(message, thrown);
      } else if (level >= Level.INFO.intValue()) {
        testNgLogger.info(message, thrown);
      } else if (level >= Level.FINE.intValue()) {
        testNgLogger.debug(message, thrown);
      } else {
        testNgLogger.trace(message, thrown);
      }
    } else {
      if (level >= Level.SEVERE.intValue()) {
        testNgLogger.error(message);
      } else if (level >= Level.WARNING.intValue()) {
        testNgLogger.warn(message);
      } else if (level >= Level.INFO.intValue()) {
        testNgLogger.info(message);
      } else if (level >= Level.FINE.intValue()) {
        testNgLogger.debug(message);
      } else {
        testNgLogger.trace(message);
      }
    }
  }

  @Override
  public void flush() {
    // No-op.
  }

  @Override
  public void close() {
    // No-op.
  }
}
