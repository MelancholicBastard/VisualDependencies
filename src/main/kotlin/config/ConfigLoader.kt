package com.melancholicbastard.config

import java.io.File
import java.net.URI

object ConfigLoader {

    sealed interface Result {
        data class Ok(val config: Config): Result
        data class Error(val errors: List<String>): Result
    }

    private fun isHttpUrl(s: String) = s.startsWith("http://") || s.startsWith("https://")

    fun load(path: String): Result {
        val file = File(path)
        if (!file.exists()) {
            return Result.Error(listOf("Файл конфигурации не найден: $path"))
        }

        val map = mutableMapOf<String, String>()
        file.readLines().forEachIndexed { _, raw ->
            val line = raw.trim()
            if (line.isEmpty() || line.startsWith("#")) return@forEachIndexed
            val firstComma = line.indexOf(',')
            if (firstComma < 0) return@forEachIndexed
            val key = line.take(firstComma).trim()
            val value = line.substring(firstComma + 1).trim()
            if (key.isEmpty()) return@forEachIndexed
            map[key] = value
        }

        val errors = mutableListOf<String>()

        fun requireKey(k: String): String? {
            val v = map[k]
            if (v == null || v.isBlank()) {
                errors += "Отсутствует или пустой параметр '$k'"
                return null
            }
            return v
        }

        val packageName = requireKey("packageName")?.also { spec ->
            // Ожидаем groupId:artifactId:version
            val parts = spec.split(':')
            val partRe = Regex("[A-Za-z0-9_.-]+")
            if (parts.size != 3 || parts.any { it.isBlank() || !partRe.matches(it) }) {
                errors += "packageName должен быть в формате groupId:artifactId:version, получено: '$spec'"
            }
        }

        val repoPath = requireKey("repoPath")?.also {
            if (isHttpUrl(it)) {
                runCatching { URI(it) }.onFailure { e ->
                    errors += "Невалидный URL repoPath: ${e.message}"
                }
            } else {
                val rf = File(it)
                if (!rf.exists()) errors += "Путь repoPath не существует: $it"
            }
        }

        val testRepoModeRaw = requireKey("testRepoMode")
        val testRepoMode = testRepoModeRaw?.let {
            runCatching { TestRepoMode.valueOf(it.uppercase()) }
                .getOrElse {
                    errors += "Недопустимый testRepoMode: '$testRepoModeRaw' (ожидаются: ${TestRepoMode.entries.joinToString()})"
                    null
                }
        }

        val outputImage = requireKey("outputImage")?.also {
            if (!it.matches(Regex("^[A-Za-z0-9_.-]+\\.(png|svg)$", RegexOption.IGNORE_CASE))) {
                errors += "outputImage должен оканчиваться на .png или .svg: '$it'"
            }
        }

        val maxDepthRaw = requireKey("maxDepth")
        val maxDepth = maxDepthRaw?.toIntOrNull()?.also {
            if (it <= 0) errors += "maxDepth должен быть > 0: $it"
        } ?: run {
            if (maxDepthRaw != null) errors += "maxDepth не целое число: '$maxDepthRaw'"
            null
        }

        if (errors.isNotEmpty()) return Result.Error(errors)

        return Result.Ok(
            Config(
                packageName = packageName!!,
                repoPath = repoPath!!,
                testRepoMode = testRepoMode!!,
                outputImage = outputImage!!,
                maxDepth = maxDepth!!
            )
        )
    }
}