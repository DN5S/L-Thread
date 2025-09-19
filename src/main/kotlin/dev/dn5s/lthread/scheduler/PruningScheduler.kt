package dev.dn5s.lthread.scheduler

import dev.dn5s.lthread.config.AppConfig
import dev.dn5s.lthread.service.BoardService
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class PruningScheduler(
    private val boardService: BoardService,
    private val appConfig: AppConfig,
    private val stringRedisTemplate: StringRedisTemplate
) {
    companion object {
        private val logger = LoggerFactory.getLogger(PruningScheduler::class.java)
    }

    /**
     * Check memory usage and prune old threads if necessary
     * Runs every 5 minutes (configurable via application.yml)
     */
    @Scheduled(fixedDelayString = "\${lthread.pruning.check-interval}")
    fun checkAndPrune() {
        try {
            val memoryUsage = getRedisMemoryUsage()
            val threshold = appConfig.pruning.memoryThreshold

            logger.info("Redis memory check - Usage: ${String.format("%.2f", memoryUsage * 100)}%, Threshold: ${String.format("%.2f", threshold * 100)}%")

            if (memoryUsage > threshold) {
                logger.warn("Memory threshold exceeded. Starting pruning process...")
                pruneOldThreads()
            }
        } catch (e: Exception) {
            logger.error("Error during pruning check: ${e.message}", e)
        }
    }

    /**
     * Get Redis memory usage as percentage of max memory
     * Uses thread count as proxy for memory usage
     */
    private fun getRedisMemoryUsage(): Double {
        return estimateByThreadCount()
    }

    /**
     * Estimate memory usage by counting threads
     */
    private fun estimateByThreadCount(): Double {
        var totalThreads = 0L
        appConfig.boards.forEach { boardName ->
            totalThreads += stringRedisTemplate.opsForZSet().size("board:$boardName") ?: 0
        }

        // Assume each thread uses approximately 100KB on average
        val estimatedMemoryUsage = totalThreads * 100 * 1024L
        val maxMemory = 2147483648L // 2GB

        return estimatedMemoryUsage.toDouble() / maxMemory.toDouble()
    }

    /**
     * Prune oldest threads from each board
     */
    private fun pruneOldThreads() {
        val pruneCount = appConfig.pruning.pruneCount
        var totalPruned = 0

        // Prune from each board
        appConfig.boards.forEach { boardName ->
            try {
                val oldestThreads = boardService.getOldestThreads(boardName, pruneCount)

                oldestThreads.forEach { threadId ->
                    try {
                        boardService.deleteThread(threadId)
                        logger.info("Pruned thread $threadId from board /$boardName/")
                        totalPruned++
                    } catch (e: Exception) {
                        logger.error("Failed to prune thread $threadId: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                logger.error("Failed to get threads from board /$boardName/: ${e.message}")
            }
        }

        if (totalPruned > 0) {
            logger.info("Pruning complete. Total threads pruned: $totalPruned")

            // Log new memory usage
            val newUsage = getRedisMemoryUsage()
            logger.info("Memory usage after pruning: ${String.format("%.2f", newUsage * 100)}%")
        } else {
            logger.info("No threads were pruned")
        }
    }

    /**
     * Manual trigger for pruning (can be called from admin endpoints)
     */
    fun forcePrune() {
        logger.info("Manual pruning triggered")
        pruneOldThreads()
    }
}