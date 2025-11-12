package com.melancholicbastard.maven

data class Dependency(
    val groupId: String,
    val artifactId: String,
    val version: String?,  // может отсутствовать
    val scope: String?     // может отсутствовать
)