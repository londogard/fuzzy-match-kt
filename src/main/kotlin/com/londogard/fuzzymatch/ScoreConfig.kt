package com.londogard.fuzzymatch

data class ScoreConfig(
    val matchedLetter: Int = 0,
    val firstLetterMatch: Int = 15,
    val unmatchedLetter: Int = -1,
    val consecutiveMatch: Int = 15,
    val separatorMatch: Int = 30,
    val camelCaseMatch: Int = 30,
    val unmatchedLeadingLetter: Int = -5 // This one should maximally hit -15 (i.e. *3) points in total
)
