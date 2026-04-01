package org.deafsapps.storeit.data.database

private const val TAG_SEPARATOR = '\u001F'

internal fun encodeTags(tags: List<String>): String = tags.joinToString(separator = TAG_SEPARATOR.toString())

internal fun decodeTags(encoded: String): List<String> = if (encoded.isEmpty()) {
    emptyList()
} else {
    encoded.split(TAG_SEPARATOR)
}
