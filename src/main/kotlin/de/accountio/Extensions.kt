package de.accountio

import kotlinx.coroutines.CompletableDeferred

suspend inline fun <reified T> await(block: (CompletableDeferred<T>) -> Unit): T {
    val defer = CompletableDeferred<T>()
    block(defer)
    return defer.await()
}
