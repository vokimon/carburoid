package tasks

import kotlin.test.Test
import kotlin.test.Ignore
import kotlin.test.assertEquals

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

    @Test
    fun `extractParams with no params returns empty list`() {
        val input = "Hello world"
        val result = extractParams(input)
        assertEquals(emptyList(), result)
    }

    @Test
    fun `extractParams with one param without format returns one name with s`() {
        val input = "Hello {name}"
        val result = extractParams(input)
        assertEquals(listOf("name" to "s"), result)
    }

    @Ignored
    @Test
    fun `extractParams with one param with format returns name and format`() {
        val input = "Hello {name:d}"
        val result = extractParams(input)
        assertEquals(listOf("name" to "d"), result)
    }

}

