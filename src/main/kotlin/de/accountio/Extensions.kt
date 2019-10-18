package de.accountio

import kotlinx.coroutines.CompletableDeferred
import java.math.BigDecimal
import java.math.BigDecimal.ZERO

suspend inline fun <reified T> await(block: (CompletableDeferred<T>) -> Unit): T {
    val defer = CompletableDeferred<T>()
    block(defer)
    return defer.await()
}

inline fun <T> Iterable<T>.sumByBigDecimal(selector: (T) -> BigDecimal): BigDecimal {
    var sum: BigDecimal = ZERO
    for (element in this) {
        sum += selector(element)
    }
    return sum
}

inline fun <R> CompletableDeferred<R>.use(block: (CompletableDeferred<R>) -> R) {
    try {
        val result = block(this)
        this.complete(result)
    } catch (t: Throwable) {
        this.completeExceptionally(t)
    }
}
