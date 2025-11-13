package com.melancholicbastard

import com.melancholicbastard.config.ConfigLoader
import com.melancholicbastard.config.TestRepoMode
import com.melancholicbastard.maven.MavenCoordinates
import com.melancholicbastard.graph.DependencyGraphBuilder
import com.melancholicbastard.graph.GraphvizRenderer
import com.melancholicbastard.testrepo.TestRepository
import guru.nidi.graphviz.engine.Format
import guru.nidi.graphviz.engine.Graphviz
import guru.nidi.graphviz.engine.GraphvizCmdLineEngine
import java.io.File


fun main(args: Array<String>) {
    // Парсинг аргументов командной строки для получения пути к конфигурации
    val cfgPath = args.find { it.startsWith("--config=") }?.substringAfter("=")
    if (cfgPath == null) {
        System.err.println("Укажите --config=path/to/config.csv")
        kotlin.system.exitProcess(1)
    }

    // Загрузка конфигурации
    val res = ConfigLoader.load(cfgPath)
    if (res is ConfigLoader.Result.Error) {
        System.err.println("Ошибки конфигурации:")
        res.errors.forEach { System.err.println(" - $it") }
        kotlin.system.exitProcess(1)
    }
    val cfg = (res as ConfigLoader.Result.Ok).config

    // Построение графа зависимостей в зависимости от режима работы
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

    // Вывод статистики по построенному графу
    println("Root: ${graph.root}")
    println("Вершин: ${graph.nodes.size}, рёбер: ${graph.edges.size}")
    if (graph.truncated.isNotEmpty())
        println("Усечение по глубине: ${graph.truncated.joinToString()}")
    if (graph.cycles.isNotEmpty())
        println("Обнаружены циклы: ${graph.cycles.size} рёбер")

    // Генерация DOT
    val dotContent = GraphvizRenderer.renderDot(graph)
    val dotFile = File("${cfg.outputImage}.dot")
    dotFile.writeText(dotContent)
    println("\nDOT сохранён в: ${dotFile.absolutePath}")

    val svgFile = File(cfg.outputImage)

    try {
        // Генерация SVG из DOT-кода с использованием Graphviz
        Graphviz.useEngine(GraphvizCmdLineEngine())
        Graphviz.fromString(dotContent).render(Format.SVG).toFile(svgFile)
        println("SVG сохранён в: ${svgFile.absolutePath}")
    } catch (e: Exception) {
        System.err.println("Не удалось сгенерировать SVG: ${e.message}")
        System.err.println("Оставлен только файл DOT: ${dotFile.absolutePath}")
    }
}

