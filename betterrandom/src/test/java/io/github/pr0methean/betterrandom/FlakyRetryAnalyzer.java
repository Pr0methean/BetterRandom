package io.github.pr0methean.betterrandom;

import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;

/**
 * From https://www.toolsqa.com/selenium-webdriver/retry-failed-tests-testng/
 */
public class FlakyRetryAnalyzer implements IRetryAnalyzer {
  int counter = 0;
  final int retryLimit = 4;

  @Override public boolean retry(ITestResult result) {
    if (result.isSuccess()) {
      return false;
    }
    if (counter < retryLimit) {
      counter++;
      return true;
    }
    return false;
  }
}
