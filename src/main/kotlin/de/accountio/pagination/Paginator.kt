package de.accountio.pagination

import de.accountio.service.PageRequest
import de.accountio.store.StorableEntity
import org.hashids.Hashids

private val HASH_IDS = Hashids("Accounting Challenge Salt")

class Paginator(pageRequest: PageRequest, val minIndex: Long) {
    val edgeId: Long = when {
        pageRequest.next.isNotBlank() -> HASH_IDS.decode(pageRequest.next)
        else -> longArrayOf()
    }.let { numbers ->
        when {
            numbers.isEmpty() -> minIndex
            else -> numbers.first()
        }
    }

    fun <T : StorableEntity> nextCursorFor(list: List<T>): String =
        HASH_IDS.encode(list.lastOrNull()?.run { id } ?: minIndex)
}
