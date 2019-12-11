package com.londogard.fuzzymatch

import kotlin.math.min

class FuzzyMatcher(private val scoreConfig: ScoreConfig = ScoreConfig()) {
    data class Result(val indices: List<Int>, val score: Int, val text: String? = null)
    private data class DataHolder(val textLeft: String, val matches: List<Int>, val recursiveResults: List<Result>)

    private val emptyResult = Result(emptyList(), 0)
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
     * A fuzzy match, finds all possible matches and retrieves the optimal solution using ScoringConfig.
     * Currently only returns if full match. Else empty result returned.
     *
     * @param text: the text input
     * @param pattern: the pattern we want to match to the text
     * @param res: recursion param (ignore it)
     * @param textLen: recursion param (ignore it)
     * @param fullText: recursion param (ignore it)
     */
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

                if (recursiveParams.matches.size != pattern.length) emptyResult
                else results.filter { it.score > 0 }.map { Result(it.indices, scoringFunction(it.indices, fullText)) }.maxBy { it.score }?.copy(text = fullText)!!
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
        texts
            .map { fuzzyMatchFunc(it, pattern) }
            .filter { it.score > 0 }
            .sortedByDescending { it.score }
            .take(topN)

    private fun scoringFunction(indices: List<Int>, text: String): Int {
        return listOf(
            min(3, indices[0]) * scoreConfig.unmatchedLeadingLetter,
            indices.windowed(2).map { indexWindow ->
                val firstLetter = if (indexWindow.first() == 0) scoreConfig.firstLetterMatch else 0
                val consecutive = if (indexWindow.first() == indexWindow.last() - 1) scoreConfig.consecutiveMatch else 0
                val neighbour = text[indexWindow.first()]
                val camelCase = if (neighbour.isLowerCase() && text[indexWindow.last()].isUpperCase()) scoreConfig.camelCaseMatch else 0
                val separator = if (neighbour == ' ' || neighbour == '_') scoreConfig.separatorMatch else 0
                val unmatched = (indices.lastOrNull() ?: text.length) * scoreConfig.unmatchedLetter

                firstLetter + consecutive + camelCase + separator + unmatched
            }.sum()
        ).sum()
    }
}