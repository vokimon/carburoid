package tasks

import kotlin.io.path.createTempFile
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class YamlToAndroidStringsTaskTest {

    @Test
    fun `escapeAndroidString should escape special characters`() {
        val input = "Line1\nLine2\tTabbed\\Backslash\"Quote'Single"
        val expected = "Line1\\nLine2\\tTabbed\\\\Backslash\\\"Quote\\'Single"
        val actual = escapeAndroidString(input)
        assertEquals(expected, actual)
    }

    @Test
    fun `escapeAndroidString should escape double question marks`() {
        val input = "Do you mean ??"
        val expected = "Do you mean \\?\\?"
        val actual = escapeAndroidString(input)
        assertEquals(expected, actual)
    }

    private fun assertExtractParams(input: String, expected: List<String>) {
        val result = extractParams(input)
        assertEquals(
            expected,
            result,
            "Input: '$input'\nExpected: $expected\nActual: $result\n",
        )
    }

    @Test
    fun `extractParams with no params returns empty list`() {
        assertExtractParams(
            "Hello world",
            emptyList(),
        )
    }

    @Test
    fun `extractParams with one param without format returns one name with s`() {
        assertExtractParams(
            "Hello {name}",
            listOf("name"),
        )
    }

    @Test
    fun `extractParams with one param with format returns name and format`() {
        assertExtractParams(
            "Hello {name:d}",
            listOf("name"),
        )
    }

    @Test
    fun `extractParams with spaces inside brackets`() {
        assertExtractParams("{ name }", listOf("name"))
        assertExtractParams("{ name:d }", listOf("name"))
    }

    @Test
    fun `extractParams with spaces around colon`() {
        assertExtractParams("{name : d}", listOf("name"))
    }

    @Test
    fun `extractParams with multiple params returns correct list`() {
        assertExtractParams(
            "Hello {param1} and {param2:d}",
            listOf("param1", "param2"),
        )
    }

    @Test
    fun `extractParams with repeated param collects it just once`() {
        assertExtractParams(
            "Hello {name}, your age is {age}, {name} again.",
            listOf("name", "age"),
        )
    }

    @Test
    fun `extractParams ignores escaped curly braces`() {
        assertExtractParams(
            "Hello {{user}}",
            emptyList(),
        )
    }

    @Test
    fun `extractParams triple curly braces`() {
        assertExtractParams(
            "Hello {{{user}}}",
            listOf("user"),
        )
    }

    // format params

    private fun assertParametersToXml(template: String, params: List<String>, expected: String) {
        val result = parametersToXml(template, params)
        assertEquals(expected, result, "Template: '$template'\nExpected: '$expected'\nActual: '$result'\n")
    }

    @Test
    fun `parametersToXml with no params returns original string`() {
        assertParametersToXml(
            template = "Just a plain string",
            params = emptyList(),
            expected = "Just a plain string",
        )
    }

    @Test
    fun `parametersToXml replaces one param with numbered format`() {
        assertParametersToXml(
            template = "Hello {name}",
            params = listOf("name"),
            expected = "Hello %1\$s",
        )
    }

    @Test
    fun `parametersToXml replaces other param with numbered format`() {
        assertParametersToXml(
            template = "Bye {user}",
            params = listOf("user"),
            expected = "Bye %1\$s",
        )
    }

    @Test
    fun `parametersToXml with format spect, use that`() {
        assertParametersToXml(
            template = "Hello {user:d}",
            params = listOf("user"),
            expected = "Hello %1\$d",
        )
    }

    @Test
    fun `parametersToXml with missing param throws MismatchedParamException`() {
        val template = "Hello {second}"
        val params = listOf("first") // Missing "second"

        val exception = assertFailsWith<MismatchedParamException> {
            parametersToXml(template, params)
        }

        assertEquals(exception.paramName, "second")
    }

    @Test
    fun `parametersToXml trims spaces before name`() {
        assertParametersToXml(
            template = "Hello { name}",
            params = listOf("name"),
            expected = "Hello %1\$s",
        )
    }

    @Test
    fun `parametersToXml trims spaces after name`() {
        assertParametersToXml(
            template = "Hello {name }",
            params = listOf("name"),
            expected = "Hello %1\$s",
        )
    }

    @Test
    fun `parametersToXml trims spaces around format spec`() {
        assertParametersToXml(
            template = "Hello {name : spec }",
            params = listOf("name"),
            expected = "Hello %1\$spec",
        )
    }

    @Test
    fun `parametersToXml replaces multiple params with numbered format`() {
        assertParametersToXml(
            template = "Hello {first}, you are {age:d} years old",
            params = listOf("first", "age"),
            expected = "Hello %1\$s, you are %2\$d years old",
        )
    }

    @Test
    fun `parametersToXml with escapped braces do not substitute`() {
        assertParametersToXml(
            template = "Hello {{name}}",
            params = listOf("name"),
            expected = "Hello {name}",
        )
    }

    @Test
    fun `parametersToXml with triple braces takes inner`() {
        assertParametersToXml(
            template = "Hello {{{name}}}",
            params = listOf("name"),
            expected = "Hello {%1\$s}",
        )
    }

    fun assertParameterOrderFromYaml(yamlContent: String, expected: Map<String, List<String>>) {
        val yamlFile = createTempFile(prefix = "tempYaml", suffix = ".yaml")
        yamlFile.writeText(yamlContent)

        val result = parameterOrderFromYaml(yamlFile.toFile())

        assertEquals(expected, result)
    }

    @Test
    fun `parameterOrderFromYaml single parameter`() {
        assertParameterOrderFromYaml(
            yamlContent = """
                greeting: "Hello {name}"
                """,
            expected = mapOf("greeting" to listOf("name")),
        )
    }

    @Test
    fun `parameterOrderFromYaml multiple parameters`() {
        assertParameterOrderFromYaml(
            yamlContent = """
                greeting: "Hello {name}, welcome to {place}"
            """,
            expected = mapOf("greeting" to listOf("name", "place")),
        )
    }

    @Test
    fun `parameterOrderFromYaml strings with no parameters`() {
        assertParameterOrderFromYaml(
            yamlContent = """
                farewell: "Goodbye"
            """,
            expected = mapOf("farewell" to emptyList<String>()),
        )
    }

    @Test
    fun `parameterOrderFromYaml ignores escaped curly braces`() {
        assertParameterOrderFromYaml(
            yamlContent = """
                info: "This is a {{escaped}} text."
            """,
            expected = mapOf("info" to emptyList<String>()),
        )
    }

    @Test
    fun `parameterOrderFromYaml ignores format specifiers`() {
        val yamlContent = """
        info: "This is a {param:number} text."
        """
        val expected = mapOf("info" to listOf("param"))

        assertParameterOrderFromYaml(yamlContent, expected)
    }

    @Test
    fun `parameterOrderFromYaml multiple texts`() {
        assertParameterOrderFromYaml(
            yamlContent = """
                greeting: "Hello {name}, welcome to {place}"
                farewell: "Bye {name}"
            """,
            expected = mapOf(
                "greeting" to listOf("name", "place"),
                "farewell" to listOf("name"),
            ),
        )
    }

    @Test
    fun `parameterOrderFromYaml hierarchical keys`() {
        assertParameterOrderFromYaml(
            yamlContent = """
                parent:
                   greeting: "Hello {name}"
            """,
            expected = mapOf(
                "parent__greeting" to listOf("name"),
            ),
        )
    }

    @Test
    fun `parameterOrderFromYaml deep hierarchical keys`() {
        assertParameterOrderFromYaml(
            yamlContent = """
                grandpa:
                    parent:
                        greeting: "Hello {name}"
            """,
            expected = mapOf(
                "grandpa__parent__greeting" to listOf("name"),
            ),
        )
    }
}
