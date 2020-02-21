package com.londogard.fuzzymatch

import org.junit.Test


class FuzzyMatcherTest   {
    val lines = javaClass.getResourceAsStream("english_355k_words.txt").bufferedReader().readLines()
    val fuzzyMatcher = FuzzyMatcher()
    @Test
    fun `fuzzy match should match something`() {
        assert(fuzzyMatcher.fuzzyMatch(lines, "2nd").contains(FuzzyMatcher.Result(listOf(0, 1, 2), 41, "2nd")))
        assert(fuzzyMatcher.fuzzyMatch(lines, "a").size == 20)
    }

    @Test
    fun `fuzzy match should return N results`() {
        assert(fuzzyMatcher.fuzzyMatch(lines, "a", 5).size == 5)
    }
}