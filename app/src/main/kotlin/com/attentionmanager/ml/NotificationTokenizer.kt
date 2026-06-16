package com.attentionmanager.ml

object NotificationTokenizer {
    private val tokenRegex = Regex("""[\p{L}\p{N}_$%]+""")

    fun tokenize(text: String, maxTokens: Int = 128): IntArray {
        val tokens = tokenRegex.findAll(text.lowercase())
            .map { it.value }
            .take(maxTokens)
            .map { stableTokenId(it) }
            .toList()
        return IntArray(maxTokens) { index -> tokens.getOrElse(index) { 0 } }
    }

    private fun stableTokenId(token: String): Int {
        var hash = 7
        token.forEach { char -> hash = 31 * hash + char.code }
        return (hash and Int.MAX_VALUE) % 30_000 + 1
    }
}
