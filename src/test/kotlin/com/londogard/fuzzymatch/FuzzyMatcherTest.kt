package com.londogard.fuzzymatch

import org.junit.Test
import kotlin.system.measureNanoTime


class FuzzyMatcherTest   {
    private val lines = javaClass.getResourceAsStream("english_355k_words.txt")
        .bufferedReader()
        .readLines()
    private val fuzzyMatcher = FuzzyMatcher()

    @Test
    fun `test speed`() {
        // Old ~2.6s for 1 'abc' match
        println(lines.take(10))

        (1 until 1000).forEach {
            fuzzyMatcher.fuzzyMatch(lines, "abc", 20)
        }
        var a = 0
        measureNanoTime {
            (1 until 1000).forEach {
                a = fuzzyMatcher.fuzzyMatch(lines, "he", 20).size
            }
        }.also { println(it / 1000 / 1000 / 1000) }
        println(fuzzyMatcher.fuzzyMatch(lines, "he", 20))
    }

    @Test
    fun `fuzzy match should match something`() {
//        assert(fuzzyMatcher.fuzzyMatch(lines, "2nd").contains(FuzzyMatcher.Result(listOf(0, 1, 2), 41, "2nd")))
        assert(fuzzyMatcher.fuzzyMatch(lines, "a").size == 20)
    }

    @Test
    fun `fuzzy match should return N results`() {
        assert(fuzzyMatcher.fuzzyMatch(lines, "a", 5).size == 5)
    }
}