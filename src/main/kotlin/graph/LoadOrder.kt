package com.melancholicbastard.graph

object LoadOrder {

    fun compute(graph: DependencyGraph): List<String> {
        if (graph.nodes.isEmpty()) return emptyList()

        // Оставляем только ацикличные рёбра
        val edges = graph.edges.filter { !it.cycle }

        // outgoing[from] = set(to), только из достижимого множества nodes
        val nodes = graph.nodes
        val outgoing = mutableMapOf<String, MutableSet<String>>()
        val indeg = mutableMapOf<String, Int>().apply { nodes.forEach { this[it] = 0 } }

        edges.forEach { e ->
            if (e.from in nodes && e.to in nodes) {
                outgoing.getOrPut(e.from) { LinkedHashSet() }.add(e.to)
            }
        }
        outgoing.values.forEach { tos ->
            tos.forEach { to -> indeg[to] = (indeg[to] ?: 0) + 1 }
        }

        // Узлы с нулевой входящей степенью
        val q = ArrayDeque<String>()
        nodes.sorted().forEach { if ((indeg[it] ?: 0) == 0) q.addLast(it) }

        val topo = mutableListOf<String>()
        val seen = mutableSetOf<String>()

        while (q.isNotEmpty()) {
            val v = q.removeFirst()
            if (!seen.add(v)) continue
            topo += v
            outgoing[v]?.sorted()?.forEach { to ->
                indeg[to] = (indeg[to] ?: 0) - 1
                if (indeg[to] == 0) q.addLast(to)
            }
        }

        // Разворот: зависимости идут перед зависящими (дети перед родителем)
        return topo.asReversed()
    }
}