package com.londogard.fuzzymatch

import kotlin.math.min

class FuzzyMatcher(private val scoreConfig: ScoreConfig = ScoreConfig()) {
    data class Result(val indices: List<Int>, val score: Int, val text: String? = null)
    data class DataHolder(val textLeft: String, val matches: List<Int>, val recursiveResults: List<Result>)

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
                else results.filter { it.score > 0 }.map { Result(it.indices, scoringFunction(it.indices, fullText)) }.maxBy { it.score }?.copy(text = fullText)!!
            }
        }
    }

    fun fuzzyMatch(texts: List<String>, pattern: String, topN: Int = 20): List<Result> =
        texts
            .asSequence()
            .map { fuzzyMatchFunc(it, pattern) }
            .sortedByDescending { it.score }
            .take(topN).toList()

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
