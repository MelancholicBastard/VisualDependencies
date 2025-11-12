package com.melancholicbastard.maven

import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URI

object MavenRepository {

    fun loadPom(repoPath: String, coords: MavenCoordinates): String {
        return if (repoPath.startsWith("http://") || repoPath.startsWith("https://")) {
            fetchRemotePom(repoPath, coords)
        } else {
            readLocalPom(repoPath, coords)
        }
    }

    private fun fetchRemotePom(baseUrl: String, coords: MavenCoordinates): String {
        val normalized = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        val url = URI(normalized + coords.toPomPath()).toURL()
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            instanceFollowRedirects = true
            connectTimeout = 15000
            readTimeout = 15000
        }
        conn.connect()
        if (conn.responseCode != 200) {
            val msg = conn.errorStream?.readBytes()?.toString(Charsets.UTF_8)
            throw IOException("HTTP ${conn.responseCode} при получении POM: ${conn.url} ${msg ?: ""}".trim())
        }
        return conn.inputStream.use { it.readBytes().toString(Charsets.UTF_8) }
    }

    private fun readLocalPom(baseDir: String, coords: MavenCoordinates): String {
        val file = File(baseDir).resolve(coords.toPomPath())
        if (!file.exists()) {
            throw IOException("POM не найден локально: ${file.absolutePath}")
        }
        return file.readText()
    }
}