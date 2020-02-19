package io.github.pr0methean.betterrandom.prng.adapter;

import io.github.pr0methean.betterrandom.seed.SecureRandomSeedGenerator;
import io.github.pr0methean.betterrandom.seed.SeedException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public enum ReseedingSplittableRandomAdapterDemo {
  ;

  private static final String[] VALUE_LABELS =
      {"A", "K", "Q", "J", "10", "9", "8", "7", "6", "5", "4", "3", "2"};
  private static final String[] SUIT_LABELS = {"♥️", "♣️", "♦️", "♠️"};

  public static void main(final String[] args) throws SeedException, InterruptedException {
    final String[] cards = new String[52];
    int i = 0;
    for (final String suit : SUIT_LABELS) {
      for (final String value : VALUE_LABELS) {
        cards[i] = value + suit;
        i++;
      }
    }
    final ThreadLocal<List<String>> deckCopies =
        ThreadLocal.withInitial(() -> Arrays.asList(cards.clone()));
    final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(4);
    final ReseedingSplittableRandomAdapter random =
        new ReseedingSplittableRandomAdapter(SecureRandomSeedGenerator.DEFAULT_INSTANCE);
    for (i = 0; i < 20; i++) {
      executor.submit(() -> {
        final List<String> deck = deckCopies.get();
        Collections.shuffle(deck, random);
        System.out.format("North: %s%nEast: %s%nSouth: %s%nWest: %s%n%n",
            String.join(",", deck.subList(0, 13)), String.join(",", deck.subList(13, 26)),
            String.join(",", deck.subList(26, 39)), String.join(",", deck.subList(39, 52)));
      });
    }
    executor.shutdown();
    executor.awaitTermination(1, TimeUnit.MINUTES);
  }
}
