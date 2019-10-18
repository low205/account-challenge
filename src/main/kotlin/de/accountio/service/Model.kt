package de.accountio.service

class PageRequest(
    val limit: Int,
    val next: String
)

class PageResponse<T : Any>(
    val nextCursor: String,
    val accounts: List<T>
)
