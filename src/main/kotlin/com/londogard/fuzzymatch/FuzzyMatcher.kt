package com.londogard.fuzzymatch

import kotlin.math.min

private data class DataHolder(val textLeft: String, val matches: List<Int>, val recursiveResults: List<Result>)

class FuzzyMatcher(private val scoreConfig: ScoreConfig = ScoreConfig()) {
    /**
     * Returns true if each character in pattern is found sequentially within text. ~3 times faster than contains
     * Example use-case:
     *      fuzzyMatchSimple(abc, "hello are you a bear?") // true (hello Are you a Bear
     *      fuzzyMatchSimple(abc, "hello are you a deer?") // false
     * @param pattern String - the pattern to be found
     * @param text String - the text where we want to find the pattern
     */
    fun fuzzyMatchSimple(pattern: String, text: String): Boolean {
        var patternIdx = 0
        var textIdx = 0
        val patternLen = pattern.length
        val textLen = text.length

        // Could be replaced by fold and still be speedy -- benchmark
        while (patternIdx != patternLen && textIdx != textLen) {
            if (pattern[patternIdx].equals(text[textIdx], ignoreCase = true)) ++patternIdx
            ++textIdx
        }

        return patternLen != 0 && textLen != 0 && patternIdx == patternLen
    }

    // TODO optimize by extracting all simpleMatches by using Set with index, then sort by scoring recursively
    // TODO optimizations
    //      1. Only save relevant characters
    //      2. Better early exit (?)
    private fun fuzzyMatchFunc(
        text: String,
        pattern: String,
        indices: List<Int> = emptyList(),
        textLen: Int = text.length,
        fullText: String = text
    ): Result {
        return when {
            pattern.isEmpty() -> MatchResult(indices, scoringFunction(indices, fullText))
            pattern.length > text.length || text.isEmpty() -> EmptyResult
            else -> {
                val recursiveParams = pattern.foldIndexed(
                    DataHolder(
                        text,
                        indices,
                        emptyList()
                    )
                ) { index, (textLeft, matches, recursiveRes), patternChar ->
                    when {
                        textLeft.isEmpty() -> return EmptyResult
                        patternChar.equals(textLeft.first(), ignoreCase = true) -> {
                            val recursiveResult = fuzzyMatchFunc(
                                textLeft.substring(1),
                                pattern.substring(index),
                                matches,
                                textLen,
                                fullText
                            )

                            DataHolder(
                                textLeft.substring(1),
                                matches + (textLen - textLeft.length),
                                recursiveRes + recursiveResult
                            )
                        }
                        else -> {
                            val updatedText = textLeft.dropWhile { !it.equals(patternChar, ignoreCase = true) }

                            DataHolder(updatedText, matches, recursiveRes)
                        }
                    }
                }

                val result = if (recursiveParams.matches.size != pattern.length) EmptyResult else MatchResult(recursiveParams.matches, scoringFunction(recursiveParams.matches, fullText))

                (recursiveParams.recursiveResults + result)
                    .mapNotNull { result -> result as? MatchResult }
                    .filter { result -> result.score > 0 }
                    .maxByOrNull { it.score } ?: EmptyResult
            }
        }
    }

    /**
     * Fuzzy match pattern on a collection of texts. Returns the topN results (scoring by ScoreConfig).
     * @param texts: The collection of texts.
     * @param pattern: The pattern to match on the texts
     * @param topN: Number of results to return
     */
    fun fuzzyMatch(texts: List<String>, pattern: String, topN: Int = 20): List<Result> =
        if (pattern.length == 1) texts.asSequence()
            .filter { it.contains(pattern) }
            .take(topN)
            .map { match -> EndResult(listOf(match.indexOf(pattern)), scoreConfig.firstLetterMatch, match) }
            .toList()
        else texts
            .mapNotNull { word ->
                (fuzzyMatchFunc(word, pattern) as? MatchResult)
                    ?.let { result -> EndResult(result.indices, result.score, word) }
            }
            .sortedByDescending { it.score }
            .take(topN)

    private fun scoringFunction(indices: List<Int>, text: String): Int {
        return listOf(
            min(3, indices[0]) * scoreConfig.unmatchedLeadingLetter,
            indices
                .asSequence()
                .windowed(2)
                .map { indexWindow ->
                    val firstLetter = if (indexWindow.first() == 0) scoreConfig.firstLetterMatch else 0
                    val consecutive =
                        if (indexWindow.first() == indexWindow.last() - 1) scoreConfig.consecutiveMatch else 0
                    val neighbour = text[indexWindow.first()]
                    val camelCase =
                        if (neighbour.isLowerCase() && text[indexWindow.last()].isUpperCase()) scoreConfig.camelCaseMatch else 0
                    val separator = if (neighbour == ' ' || neighbour == '_') scoreConfig.separatorMatch else 0
                    val unmatched = (indices.lastOrNull() ?: text.length) * scoreConfig.unmatchedLetter

                    firstLetter + consecutive + camelCase + separator + unmatched
                }
                .sum()
        ).sum()
    }

    object A {
        @JvmStatic
        fun main(args: Array<String>) {

        }
    }
}