package io.github.pr0methean.betterrandom.prng.adapter;

import static io.github.pr0methean.betterrandom.seed.DefaultSeedGenerator.DEFAULT_SEED_GENERATOR;

import edu.emory.mathcs.backport.java.util.Collections;
import io.github.pr0methean.betterrandom.seed.SeedException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class SplittableRandomAdapterDemo {
  private static final String[] VALUE_LABELS = {"A","K","Q","J","10","9","8","7","6","5","4","3","2"};
  private static final String[] SUIT_LABELS = {"♥️","♣️","♦️","♠️"};
  public static void main(String[] args) throws SeedException, InterruptedException {
    String[] cards = new String[52];
    int i=0;
    for (String suit : SUIT_LABELS) {
      for (String value : VALUE_LABELS) {
        cards[i] = value + suit;
        i++;
      }
    }
    ThreadLocal<List<String>> deckCopies = ThreadLocal.withInitial(() -> Arrays.asList(cards.clone()));
    ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(4);
    ReseedingSplittableRandomAdapter random = ReseedingSplittableRandomAdapter.getInstance(
        DEFAULT_SEED_GENERATOR);
    for (i=0; i<1000; i++) {
      executor.submit(() -> {
        List<String> deck = deckCopies.get();
        Collections.shuffle(deck, random);
        System.out.format("North: %s%nEast: %s%nSouth: %s%nWest: %s%n%n",
            String.join(",", deck.subList(0, 13)),
            String.join(",", deck.subList(13, 26)),
            String.join(",", deck.subList(26, 39)),
            String.join(",", deck.subList(39, 52)));
      });
    }
    executor.shutdown();
    executor.awaitTermination(1, TimeUnit.MINUTES);
  }
}
