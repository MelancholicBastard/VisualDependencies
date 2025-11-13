package com.melancholicbastard.graph

object GraphvizRenderer {

    fun renderDot(graph: DependencyGraph): String {
        val sb = StringBuilder()
        sb.appendLine("digraph dependencies {")
        sb.appendLine("  rankdir=LR;")
        sb.appendLine("  node [shape=box, style=rounded];")

        // Корень — зелёный
        sb.appendLine("  \"${graph.root.escape()}\" [color=green, penwidth=2];")

        // Усечённые узлы — оранжевая пунктирная рамка
        graph.truncated.forEach { id ->
            sb.appendLine("  \"${id.escape()}\" [color=orange, style=\"rounded,dashed\"];")
        }

        // Рёбра: обычные — сплошные, циклические — красные пунктирные
        graph.edges.forEach { e ->
            val from = e.from.escape()
            val to = e.to.escape()
            if (e.cycle) {
                sb.appendLine("  \"$from\" -> \"$to\" [color=red, style=dashed];")
            } else {
                sb.appendLine("  \"$from\" -> \"$to\";")
            }
        }

        sb.appendLine("}")
        return sb.toString()
    }

    private fun String.escape(): String =
        this.replace("\\", "\\\\")
            .replace("\"", "\\\"")
}