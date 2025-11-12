package com.melancholicbastard.config

enum class TestRepoMode { LOCAL, GIT }

data class Config(
    val packageName: String,
    val repoPath: String,
    val testRepoMode: TestRepoMode,
    val outputImage: String,
    val maxDepth: Int
) {
    fun asKeyValuePairs(): List<Pair<String, String>> = listOf(
        "packageName" to packageName,
        "repoPath" to repoPath,
        "testRepoMode" to testRepoMode.name,
        "outputImage" to outputImage,
        "maxDepth" to maxDepth.toString()
    )
}