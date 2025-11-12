package com.melancholicbastard

import com.melancholicbastard.config.ConfigLoader
import com.melancholicbastard.config.TestRepoMode
import com.melancholicbastard.maven.MavenCoordinates
import com.melancholicbastard.graph.DependencyGraphBuilder
import com.melancholicbastard.graph.LoadOrder
import com.melancholicbastard.testrepo.TestRepository

fun main(args: Array<String>) {
    val cfgPath = args.find { it.startsWith("--config=") }?.substringAfter("=")
    val printOrder = args.any { it == "--print-order" }
    if (cfgPath == null) {
        System.err.println("Укажите --config=path/to/config.csv")
        kotlin.system.exitProcess(1)
    }

    val res = ConfigLoader.load(cfgPath)
    if (res is ConfigLoader.Result.Error) {
        System.err.println("Ошибки конфигурации:")
        res.errors.forEach { System.err.println(" - $it") }
        kotlin.system.exitProcess(1)
    }
    val cfg = (res as ConfigLoader.Result.Ok).config

    val graph = when (cfg.testRepoMode) {
        TestRepoMode.TEST -> {
            val repo = TestRepository.load(cfg.repoPath)
            DependencyGraphBuilder.buildTest(cfg.packageName, repo, cfg.maxDepth)
        }
        TestRepoMode.URL -> {
            val coords = try { MavenCoordinates.parse(cfg.packageName) } catch (e: Exception) {
                System.err.println("Некорректные координаты: ${e.message}")
                kotlin.system.exitProcess(1)
            }
            DependencyGraphBuilder.buildMaven(coords, cfg.repoPath, cfg.maxDepth)
        }
    }

    println("Root: ${graph.root}")
    println("Вершин: ${graph.nodes.size}, рёбер: ${graph.edges.size}")
    if (graph.truncated.isNotEmpty())
        println("Усечение по глубине на: ${graph.truncated.joinToString()}")

    if (printOrder) {
        val order = LoadOrder.compute(graph)
        println("\nПорядок загрузки зависимостей (дети → родитель):")
        order.forEachIndexed { i, id -> println("${i + 1}. $id") }
        if (graph.cycles.isNotEmpty()) {
            println("\nВнимание: обнаружены циклы")
        }
    } else {
        println("\nAdjacency (from -> to${'$'}{cycle?}):")
        graph.edges.forEach {
            val mark = if (it.cycle) " (cycle)" else ""
            println("${it.from} -> ${it.to}$mark")
        }
        if (graph.cycles.isEmpty()) {
            println("\nЦиклов не обнаружено")
        } else {
            println("\nЦиклические рёбра:")
            graph.cycles.forEach { println("${it.from} -> ${it.to}") }
        }
    }
}

