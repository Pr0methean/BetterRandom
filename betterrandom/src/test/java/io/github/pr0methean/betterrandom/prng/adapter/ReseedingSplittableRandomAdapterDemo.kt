package io.github.pr0methean.betterrandom.prng.adapter

import io.github.pr0methean.betterrandom.seed.DefaultSeedGenerator.DEFAULT_SEED_GENERATOR

import io.github.pr0methean.betterrandom.seed.SeedException
import java.util.Arrays
import java.util.Collections
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit

enum class ReseedingSplittableRandomAdapterDemo {
    ;

    companion object {

        private val VALUE_LABELS = arrayOf("A", "K", "Q", "J", "10", "9", "8", "7", "6", "5", "4", "3", "2")
        private val SUIT_LABELS = arrayOf("♥️", "♣️", "♦️", "♠️")

        @Throws(SeedException::class, InterruptedException::class)
        @JvmStatic
        fun main(args: Array<String>) {
            val cards = arrayOfNulls<String>(52)
            var i = 0
            for (suit in SUIT_LABELS) {
                for (value in VALUE_LABELS) {
                    cards[i] = value + suit
                    i++
                }
            }
            val deckCopies = ThreadLocal.withInitial { Arrays.asList<String>(*cards.clone()) }
            val executor = ScheduledThreadPoolExecutor(4)
            val random = ReseedingSplittableRandomAdapter.getInstance(DEFAULT_SEED_GENERATOR)
            i = 0
            while (i < 20) {
                executor.submit {
                    val deck = deckCopies.get()
                    Collections.shuffle(deck, random)
                    System.out.format("North: %s%nEast: %s%nSouth: %s%nWest: %s%n%n",
                            deck.subList(0, 13).joinToString(","), deck.subList(13, 26).joinToString(","),
                            deck.subList(26, 39).joinToString(","), deck.subList(39, 52).joinToString(","))
                }
                i++
            }
            executor.shutdown()
            executor.awaitTermination(1, TimeUnit.MINUTES)
        }
    }
}
