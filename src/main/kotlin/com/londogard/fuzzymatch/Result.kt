package com.londogard.fuzzymatch

sealed class Result

data class MatchResult(val indices: List<Int>, val score: Int): Result()
data class EndResult(val indices: List<Int>, val score: Int, val text: String): Result()
object EmptyResult: Result()
