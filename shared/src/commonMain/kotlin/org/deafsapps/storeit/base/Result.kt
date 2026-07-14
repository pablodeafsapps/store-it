package org.deafsapps.storeit.base

/**
 * A right-biased disjoint union for success (Ok) and failure (Err).
 */
sealed interface Result<out E, out A> {
    val isOk: Boolean get() = this is Ok
    val isErr: Boolean get() = this is Err

    companion object {
        fun <A> ok(value: A): Result<Nothing, A> = Ok(value = value)

        fun <E> err(error: E): Result<E, Nothing> = Err(error = error)

        inline fun <E, V> catch(block: () -> V, mapError: (Throwable) -> E): Result<E, V> =
            try {
                Ok(value = block())
            } catch (t: Throwable) {
                Err(error = mapError(t))
            }

        inline fun <E, A> A.fromNullable(ifNull: () -> E): Result<E, A> =
            if (this != null) Ok(value = this) else Err(error = ifNull())

        inline fun <T : Boolean, E> T.fromBoolean(ifFalse: () -> E): Result<E, Unit> =
            if (this) Ok(value = Unit) else Err(error = ifFalse())

        inline fun <E, A, B, C> zip(
            ra: Result<E, A>,
            rb: Result<E, B>,
            crossinline combine: (A, B) -> C
        ): Result<E, C> =
            ra.flatMap { a -> rb.map { b -> combine(a, b) } }

        inline fun <E, A> combine(
            vararg results: Result<E, *>,
            crossinline build: () -> A
        ): Result<E, A> {
            for (r in results) if (r is Err) return r
            return Ok(value = build())
        }
    }
}

data class Ok<out A>(val value: A) : Result<Nothing, A>

data class Err<out E>(val error: E) : Result<E, Nothing>

fun <E, V> Result<E, V>.getOrNull(): V? = (this as? Ok<V>)?.value

fun <E, V> Result<E, V>.leftOrNull(): E? = (this as? Err<E>)?.error

fun <E, V> Result<E, V>.getOrDefault(default: V): V = getOrNull() ?: default

inline fun <E, V, B> Result<E, V>.map(onSuccess: (V) -> B): Result<E, B> =
    (this as? Ok)?.run { Ok(value = onSuccess(value)) } ?: this as Err

inline fun <E, V, EE> Result<E, V>.mapLeft(onFailure: (E) -> EE): Result<EE, V> =
    (this as? Err)?.run { Err(error = onFailure(error)) } ?: this as Ok

inline fun <E, V, B> Result<E, V>.flatMap(onSuccess: (V) -> Result<E, B>): Result<E, B> =
    if (this is Ok) {
        onSuccess(value)
    } else {
        this as Err
    }

suspend inline fun <E, V, B> Result<E, V>.suspendFlatMap(
    onSuccess: suspend (V) -> Result<E, B>
): Result<E, B> = if (this is Ok) {
    onSuccess(value)
} else {
    this as Err
}

inline fun <E, V, EE> Result<E, V>.flatFailure(onFailure: (E) -> Result<EE, V>): Result<EE, V> =
    if (this is Err) {
        onFailure(error)
    } else {
        this as Ok
    }

suspend inline fun <E, V, EE> Result<E, V>.suspendFlatFailure(
    onFailure: suspend (E) -> Result<EE, Nothing>
): Result<EE, V> = if (this is Err) {
    onFailure(error)
} else {
    this as Ok
}

inline fun <E, V, R> Result<E, V>.fold(
    ifErr: (E) -> R,
    ifOk: (V) -> R,
): R = when (val err = failureOrNull()) {
    null -> ifOk((this as Ok<V>).value)
    else -> ifErr(err)
}

inline fun <E, V, R> Result<E, V>.foldMap(
    ifOk: (V) -> Result<E, R>,
): Result<E, R> = when (val err = failureOrNull()) {
    null -> ifOk((this as Ok<V>).value)
    else -> err.err()
}

inline fun <E, V, EE> Result<E, V>.foldFailure(
    ifErr: (E) -> Result<EE, V>,
): Result<EE, V> = when (val err = failureOrNull()) {
    null -> this as Ok<V>
    else -> ifErr(err)
}

inline infix fun <E, V> Result<E, V>.onOk(action: (V) -> Unit): Result<E, V> =
    also { if (this is Ok) action(value) }

inline infix fun <E, V> Result<E, V>.onErr(action: (E) -> Unit): Result<E, V> =
    also { if (this is Err) action(error) }

inline fun <E, V, EE : V> Result<E, V>.getOrElse(onFailure: (Err<E>) -> EE): V =
    (this as? Ok)?.value ?: onFailure(this as Err)

inline fun <E, V> Result<E, V>.getOrThrow(mapLeftToThrowable: (E) -> Throwable): V =
    (this as? Ok)?.value ?: throw mapLeftToThrowable((this as Err).error)

fun <E, V> Result<E, V>.failureOrNull(): E? = (this as? Err<E>)?.error

fun <E, V> Result<E, V>.swap(): Result<V, E> = when (this) {
    is Ok -> Err(error = value)
    is Err -> Ok(value = error)
}

// Syntax helpers
fun <V> V.ok(): Result<Nothing, V> = Ok(value = this)

fun <E> E.err(): Result<E, Nothing> = Err(error = this)

fun <E, V> Result<E, V>.and(vararg others: Result<E, V>): Result<E, V> =
    if (this is Ok && others.toList().all { o -> o is Ok }) {
        this
    } else {
        (this as? Err) ?: (others.toList().first { it is Err })
    }
