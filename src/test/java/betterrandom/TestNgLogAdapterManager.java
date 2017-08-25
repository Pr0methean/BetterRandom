package betterrandom;

import java.util.logging.LogManager;
import java.util.logging.Logger;

public class TestNgLogAdapterManager extends LogManager {

  @Override
  public Logger getLogger(String name) {
    Logger logger = super.getLogger(name);
    logger.addHandler(TestNgLogAdapter.getHandler(name));
    return logger;
  }
}
