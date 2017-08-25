package betterrandom;

import java.util.logging.LogManager;
import java.util.logging.Logger;

public class TestNgLogAdapterManager extends LogManager {

  static {
    TestNgLogAdapter.ensureClassLoaded();
  }

  @Override
  public Logger getLogger(String name) {
    if (name == null) {
      name = Object.class.getName();
    }
    Logger logger = super.getLogger(name);
    if (logger != null) {
      logger.addHandler(TestNgLogAdapter.getHandler(name));
    }
    return logger;
  }
}
