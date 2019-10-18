package de.accountio.store

import java.util.concurrent.atomic.AtomicLong

interface StorableEntity {
    val id: Long
}

open class Store<T : StorableEntity> internal constructor(val minIndex: Long = 0) {
    private val entities = mutableMapOf<Long, T>()
    fun findById(id: Long) = entities[id]
    fun getById(id: Long) = findById(id) ?: throw EntityNotFoundException(id)
    fun save(entity: T): T {
        entities[entity.id] = entity
        return entity
    }

    open fun getAll() = entities.values.toList()
    private val sequence: AtomicLong = AtomicLong(minIndex)
    fun nextId() = sequence.incrementAndGet()
}

class EntityNotFoundException(val id: Long) : RuntimeException()
