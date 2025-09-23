package dev.dn5s.lthread.repository

import dev.dn5s.lthread.model.Post
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
class PostRepository(
    private val stringRedisTemplate: StringRedisTemplate,
    private val hashRedisTemplate: RedisTemplate<String, Any>
) {
    companion object {
        const val POST_ID_KEY = "post:id"
        const val POST_KEY_PREFIX = "post:"
    }

    fun generateId(): Long {
        return stringRedisTemplate.opsForValue().increment(POST_ID_KEY) ?: 1L
    }

    fun save(post: Post): Post {
        val key = "$POST_KEY_PREFIX${post.id}"

        val dataMap = mutableMapOf<String, String>()
        dataMap["id"] = post.id.toString()
        dataMap["text"] = post.text
        dataMap["timestamp"] = post.timestamp.toString()
        post.imagePath?.let { dataMap["imagePath"] = it }
        post.thumbnailPath?.let { dataMap["thumbnailPath"] = it }
        dataMap["author"] = post.author

        hashRedisTemplate.opsForHash<String, String>().putAll(key, dataMap)

        return post
    }

    fun findById(id: Long): Post? {
        val key = "$POST_KEY_PREFIX$id"
        val hashOps = hashRedisTemplate.opsForHash<String, String>()
        val data = hashOps.entries(key)

        if (data.isEmpty()) return null

        return Post(
            id = data["id"]?.toLong() ?: throw IllegalStateException("Post data is corrupted: ID mismatch for key $key"),
            text = data["text"] ?: "",
            imagePath = data["imagePath"],
            thumbnailPath = data["thumbnailPath"],
            timestamp = data["timestamp"]?.let { Instant.parse(it) } ?: Instant.now(),
            author = data["author"] ?: "Anonymous"
        )
    }

    fun deleteById(id: Long) {
        val key = "$POST_KEY_PREFIX$id"
        hashRedisTemplate.delete(key)
    }

    fun findMultipleByIds(ids: List<Long>): List<Post> {
        return ids.mapNotNull { findById(it) }
    }
}