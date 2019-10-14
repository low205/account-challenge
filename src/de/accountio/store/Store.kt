package de.accountio.de.accountio.store

import java.util.concurrent.atomic.AtomicLong

interface StorableEntity {
    val id: Long
}

open class Store<T : StorableEntity> internal constructor() {
    private val entities = mutableMapOf<Long, T>()
    protected fun findById(id: Long) = entities[id]
    protected fun save(entity: T) {
        entities[entity.id] = entity
    }

    protected fun getAll() = entities.values.toList()
    private val sequence: AtomicLong = AtomicLong(0)
    protected fun nextId() = sequence.incrementAndGet()
    protected fun delete(id: Long) = entities.remove(id)
}
