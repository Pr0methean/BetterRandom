package betterrandom;

import betterrandom.util.LooperThread;
import java.util.Map;
import java.util.concurrent.locks.Condition;

public class AllThreadsStackDumperThread extends LooperThread {
  public static final AllThreadsStackDumperThread INSTANCE = new AllThreadsStackDumperThread();

  @Override
  public void iterate() throws InterruptedException {
    sleep(30_000);
    Thread.getAllStackTraces().forEach((key, value) -> {
      System.out.println(key);
      for (StackTraceElement element : value) {
        System.out.println(element);
      }
      System.out.println();
    });
  }
}
