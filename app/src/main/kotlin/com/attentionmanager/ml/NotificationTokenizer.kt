package com.attentionmanager.ml

object NotificationTokenizer {
    const val MAX_TOKENS = 128
    const val MAX_TOKEN_ID = 30_000

    private val tokenRegex = Regex("""[\p{L}\p{N}_$%]+""")

    fun tokenize(text: String, maxTokens: Int = MAX_TOKENS): IntArray {
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
        return (hash and Int.MAX_VALUE) % MAX_TOKEN_ID + 1
    }
}
