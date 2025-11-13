package com.melancholicbastard.config

data class Config(
    val packageName: String,
    val repoPath: String,
    val testRepoMode: TestRepoMode,
    val outputImage: String,
    val maxDepth: Int
)