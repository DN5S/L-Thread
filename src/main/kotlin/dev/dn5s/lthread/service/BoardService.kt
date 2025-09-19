package dev.dn5s.lthread.service

import dev.dn5s.lthread.config.AppConfig
import dev.dn5s.lthread.model.Board
import dev.dn5s.lthread.model.Post
import dev.dn5s.lthread.model.Thread
import dev.dn5s.lthread.repository.BoardRepository
import dev.dn5s.lthread.repository.PostRepository
import dev.dn5s.lthread.repository.ThreadRepository
import dev.dn5s.lthread.util.TextSanitizer
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.time.Instant

@Service
class BoardService(
    private val boardRepository: BoardRepository,
    private val threadRepository: ThreadRepository,
    private val postRepository: PostRepository,
    private val imageService: ImageService,
    private val tripcodeService: TripcodeService,
    private val textSanitizer: TextSanitizer,
    private val appConfig: AppConfig
) {

    /**
     * Get a list of available boards
     */
    fun getBoards(): List<Board> {
        return Board.DEFAULT_BOARDS
    }

    /**
     * Check if the board exists
     */
    fun boardExists(boardName: String): Boolean {
        return appConfig.boards.contains(boardName)
    }

    /**
     * Generate a formatted display name with optional tripcode
     */
    private fun generateFormattedName(nameInput: String?): String {
        // Sanitize name input first
        val sanitizedName = textSanitizer.sanitizeName(nameInput)
        val (displayName, tripcode) = tripcodeService.generateTripcode(sanitizedName)
            ?: Pair("Anonymous", null)
        return tripcodeService.formatDisplay(displayName, tripcode)
    }

    /**
     * Create a new thread (image required)
     */
    @Transactional
    fun createThread(
        boardName: String,
        text: String,
        imageFile: MultipartFile,
        nameInput: String? = null
    ): Thread {
        // Validate board
        if (!boardExists(boardName)) {
            throw IllegalArgumentException("Board /$boardName/ does not exist")
        }

        // Validate text length
        if (text.length > Post.MAX_TEXT_LENGTH) {
            throw IllegalArgumentException("Text exceeds maximum length of ${Post.MAX_TEXT_LENGTH}")
        }

        // Sanitize text input
        val sanitizedText = textSanitizer.sanitizeText(text)

        // Validate image is provided and not empty (required for threads)
        if (imageFile.isEmpty) {
            throw IllegalArgumentException("Image is required for creating threads")
        }

        // Process image (required for threads)
        val (imagePath, thumbnailPath) = imageService.processImage(imageFile)

        // Create post
        val postId = postRepository.generateId()
        val post = Post(
            id = postId,
            text = sanitizedText,
            imagePath = imagePath,
            thumbnailPath = thumbnailPath,
            timestamp = Instant.now(),
            tripcode = generateFormattedName(nameInput)
        )
        postRepository.save(post)

        // Create thread
        val threadId = threadRepository.create(boardName, postId)

        return Thread(
            id = threadId,
            boardName = boardName,
            postIds = mutableListOf(postId),
            lastPostTime = post.timestamp,
            createdAt = post.timestamp
        )
    }

    /**
     * Post reply to thread
     */
    @Transactional
    fun postReply(
        threadId: Long,
        text: String,
        imageFile: MultipartFile? = null,
        nameInput: String? = null
    ): Post {
        // Get board name from thread
        val boardName = threadRepository.findBoardNameByThreadId(threadId)
            ?: throw IllegalArgumentException("Thread not found")

        // Validate text length
        if (text.length > Post.MAX_TEXT_LENGTH) {
            throw IllegalArgumentException("Text exceeds maximum length of ${Post.MAX_TEXT_LENGTH}")
        }

        // Sanitize text input
        val sanitizedText = textSanitizer.sanitizeText(text)

        // Process image if provided and not empty
        val (imagePath, thumbnailPath) = if (imageFile != null && !imageFile.isEmpty) {
            imageService.processImage(imageFile)
        } else {
            Pair(null, null)
        }

        // Create a reply post
        val postId = postRepository.generateId()
        val post = Post(
            id = postId,
            text = sanitizedText,
            imagePath = imagePath,
            thumbnailPath = thumbnailPath,
            timestamp = Instant.now(),
            tripcode = generateFormattedName(nameInput)
        )
        postRepository.save(post)

        // Add reply to thread and bump it
        threadRepository.addReply(threadId, postId, boardName)

        return post
    }

    /**
     * Get a paginated thread list for a board
     */
    fun getThreadList(boardName: String, page: Int = 1): ThreadListResult {
        if (!boardExists(boardName)) {
            throw IllegalArgumentException("Board /$boardName/ does not exist")
        }

        val pageSize = appConfig.thread.pageSize
        val threadIds = threadRepository.findRecentThreadIds(boardName, page, pageSize)

        val threads = threadIds.map { threadId ->
            val postIds = threadRepository.findPostIdsByThreadId(threadId)
            val posts = postRepository.findMultipleByIds(postIds)

            // Get OP and preview replies
            val op = posts.firstOrNull()
            val previewCount = appConfig.thread.previewReplies
            val replies = if (posts.size > 1) {
                posts.drop(1).takeLast(previewCount)
            } else {
                emptyList()
            }

            ThreadPreview(
                id = threadId,
                op = op,
                previewReplies = replies,
                replyCount = posts.size - 1,
                lastPostTime = posts.lastOrNull()?.timestamp ?: Instant.now()
            )
        }

        val totalThreads = boardRepository.getThreadCount(boardName)
        val totalPages = (totalThreads + pageSize - 1) / pageSize

        return ThreadListResult(
            boardName = boardName,
            threads = threads,
            currentPage = page,
            totalPages = totalPages.toInt(),
            totalThreads = totalThreads
        )
    }

    /**
     * Get thread detail with all posts
     */
    fun getThreadDetail(threadId: Long): ThreadDetail? {
        val boardName = threadRepository.findBoardNameByThreadId(threadId)
            ?: return null

        val postIds = threadRepository.findPostIdsByThreadId(threadId)
        if (postIds.isEmpty()) {
            return null
        }

        val posts = postRepository.findMultipleByIds(postIds)

        return ThreadDetail(
            id = threadId,
            boardName = boardName,
            posts = posts,
            replyCount = posts.size - 1,
            createdAt = posts.firstOrNull()?.timestamp ?: Instant.now(),
            lastPostTime = posts.lastOrNull()?.timestamp ?: Instant.now()
        )
    }

    /**
     * Delete thread and all associated data
     * Used by a pruning system
     */
    @Transactional
    fun deleteThread(threadId: Long) {
        val boardName = threadRepository.findBoardNameByThreadId(threadId)
            ?: return // Thread doesn't exist

        val postIds = threadRepository.findPostIdsByThreadId(threadId)

        // Delete all post-images
        postIds.forEach { postId ->
            val post = postRepository.findById(postId)
            post?.imagePath?.let { imageService.deleteImages(it) }
        }

        // Delete all posts
        postIds.forEach { postId ->
            postRepository.deleteById(postId)
        }

        // Delete thread
        threadRepository.deleteByThreadId(threadId, boardName)
    }

    /**
     * Get oldest threads for pruning
     */
    fun getOldestThreads(boardName: String, count: Int): List<Long> {
        return boardRepository.getOldestThreadIds(boardName, count)
    }
}

// DTOs
data class ThreadListResult(
    val boardName: String,
    val threads: List<ThreadPreview>,
    val currentPage: Int,
    val totalPages: Int,
    val totalThreads: Long
)

data class ThreadPreview(
    val id: Long,
    val op: Post?,
    val previewReplies: List<Post>,
    val replyCount: Int,
    val lastPostTime: Instant
)

data class ThreadDetail(
    val id: Long,
    val boardName: String,
    val posts: List<Post>,
    val replyCount: Int,
    val createdAt: Instant,
    val lastPostTime: Instant
)