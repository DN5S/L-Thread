package dev.dn5s.lthread.repository

import dev.dn5s.lthread.config.AppConfig
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Repository

@Repository
class BoardRepository(
    private val stringRedisTemplate: StringRedisTemplate,
    private val appConfig: AppConfig
) {
    companion object {
        const val BOARD_KEY_PREFIX = "board:"
    }

    fun getThreadCount(boardName: String): Long {
        val boardKey = "$BOARD_KEY_PREFIX$boardName"
        return stringRedisTemplate.opsForZSet().size(boardKey) ?: 0L
    }

    fun getOldestThreadIds(boardName: String, count: Int): List<Long> {
        val boardKey = "$BOARD_KEY_PREFIX$boardName"
        return stringRedisTemplate.opsForZSet()
            .range(boardKey, 0, count - 1L)
            ?.map { it.toLong() }
            ?: emptyList()
    }

    fun removeThreads(boardName: String, threadIds: List<Long>) {
        if (threadIds.isEmpty()) return

        val boardKey = "$BOARD_KEY_PREFIX$boardName"
        val threadIdStrings = threadIds.map { it.toString() }.toTypedArray()
        stringRedisTemplate.opsForZSet().remove(boardKey, *threadIdStrings)
    }

    fun getAllBoards(): List<String> {
        return appConfig.boards
    }
}