package com.melancholicbastard

import com.melancholicbastard.config.ConfigLoader
import com.melancholicbastard.maven.MavenCoordinates
import com.melancholicbastard.maven.MavenRepository
import com.melancholicbastard.maven.PomParser

fun main(args: Array<String>) {
    val configArg = args.find { it.startsWith("--config=") }?.substringAfter("=")
    if (configArg == null) {
        System.err.println("Укажите путь к конфигурации флагом --config=path/to/config.csv")
        kotlin.system.exitProcess(1)
    }

    when (val res = ConfigLoader.load(configArg)) {
        is ConfigLoader.Result.Error -> {
            System.err.println("Ошибки конфигурации:")
            res.errors.forEach { System.err.println(" - $it") }
            kotlin.system.exitProcess(1)
        }
        is ConfigLoader.Result.Ok -> {
            val cfg = res.config
            val coords = try {
                MavenCoordinates.parse(cfg.packageName)
            } catch (e: IllegalArgumentException) {
                System.err.println("Некорректные координаты Maven: ${e.message}")
                kotlin.system.exitProcess(1)
            }

            val pomXml = try {
                MavenRepository.loadPom(cfg.repoPath, coords)
            } catch (e: Exception) {
                System.err.println("Не удалось получить POM: ${e.message}")
                kotlin.system.exitProcess(1)
            }

            val deps = try {
                PomParser.parseDirectDependencies(pomXml)
            } catch (e: Exception) {
                System.err.println("Ошибка парсинга POM: ${e.message}")
                kotlin.system.exitProcess(1)
            }

            // Вывод прямых зависимостей (этап 3)
            if (deps.isEmpty()) {
                println("Нет прямых зависимостей")
            } else {
                deps.forEach { d ->
                    val ver = d.version ?: "(без версии)"
                    val scope = d.scope?.let { " [$it]" } ?: ""
                    println("${d.groupId}:${d.artifactId}:$ver$scope")
                }
            }
        }
    }
}

