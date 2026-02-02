package net.canvoki.carburoid.test

import com.github.difflib.DiffUtils
import com.github.difflib.patch.AbstractDelta
import com.github.difflib.patch.DeltaType

private const val RED = "\u001B[31m❮"
private const val GREEN = "\u001B[32m❮"
private const val RESET = "❯\u001B[0m"

private fun charLevelDiff(
    old: String,
    new: String,
): Pair<String, String> {
    val diff = DiffUtils.diff(old.toList(), new.toList())

    val oldOut = StringBuilder()
    val newOut = StringBuilder()

    var oldPos = 0
    var newPos = 0

    for (delta in diff.deltas) {
        val src = delta.source
        val tgt = delta.target

        // unchanged prefix
        while (oldPos < src.position) oldOut.append(old[oldPos++])
        while (newPos < tgt.position) newOut.append(new[newPos++])

        when (delta.type) {
            DeltaType.DELETE -> {
                oldOut.append(RED)
                src.lines.forEach { oldOut.append(it) }
                oldOut.append(RESET)
                newOut.append(GREEN).append(RESET) // placeholder
                oldPos += src.size()
            }
            DeltaType.INSERT -> {
                newOut.append(GREEN)
                tgt.lines.forEach { newOut.append(it) }
                newOut.append(RESET)
                oldOut.append(RED).append(RESET) // placeholder
                newPos += tgt.size()
            }
            DeltaType.CHANGE -> {
                oldOut.append(RED)
                src.lines.forEach { oldOut.append(it) }
                oldOut.append(RESET)

                newOut.append(GREEN)
                tgt.lines.forEach { newOut.append(it) }
                newOut.append(RESET)

                oldPos += src.size()
                newPos += tgt.size()
            }
            else -> {}
        }
    }

    // tail
    while (oldPos < old.length) oldOut.append(old[oldPos++])
    while (newPos < new.length) newOut.append(new[newPos++])

    return oldOut.toString() to newOut.toString()
}

private fun hunkHeader(delta: AbstractDelta<String>): String {
    val src = delta.source
    val tgt = delta.target
    return "@@ -${src.position + 1},${src.size()} +${tgt.position + 1},${tgt.size()} @@"
}

private fun contextBefore(
    lines: List<String>,
    start: Int,
    context: Int,
): List<String> = lines.subList(maxOf(0, start - context), start)

private fun contextAfter(
    lines: List<String>,
    end: Int,
    context: Int,
): List<String> = lines.subList(end, minOf(lines.size, end + context))

fun assertEquals(
    expected: String,
    actual: String,
    contextLines: Int = 3,
) {
    val expectedLines = expected.lines()
    val actualLines = actual.lines()

    val patch = DiffUtils.diff(expectedLines, actualLines)

    if (patch.deltas.isEmpty()) return

    val message =
        buildString {
            appendLine("Multiline diff (expected vs actual):")
            appendLine()

            for (delta in patch.deltas) {
                appendLine(hunkHeader(delta))

                val srcPos = delta.source.position
                val srcEnd = srcPos + delta.source.size()

                // context before
                contextBefore(expectedLines, srcPos, contextLines).forEach {
                    appendLine("  $it")
                }

                // diff lines
                when (delta.type) {
                    DeltaType.DELETE ->
                        delta.source.lines.forEach {
                            appendLine("$RED- $it$RESET")
                        }
                    DeltaType.INSERT ->
                        delta.target.lines.forEach {
                            appendLine("$GREEN+ $it$RESET")
                        }
                    DeltaType.CHANGE -> {
                        val oldLines = delta.source.lines
                        val newLines = delta.target.lines
                        val max = maxOf(oldLines.size, newLines.size)
                        repeat(max) { i ->
                            val old = oldLines.getOrNull(i)
                            val new = newLines.getOrNull(i)
                            if (old != null && new != null) {
                                val (oldDiff, newDiff) = charLevelDiff(old, new)
                                appendLine("- $oldDiff")
                                appendLine("+ $newDiff")
                            } else if (old != null) {
                                appendLine("$RED- $old$RESET")
                            } else if (new != null) {
                                appendLine("$GREEN+ $new$RESET")
                            }
                        }
                    }
                    else -> {}
                }

                // context after
                contextAfter(expectedLines, srcEnd, contextLines).forEach {
                    appendLine("  $it")
                }

                appendLine()
                appendLine()
            }
        }

    throw AssertionError(message)
}
