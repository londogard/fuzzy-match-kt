package com.londogard.fuzzymatch

import java.io.File
import kotlin.math.min
import kotlin.system.measureNanoTime
import kotlin.system.measureTimeMillis

data class RecursiveResult(
    val matched: Boolean,
    val score: Int = 0,
    val matches: List<Int> = emptyList()
)

/**
 * Returns true if each character in pattern is found sequentially within text.
 * @param pattern String - the pattern to be found
 */
fun String.fuzzyMatchSimple(pattern: String): Boolean {
    var patternIdx = 0
    var textIdx = 0
    val patternLen = pattern.length
    val textLen = length
    val badPattern = patternLen > textLen || patternLen == 0 || textLen == 0

    while (!badPattern && patternIdx != patternLen && textIdx != textLen) {
        if (pattern[patternIdx].toLowerCase() == this[textIdx].toLowerCase()) ++patternIdx
        ++textIdx
    }

    return !badPattern && patternIdx == patternLen
}

class FuzzyMatcher(val scoreConfig: ScoreConfig = ScoreConfig()) {

    /**
     * Returns true if each character in pattern is found sequentially within text. ~3 times faster than contains
     * @param pattern String - the pattern to be found
     * @param text String - the text where we want to find the pattern
     */
    fun fuzzyMatchSimple(pattern: String, text: String): Boolean {
        //text.contains()
        var patternIdx = 0
        var textIdx = 0
        val patternLen = pattern.length
        val textLen = text.length

        while (patternIdx != patternLen && textIdx != textLen) {
            if (pattern[patternIdx].toLowerCase() == text[textIdx].toLowerCase()) ++patternIdx
            ++textIdx
        }

        return patternLen != 0 && textLen != 0 && patternIdx == patternLen
    }

    /**
     * Does a fuzzy search to find pattern inside a string.
     * @param {*} pattern string        pattern to search for
     * @param {*} str     string        string which is being searched
     * @returns [boolean, number]       a boolean which tells if pattern was
     *                                  found or not and a search score
     */

    data class Result(
        val indices: List<Int>,
        val score: Int
    )
    val emptyResult = Result(emptyList(), 0)

    data class RecursiveParams(
        val srcMatches: List<Int>, val maxMatches: Int,
        val recursionCount: Int, val recursionLimit: Int
    )

    data class DataHolder(val textLeft: String, val matches: List<Int>, val recursiveResults: List<Result>)

    fun fuzzyMatchFunc(text: String, pattern: String, res: Result = emptyResult, textLen: Int = text.length, fullText: String = text): Result {
        return when {
            pattern.length > text.length || text.isEmpty()-> emptyResult
            pattern.isEmpty() -> res
            else -> {
                val recursiveParams = pattern.foldIndexed(DataHolder(text, res.indices, emptyList())) { index, (textLeft, matches, recursiveRes), patternChar ->
                    when {
                        textLeft.isEmpty() -> return emptyResult
                        patternChar.equals(textLeft[0], true) -> {
                            val recursiveResult = fuzzyMatchFunc(textLeft.drop(1), pattern.substring(index), res.copy(indices = matches), textLen, fullText)

                            DataHolder(textLeft.drop(1), matches + (textLen - textLeft.length), recursiveRes + recursiveResult)
                        }
                        else -> {
                            val updatedText = textLeft.dropWhile { !it.equals(patternChar, true) }
                            if (updatedText.isEmpty()) return emptyResult

                            val recursiveResult = fuzzyMatchFunc(updatedText.drop(1), pattern.substring(index), res.copy(indices = matches), textLen, fullText)

                            DataHolder(updatedText.drop(1), matches + (textLen - updatedText.length), recursiveRes + recursiveResult)
                        }
                    }
                }
                val results = recursiveParams.recursiveResults + Result(recursiveParams.matches, 10)

                if (results.isEmpty() || recursiveParams.textLeft.isEmpty() && !pattern.last().equals(text.last(), ignoreCase = true)) emptyResult
                else results.filter { it.score > 0 }.map { Result(it.indices, scoringFunction(it.indices, fullText)) }.maxBy { it.score }!!
            }
        }
    }

    private fun scoringFunction(indices: List<Int>, text: String): Int {
        return listOf(
            min(3, indices[0]) * scoreConfig.unmatchedLeadingLetter,
            // add unmatched penalty [val unmatched = text.length - nextMatchVar] * scoreConfig.unmatchedLetter
            indices.windowed(2).map { indexWindow ->
                val firstLetter = if (indexWindow.first() == 0) scoreConfig.firstLetterMatch else 0
                val consecutive = if (indexWindow.first() == indexWindow.last() - 1) scoreConfig.consecutiveMatch else 0
                val neighbour = text[indexWindow.first()]
                val camelCase = if (neighbour.isLowerCase() && text[indexWindow.last()].isUpperCase()) scoreConfig.camelCaseMatch else 0
                val separator = if (neighbour == ' ' || neighbour == '_') scoreConfig.separatorMatch else 0

                firstLetter + consecutive + camelCase + separator
            }.sum()
        ).sum()
    }
}

object a {
    @JvmStatic
    fun main(args: Array<String>) {
        val fuzzy = FuzzyMatcher()
        val lines = File(javaClass.getResource("/english_355k_words.txt").path).readLines()
        //val text = "whois this bastard Hello ded hello"
        val text = "SVisualLoggerLogsList.h"
        val pattern = "LLL"
        // 6,7,13 & 17 - we want all permutations of this..!
        // 6,7,13
        // 6,13,17
        // 7,13,17
        // 6,7,17
        (1..100).forEach {lines.forEach { fuzzy.fuzzyMatchFunc(it, pattern) }  }
        println(measureTimeMillis { lines.forEach { fuzzy.fuzzyMatchFunc(it, pattern) } })
        //println(fuzzy.fuzzyMatchFunc(text, pattern).indices.map { text[it] })

        //println("Fuzzy: ${selfImpl / 1_000_000} ns")
        //println("FuzzyStrExt: ${selfImpl / 1_000_000} ns")
        //println("Contains: ${containsImpl / 1_000_000} ns")
    }
}