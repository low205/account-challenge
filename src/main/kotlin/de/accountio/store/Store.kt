package de.accountio.store

import java.util.concurrent.atomic.AtomicLong

interface StorableEntity {
    val id: Long
}

open class Store<T : StorableEntity> internal constructor() {
    private val entities = mutableMapOf<Long, T>()
    fun findById(id: Long) = entities[id]
    fun save(entity: T): T {
        entities[entity.id] = entity
        return entity
    }

    fun getAll() = entities.values.toList()
    private val sequence: AtomicLong = AtomicLong(0)
    fun nextId() = sequence.incrementAndGet()
}

class EntityNotFoundException(val id: Long) : RuntimeException()
