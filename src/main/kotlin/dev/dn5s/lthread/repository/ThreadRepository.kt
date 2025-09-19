package dev.dn5s.lthread.repository

import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
class ThreadRepository(
    private val stringRedisTemplate: StringRedisTemplate
) {
    companion object {
        const val THREAD_ID_KEY = "thread:id"
        const val THREAD_KEY_PREFIX = "thread:"
        const val BOARD_KEY_PREFIX = "board:"
        const val FIELD_BOARD = "board"
        const val FIELD_POSTS = "posts"
    }

    /**
     * Create new thread with Hash structure
     * Stores board name and initial post ID
     */
    fun create(boardName: String, opPostId: Long): Long {
        val threadId = stringRedisTemplate.opsForValue().increment(THREAD_ID_KEY) ?: 1L
        val threadKey = "$THREAD_KEY_PREFIX$threadId"

        // Store as Hash with board name and posts list
        val hashOps = stringRedisTemplate.opsForHash<String, String>()
        hashOps.put(threadKey, FIELD_BOARD, boardName)
        hashOps.put(threadKey, FIELD_POSTS, opPostId.toString())

        // Add to board sorted set for ordering
        val boardKey = "$BOARD_KEY_PREFIX$boardName"
        val timestamp = Instant.now().epochSecond.toDouble()
        stringRedisTemplate.opsForZSet().add(boardKey, threadId.toString(), timestamp)

        return threadId
    }

    /**
     * Add reply to thread
     * Appends post ID to posts field in Hash
     */
    fun addReply(threadId: Long, postId: Long, boardName: String) {
        val threadKey = "$THREAD_KEY_PREFIX$threadId"
        val hashOps = stringRedisTemplate.opsForHash<String, String>()

        // Get current posts and append new one
        val currentPosts = hashOps.get(threadKey, FIELD_POSTS) ?: ""
        val updatedPosts = if (currentPosts.isEmpty()) {
            postId.toString()
        } else {
            "$currentPosts,$postId"
        }
        hashOps.put(threadKey, FIELD_POSTS, updatedPosts)

        // Update board sorted set for bumping
        val boardKey = "$BOARD_KEY_PREFIX$boardName"
        val timestamp = Instant.now().epochSecond.toDouble()
        stringRedisTemplate.opsForZSet().add(boardKey, threadId.toString(), timestamp)
    }

    /**
     * Find all post IDs for a thread
     * Parses the comma-separated posts field
     */
    fun findPostIdsByThreadId(threadId: Long): List<Long> {
        val threadKey = "$THREAD_KEY_PREFIX$threadId"
        val hashOps = stringRedisTemplate.opsForHash<String, String>()

        val postsString = hashOps.get(threadKey, FIELD_POSTS) ?: return emptyList()

        return postsString.split(",")
            .mapNotNull { it.toLongOrNull() }
    }

    /**
     * Find board name for a thread
     * Direct lookup from thread Hash
     */
    fun findBoardNameByThreadId(threadId: Long): String? {
        val threadKey = "$THREAD_KEY_PREFIX$threadId"
        val hashOps = stringRedisTemplate.opsForHash<String, String>()

        return hashOps.get(threadKey, FIELD_BOARD)
    }

    /**
     * Find recent thread IDs for a board
     */
    fun findRecentThreadIds(boardName: String, page: Int, pageSize: Int): List<Long> {
        val boardKey = "$BOARD_KEY_PREFIX$boardName"
        val start = (page - 1) * pageSize.toLong()
        val end = start + pageSize - 1

        return stringRedisTemplate.opsForZSet()
            .reverseRange(boardKey, start, end)
            ?.map { it.toLong() }
            ?: emptyList()
    }

    /**
     * Delete thread by ID
     * Removes from both Hash and board sorted set
     */
    fun deleteByThreadId(threadId: Long, boardName: String) {
        val threadKey = "$THREAD_KEY_PREFIX$threadId"
        stringRedisTemplate.delete(threadKey)

        val boardKey = "$BOARD_KEY_PREFIX$boardName"
        stringRedisTemplate.opsForZSet().remove(boardKey, threadId.toString())
    }
}