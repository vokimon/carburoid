package tasks

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import java.io.File
import java.util.*
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import org.gradle.api.GradleException
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

typealias ParamList = List<String>
typealias ParamCatalog = Map<String, ParamList>

fun escapeAndroidString(input: String): String {
    var escaped = input
        .replace("\\", "\\\\")
        .replace("'", "\\'")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\t", "\\t")
        .replace("\r", "\\r")
    if (escaped.startsWith("??")) {
        escaped = "\\?" + escaped.substring(1)
    } else if (escaped.contains("??")) {
        escaped = escaped.replace("??", "\\?\\?")
    }
    return escaped
}

class MismatchedParamException(val paramName: String) : Exception("Parameter '$paramName' not found in provided params.")

fun extractParams(template: String): ParamList {
    val tempTemplate = template.replace("{{", "<escaped_open>")
    val regex = "\\{([^}:]+)(?::([^}]+))?}".toRegex()

    return regex.findAll(tempTemplate)
        .map { it.groupValues[1].trim() }
        .distinct()
        .toList()
}

fun parametersToXml(template: String, params: ParamList): String {
    val tempTemplate = template.replace("{{", "<escaped_open>")
    val regex = "\\{([^}:]+)(?::([^}]+))?}".toRegex()

    return regex.replace(tempTemplate) { match ->
        val paramName = match.groupValues[1].trim()
        val format = match.groupValues.getOrNull(2)?.trim()?.takeIf { it.isNotEmpty() } ?: "s"
        val index = params.indexOf(paramName)
        if (index < 0) {
            throw MismatchedParamException(paramName)
        }
        "%${index+1}\$${format}"
    }.replace("<escaped_open>", "{").replace("}}", "}")
}

fun parameterOrderFromYaml(yamlFile: File): ParamCatalog {
    val mapper = ObjectMapper(YAMLFactory())
    val yamlContent = mapper.readValue(yamlFile, Map::class.java) as Map<*, *>
    val result = mutableMapOf<String, ParamList>()

    fun processKey(content: Map<*, *>, prefix: String="") {
        content.forEach { (key, value) ->
            val fullKey = prefix + (key as String)
            when (value) {
                is Map<*, *> -> {
                    processKey(value, prefix="${fullKey}__")

                }
                is String -> {
                    val parameters = extractParams(value as String)
                    result[fullKey] = parameters
                }
            }
        }
    }
    processKey(yamlContent)
    return result
}

open class YamlToAndroidStringsTask : DefaultTask() {
    @InputDirectory
    var yamlDir: File = project.projectDir.resolve("src/main/translations")

    @OutputDirectory
    var resDir: File = project.projectDir.resolve("src/main/res")

    @Input
    var defaultLanguage: String = "en"

    private val errors = mutableListOf<String>()

    private fun writeArraysFile(resDir: File, languageCodes: Set<String>) {
        val arraysFile = File(resDir, "values/arrays_languages.xml")
        arraysFile.parentFile.mkdirs()
        arraysFile.writeText(
            buildString {
                appendLine("""<?xml version="1.0" encoding="utf-8"?>""")
                appendLine("<!-- AUTO-GENERATED FILE. DO NOT EDIT MANUALLY. -->")
                appendLine("<!-- This file is generated automatically from YAML translations. -->")
                appendLine("<!-- Any manual changes will be overwritten. -->")
                appendLine("<resources>")
                appendLine("    <string-array name=\"supported_language_codes\">")
                languageCodes.forEach {
                    appendLine("        <item>$it</item>")
                }
                appendLine("    </string-array>")
                appendLine("</resources>")
            }
        )
        println("Generated language arrays: ${languageCodes.joinToString(", ")}")
    }

    fun getLanguageCodes(yamlDir: File): SortedSet<String> {
        return yamlDir
            .listFiles { it -> it.extension in listOf("yml", "yaml") }
            ?.map { it.nameWithoutExtension.lowercase(Locale.ROOT) }
            ?.toSortedSet()
            ?: emptySet<String>().toSortedSet()
    }

    private fun yamlForLanguage(yamlDir: File, langCode: String): File {
        return yamlDir.resolve("$langCode.yaml").takeIf { it.exists() }
            ?: yamlDir.resolve("$langCode.yml")

    }

    @TaskAction
    fun run() {
        if (!yamlDir.exists()) {
            println("Translations directory not found: ${yamlDir.absolutePath}")
            return
        }

        val paramCatalog = parameterOrderFromYaml(yamlForLanguage(yamlDir, defaultLanguage))
        val languageCodes = getLanguageCodes(yamlDir)
        writeArraysFile(resDir, languageCodes)

        languageCodes.forEach { langCode ->
            val file = yamlForLanguage(yamlDir, langCode)

            if (!file.exists()) return@forEach

            val qualifier = if (langCode == defaultLanguage || langCode == "default") "values" else "values-$langCode"
            val targetDir = File(resDir, qualifier)
            targetDir.mkdirs()

            val xmlFile = File(targetDir, "strings.xml")
            convertYamlToAndroidXml(file, xmlFile, paramCatalog)
            println("Generated: ${xmlFile.absolutePath}")
        }
        if (errors.isNotEmpty()) {
            throw GradleException("Errors found:\n${errors.joinToString("\n")}")
        }
    }

    private fun convertYamlToAndroidXml(yamlFile: File, xmlFile: File, paramCatalog: ParamCatalog) {

        fun processYamlMap(map: Map<*, *>, prefix: String, resources: org.w3c.dom.Element, paramCatalog: ParamCatalog) {
            val doc = resources.ownerDocument
            map.forEach { (key, value) ->
                val fullKey = "$prefix$key"
                when (value) {
                    is Map<*, *> -> processYamlMap(value as Map<*, *>, "${fullKey}__", resources, paramCatalog)
                    is String -> {
                        val stringElem = doc.createElement("string")
                        stringElem.setAttribute("name", fullKey.lowercase(Locale.ROOT))
                        val paramList = paramCatalog[fullKey] ?: emptyList()
                        val valueWithPositionalParameters = try {
                            parametersToXml(value, paramList)
                        } catch(e: MismatchedParamException) {
                            errors.add(
                                """
                                Key "${fullKey}" has a parameter "${e.paramName}" not present the original string.
                                    File: ${yamlFile}
                                """.trimIndent()
                            )
                            value // keep the old string and continue
                        }
                        stringElem.textContent = escapeAndroidString(valueWithPositionalParameters)
                        resources.appendChild(stringElem)
                    }
                }
            }
        }

        val mapper = ObjectMapper(YAMLFactory())
        val yamlContent = mapper.readValue(yamlFile, Map::class.java) as Map<*, *>

        val docBuilder = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }.newDocumentBuilder()
        val doc = docBuilder.newDocument()

        val comment = doc.createComment(" AUTO-GENERATED from ${yamlFile.name}. DO NOT EDIT THIS FILE DIRECTLY! ")
        doc.appendChild(comment)

        val resources = doc.createElement("resources")
        doc.appendChild(resources)

        processYamlMap(yamlContent, "", resources, paramCatalog)

        val transformer = TransformerFactory.newInstance().newTransformer().apply {
            setOutputProperty(OutputKeys.INDENT, "yes")
            setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4")
            setOutputProperty(OutputKeys.ENCODING, "utf-8")
            setOutputProperty(OutputKeys.STANDALONE, "yes")
        }

        transformer.transform(DOMSource(doc), StreamResult(xmlFile))

    }

}

