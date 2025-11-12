package com.melancholicbastard.maven

import org.w3c.dom.Element
import java.io.ByteArrayInputStream
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory

object PomParser {

    data class ProjectInfo(
        val groupId: String?,
        val artifactId: String?,
        val version: String?,
        val properties: Map<String, String>
    )

    fun parseDirectDependencies(pomXml: String): List<Dependency> {
        val doc = newSecureFactory().newDocumentBuilder()
            .parse(ByteArrayInputStream(pomXml.toByteArray(Charsets.UTF_8)))
        doc.documentElement.normalize()

        val project = doc.documentElement
        val parentEl = project.getFirst("parent")
        val projGroup = project.getText("groupId") ?: parentEl?.getText("groupId")
        val projArtifact = project.getText("artifactId")
        val projVersion = project.getText("version") ?: parentEl?.getText("version")

        val props = mutableMapOf<String, String>()
        // project.* для плейсхолдеров
        projGroup?.let { props["project.groupId"] = it }
        projArtifact?.let { props["project.artifactId"] = it }
        projVersion?.let { props["project.version"] = it }
        // <properties>
        project.getFirst("properties")?.let { propsEl ->
            propsEl.childNodes.let { nodes ->
                for (i in 0 until nodes.length) {
                    val n = nodes.item(i)
                    if (n is Element) {
                        val k = n.tagName
                        val v = n.textContent?.trim().orEmpty()
                        if (k.isNotEmpty() && v.isNotEmpty()) props[k] = v
                    }
                }
            }
        }

        val depsRoot = project.getFirst("dependencies") ?: return emptyList()
        val out = mutableListOf<Dependency>()
        depsRoot.childNodes.let { nodes ->
            for (i in 0 until nodes.length) {
                val n = nodes.item(i)
                if (n is Element && n.tagName == "dependency") {
                    val g = n.getText("groupId")?.let { resolve(it, props) } ?: continue
                    val a = n.getText("artifactId")?.let { resolve(it, props) } ?: continue
                    val vRaw = n.getText("version")
                    val v = vRaw?.let { resolve(it, props) }
                    val scope = n.getText("scope")?.trim()?.ifEmpty { null }
                    out += Dependency(g, a, v, scope)
                }
            }
        }
        return out
    }

    private fun resolve(s: String, props: Map<String, String>): String {
        var result = s
        var guard = 0
        val regex = Regex("""\$\{([^}]+)}""")
        while (true) {
            guard++
            if (guard > 10) break
            val m = regex.find(result) ?: break
            val key = m.groupValues[1]
            val value = props[key] ?: break
            result = result.replace("\${$key}", value)
        }
        return result
    }

    private fun Element.getFirst(tag: String): Element? {
        val list = this.getElementsByTagName(tag)
        return (0 until list.length)
            .asSequence()
            .map { list.item(it) }
            .filterIsInstance<Element>()
            .firstOrNull { it.parentNode == this }
    }

    private fun Element.getText(tag: String): String? =
        getFirst(tag)?.textContent?.trim()?.takeIf { it.isNotEmpty() }

    private fun newSecureFactory(): DocumentBuilderFactory {
        val f = DocumentBuilderFactory.newInstance()
        f.isNamespaceAware = false
        f.isXIncludeAware = false
        f.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true)
        f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
        return f
    }
}