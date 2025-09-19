package dev.dn5s.lthread.model

import java.time.Instant

data class Thread(
    val id: Long,
    val boardName: String,
    val postIds: MutableList<Long> = mutableListOf(),
    val lastPostTime: Instant = Instant.now(),
    val createdAt: Instant = Instant.now()
) {
    val replyCount: Int
        get() = postIds.size - 1 // Excluding OP

    val originalPostId: Long?
        get() = postIds.firstOrNull()
}