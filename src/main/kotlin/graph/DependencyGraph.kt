package com.melancholicbastard.graph

data class DependencyGraph(
    val root: String,
    val nodes: Set<String>,
    val edges: List<GraphEdge>,
    val truncated: Set<String>,     // вершины, где остановился обход по глубине
    val cycles: List<GraphEdge>     // подмножество edges с cycle=true
)