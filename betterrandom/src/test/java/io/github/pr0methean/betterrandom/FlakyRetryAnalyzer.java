package io.github.pr0methean.betterrandom;

import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;

/**
 * From https://www.toolsqa.com/selenium-webdriver/retry-failed-tests-testng/
 */
public class FlakyRetryAnalyzer implements IRetryAnalyzer {
  @Override public boolean retry(ITestResult result) {
    return false;
  }
}
