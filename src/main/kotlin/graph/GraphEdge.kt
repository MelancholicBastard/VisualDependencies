package com.melancholicbastard.graph

data class GraphEdge(
    val from: String,
    val to: String,
    val cycle: Boolean
)