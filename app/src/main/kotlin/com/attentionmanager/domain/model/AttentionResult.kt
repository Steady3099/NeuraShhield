package com.attentionmanager.domain.model

sealed interface AttentionResult<out T> {
    data class Success<T>(val value: T) : AttentionResult<T>
    data class Failure(val error: AttentionError) : AttentionResult<Nothing>
}

sealed class AttentionError(
    open val message: String,
    open val cause: Throwable? = null
) {
    data class Database(override val message: String, override val cause: Throwable? = null) :
        AttentionError(message, cause)

    data class Ml(override val message: String, override val cause: Throwable? = null) :
        AttentionError(message, cause)

    data class Permission(override val message: String) : AttentionError(message)

    data class Unknown(override val message: String, override val cause: Throwable? = null) :
        AttentionError(message, cause)
}

inline fun <T> attentionRunCatching(
    crossinline block: () -> T
): AttentionResult<T> = try {
    AttentionResult.Success(block())
} catch (throwable: Throwable) {
    AttentionResult.Failure(AttentionError.Unknown(throwable.message ?: "Unknown error", throwable))
}
