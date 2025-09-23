package dev.dn5s.lthread.model

import java.time.Instant

data class Post(
    val id: Long,
    val text: String,
    val imagePath: String? = null,
    val thumbnailPath: String? = null,
    val timestamp: Instant = Instant.now(),
    val author: String = "Anonymous"
) {
    companion object {
        const val MAX_TEXT_LENGTH = 20000 // Increased for code sharing
        const val MAX_FILE_SIZE = 20 * 1024 * 1024 // 20MB
        const val MAX_IMAGE_DIMENSION = 10000
    }
}