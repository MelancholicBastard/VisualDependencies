package com.melancholicbastard.config

import java.io.File
import java.net.URI

// Объект для загрузки конфигурации
object ConfigLoader {

    sealed interface Result {
        data class Ok(val config: Config): Result
        data class Error(val errors: List<String>): Result
    }

    private fun isHttpUrl(s: String) = s.startsWith("http://") || s.startsWith("https://")

    fun load(path: String): Result {
        val file = File(path)
        if (!file.exists()) return Result.Error(listOf("Файл конфигурации не найден: $path"))

        // Парсинг CSV-файла в map ключ-значение
        val map = mutableMapOf<String, String>()
        file.readLines().forEach { raw ->
            val line = raw.trim()
            // Пропускаем пустые строки и комментарии
            if (line.isEmpty() || line.startsWith("#")) return@forEach
            val i = line.indexOf(',')
            if (i <= 0) return@forEach
            map[line.take(i).trim()] = line.substring(i + 1).trim()
        }

        val errors = mutableListOf<String>()

        // Вспомогательная функция для заполнения списка ошибок при несоответствии типов
        fun need(key: String): String? {
            val v = map[key]
            if (v.isNullOrBlank()) errors += "Отсутствует или пустой параметр '$key'"
            return v
        }

        val rawMode = need("testRepoMode")
        val mode = rawMode?.let {
            // Определение mode как null, TEST или URL
            runCatching { TestRepoMode.valueOf(it.uppercase()) }
                .getOrElse { e ->
                    errors += "Недопустимый testRepoMode '$it': ${e.message}"
                    null
                }
        }

        val packageName = need("packageName")?.also { spec ->
            if (mode == TestRepoMode.TEST) {
                if (!spec.matches(Regex("^[A-Z]+$"))) {
                    errors += "В TEST режиме packageName должен быть заглавными буквами (напр. 'A' или 'ROOT'): '$spec'"
                }
            } else {
                val parts = spec.split(':')
                val partRe = Regex("[A-Za-z0-9_.-]+")
                if (parts.size != 3 || parts.any { it.isBlank() || !partRe.matches(it) }) {
                    errors += "packageName должен быть groupId:artifactId:version в обычном режиме: '$spec'"
                }
            }
        }

        val repoPath = need("repoPath")?.also {
            if (mode == TestRepoMode.TEST) {
                val f = File(it)
                if (!f.exists()) errors += "TEST файл репозитория не найден: $it"
            } else {
                if (isHttpUrl(it)) {
                    runCatching { URI(it) }.onFailure { e -> errors += "Невалидный URL repoPath: ${e.message}" }
                } else {
                    if (!File(it).exists()) errors += "Путь repoPath не существует: $it"
                }
            }
        }

        val outputImage = need("outputImage")?.also {
            if (!it.matches(Regex("^[A-Za-z0-9_.-]+\\.svg$", RegexOption.IGNORE_CASE)))
                errors += "outputImage должен оканчиваться на .svg: '$it'"
        }

        val maxDepthRaw = need("maxDepth")
        val maxDepth = maxDepthRaw?.toIntOrNull()?.also {
            if (it <= 0) errors += "maxDepth должен быть > 0"
        } ?: run {
            if (maxDepthRaw != null) errors += "maxDepth не целое число"
            null
        }

        // Если набралась хотя бы одна ошибка
        if (errors.isNotEmpty()) return Result.Error(errors)

        return Result.Ok(
            Config(
                packageName = packageName!!,
                repoPath = repoPath!!,
                testRepoMode = mode!!,
                outputImage = outputImage!!,
                maxDepth = maxDepth!!
            )
        )
    }
}