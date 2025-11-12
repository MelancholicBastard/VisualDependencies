package com.melancholicbastard.testrepo

import java.io.File

/**
 * Формат строк:
 *   A:B,C,D
 *   X:
 * Комментарии начинаются с '#'.
 */
class TestRepository private constructor(
    private val graph: Map<String, List<String>>
) {
    fun directDeps(id: String): List<String> = graph[id].orEmpty()

    companion object {
        fun load(path: String): TestRepository {
            val map = mutableMapOf<String, List<String>>()
            File(path).readLines().forEachIndexed { lineNo, raw ->
                val line = raw.trim()
                if (line.isEmpty() || line.startsWith("#")) return@forEachIndexed
                val colon = line.indexOf(':')
                require(colon >= 1) { "Неверный формат (нет ':') в строке ${lineNo + 1}" }
                val id = line.substring(0, colon).trim()
                require(id.matches(Regex("^[A-Z]+$"))) { "Некорректный идентификатор '$id' строка ${lineNo + 1}" }
                val depsPart = line.substring(colon + 1).trim()
                val deps = if (depsPart.isBlank()) {
                    emptyList()
                } else {
                    depsPart.split(',').map { it.trim() }.filter { it.isNotEmpty() }.also { list ->
                        list.forEach {
                            require(it.matches(Regex("^[A-Z]+$"))) { "Некорректная зависимость '$it' строка ${lineNo + 1}" }
                        }
                    }
                }
                map[id] = deps
            }
            return TestRepository(map)
        }
    }
}