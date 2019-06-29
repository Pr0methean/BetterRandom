package io.github.pr0methean.betterrandom;

import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;

/** From https://www.toolsqa.com/selenium-webdriver/retry-failed-tests-testng/ */
public class FlakyRetryAnalyzer implements IRetryAnalyzer {
  int counter = 0;
  int retryLimit = 1;

  @Override
  public boolean retry(ITestResult result) {

    if(counter < retryLimit)
    {
      counter++;
      return true;
    }
    return false;
  }
}
