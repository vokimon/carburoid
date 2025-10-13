package tasks

import kotlin.test.Test
import kotlin.test.Ignore
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.fail

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
            "Input: '$input'\nExpected: $expected\nActual: $result\n"
        )
    }

    @Test
    fun `extractParams with no params returns empty list`() {
        assertExtractParams(
            "Hello world",
            emptyList()
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
            listOf("param1", "param2")
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
    private fun assertParametersToXmlOld(template: String, params: List<Pair<String, String>>, expected: String) {
        assertParametersToXml(template, params.map {it.first}, expected)
    }

    @Test
    fun `parametersToXml with no params returns original string`() {
        assertParametersToXmlOld(
            template = "Just a plain string",
            params = emptyList(),
            expected = "Just a plain string"
        )
    }

    @Test
    fun `parametersToXml replaces one param with numbered format`() {
        assertParametersToXmlOld(
            template = "Hello {name}",
            params = listOf("name" to "s"),
            expected = "Hello %1\$s"
        )
    }

    @Test
    fun `parametersToXml replaces other param with numbered format`() {
        assertParametersToXmlOld(
            template = "Bye {user}",
            params = listOf("user" to "s"),
            expected = "Bye %1\$s"
        )
    }

    @Test
    fun `parametersToXml with format spect, use that`() {
        assertParametersToXmlOld(
            template = "Hello {user:d}",
            params = listOf("user" to "d"),
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
        assertParametersToXmlOld(
            template = "Hello { name}",
            params = listOf("name" to "s"),
            expected = "Hello %1\$s"
        )
    }

    @Test
    fun `parametersToXml trims spaces after name`() {
        assertParametersToXmlOld(
            template = "Hello {name }",
            params = listOf("name" to "s"),
            expected = "Hello %1\$s"
        )
    }

    @Test
    fun `parametersToXml trims spaces around format spec`() {
        assertParametersToXmlOld(
            template = "Hello {name : spec }",
            params = listOf("name" to "spec"),
            expected = "Hello %1\$spec"
        )
    }

    @Test
    fun `parametersToXml replaces multiple params with numbered format`() {
        assertParametersToXmlOld(
            template = "Hello {first}, you are {age:d} years old",
            params = listOf("first" to "s", "age" to "d"),
            expected = "Hello %1\$s, you are %2\$d years old"
        )
    }

    @Test
    fun `parametersToXml with escapped braces do not substitute`() {
        assertParametersToXmlOld(
            template = "Hello {{name}}",
            params = listOf("name" to "s"),
            expected = "Hello {name}"
        )
    }

    @Test
    fun `parametersToXml with triple braces takes inner`() {
        assertParametersToXmlOld(
            template = "Hello {{{name}}}",
            params = listOf("name" to "s"),
            expected = "Hello {%1\$s}"
        )
    }


}

