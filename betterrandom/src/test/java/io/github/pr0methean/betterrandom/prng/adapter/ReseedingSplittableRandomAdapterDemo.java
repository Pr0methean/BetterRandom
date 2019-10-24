package io.github.pr0methean.betterrandom.prng.adapter;

import static io.github.pr0methean.betterrandom.seed.SecureRandomSeedGenerator.DEFAULT_INSTANCE;

import com.google.common.base.Joiner;
import io.github.pr0methean.betterrandom.seed.LegacyRandomSeeder;
import io.github.pr0methean.betterrandom.seed.SeedException;
import io.github.pr0methean.betterrandom.seed.SimpleRandomSeeder;
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
  private static final Joiner COMMA_JOINER = Joiner.on(',');

  public static void main(final String[] args) throws SeedException, InterruptedException {
    final String[] cards = new String[52];
    int i = 0;
    for (final String suit : SUIT_LABELS) {
      for (final String value : VALUE_LABELS) {
        cards[i] = value + suit;
        i++;
      }
    }
    final ThreadLocal<List<String>> deckCopies = new ThreadLocal<List<String>>() {
      @Override public List<String> initialValue() {
        return Arrays.asList(cards.clone());
      }
    };
    final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(4);
    final SimpleRandomSeeder randomSeederThread = new SimpleRandomSeeder(DEFAULT_INSTANCE);
    final ReseedingSplittableRandomAdapter random = ReseedingSplittableRandomAdapter
        .getInstance(randomSeederThread, DEFAULT_INSTANCE);
    for (i = 0; i < 20; i++) {
      executor.submit(new Runnable() {
        @Override public void run() {
          final List<String> deck = deckCopies.get();
          Collections.shuffle(deck, random);
          System.out.format("North: %s%nEast: %s%nSouth: %s%nWest: %s%n%n",
              COMMA_JOINER.join(deck.subList(0, 13)), COMMA_JOINER.join(deck.subList(13, 26)),
              COMMA_JOINER.join(deck.subList(26, 39)), COMMA_JOINER.join(deck.subList(39, 52)));
        }
      });
    }
    executor.shutdown();
    executor.awaitTermination(1, TimeUnit.MINUTES);
  }
}
