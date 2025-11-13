package com.melancholicbastard.maven

// Maven координаты (groupId:artifactId:version)
data class MavenCoordinates(
    val groupId: String,
    val artifactId: String,
    val version: String
) {
    // Генерация пути к POM файлу в Maven репозитории
    fun toPomPath(): String {
        val groupPath = groupId.replace('.', '/')
        return "$groupPath/$artifactId/$version/$artifactId-$version.pom"
    }

    companion object {
        fun parse(spec: String): MavenCoordinates {
            val parts = spec.split(':')
            require(parts.size == 3) { "Ожидается groupId:artifactId:version, получено: $spec" }
            val (g, a, v) = parts
            require(g.isNotBlank() && a.isNotBlank() && v.isNotBlank()) { "Пустые части координат: $spec" }
            return MavenCoordinates(g, a, v)
        }
    }
}