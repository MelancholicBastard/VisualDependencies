package com.melancholicbastard.graph

import com.melancholicbastard.maven.MavenCoordinates
import com.melancholicbastard.maven.MavenRepository
import com.melancholicbastard.maven.PomParser
import com.melancholicbastard.testrepo.TestRepository

/**
 * Итеративный DFS без рекурсии.
 * Цвета (state): 0 = в стеке (visiting), 1 = обработан (done).
 */
object DependencyGraphBuilder {

    private data class Frame(
        val id: String,
        val depth: Int,
        var deps: List<String>?,
        var index: Int = 0
    )

    fun buildMaven(rootCoords: MavenCoordinates, repoPath: String, maxDepth: Int): DependencyGraph {
        val rootId = "${rootCoords.groupId}:${rootCoords.artifactId}:${rootCoords.version}"
        val edges = mutableListOf<GraphEdge>()
        val truncated = mutableSetOf<String>()
        val state = mutableMapOf<String, Int>() // 0 visiting, 1 done
        val stack = ArrayDeque<Frame>()
        stack.addLast(Frame(rootId, 0, null))
        state[rootId] = 0
        val nodes = mutableSetOf(rootId)

        fun directDeps(id: String): List<String> {
            val parts = id.split(':')
            val coords = MavenCoordinates(parts[0], parts[1], parts[2])
            val pom = MavenRepository.loadPom(repoPath, coords)
            val deps = PomParser.parseDirectDependencies(pom)
            return deps
                .filter { it.version != null } // пропускаем без версии (упрощение)
                .map { "${it.groupId}:${it.artifactId}:${it.version}" }
                .distinct()
        }

        while (stack.isNotEmpty()) {
            val frame = stack.last()
            if (frame.deps == null) {
                if (frame.depth >= maxDepth) {
                    truncated += frame.id
                    state[frame.id] = 1
                    stack.removeLast()
                    continue
                }
                frame.deps = runCatching { directDeps(frame.id) }.getOrElse { emptyList() }
            }
            if (frame.index < frame.deps!!.size) {
                val dep = frame.deps!![frame.index++]
                nodes += dep
                when (state[dep]) {
                    null -> {
                        edges += GraphEdge(frame.id, dep, false)
                        state[dep] = 0
                        stack.addLast(Frame(dep, frame.depth + 1, null))
                    }
                    0 -> { // цикл
                        edges += GraphEdge(frame.id, dep, true)
                    }
                    1 -> {
                        edges += GraphEdge(frame.id, dep, false)
                    }
                }
            } else {
                state[frame.id] = 1
                stack.removeLast()
            }
        }

        val cycles = edges.filter { it.cycle }
        return DependencyGraph(rootId, nodes, edges, truncated, cycles)
    }

    fun buildTest(root: String, repo: TestRepository, maxDepth: Int): DependencyGraph {
        val edges = mutableListOf<GraphEdge>()
        val truncated = mutableSetOf<String>()
        val state = mutableMapOf<String, Int>()
        val stack = ArrayDeque<Frame>()
        stack.addLast(Frame(root, 0, null))
        state[root] = 0
        val nodes = mutableSetOf(root)

        fun direct(id: String) = repo.directDeps(id)

        while (stack.isNotEmpty()) {
            val frame = stack.last()
            if (frame.deps == null) {
                if (frame.depth >= maxDepth) {
                    truncated += frame.id
                    state[frame.id] = 1
                    stack.removeLast()
                    continue
                }
                frame.deps = direct(frame.id)
            }
            if (frame.index < frame.deps!!.size) {
                val dep = frame.deps!![frame.index++]
                nodes += dep
                when (state[dep]) {
                    null -> {
                        edges += GraphEdge(frame.id, dep, false)
                        state[dep] = 0
                        stack.addLast(Frame(dep, frame.depth + 1, null))
                    }
                    0 -> edges += GraphEdge(frame.id, dep, true)
                    1 -> edges += GraphEdge(frame.id, dep, false)
                }
            } else {
                state[frame.id] = 1
                stack.removeLast()
            }
        }
        val cycles = edges.filter { it.cycle }
        return DependencyGraph(root, nodes, edges, truncated, cycles)
    }
}