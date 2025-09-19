package dev.dn5s.lthread.controller

import dev.dn5s.lthread.service.BoardService
import dev.dn5s.lthread.service.ThreadDetail
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/thread")
class ThreadController(
    private val boardService: BoardService
) {

    /**
     * Get thread detail with all posts
     */
    @GetMapping("/{threadId}")
    fun getThreadDetail(@PathVariable threadId: Long): ResponseEntity<ThreadDetail> {
        val threadDetail = boardService.getThreadDetail(threadId)
            ?: throw IllegalArgumentException("Thread not found")

        return ResponseEntity.ok(threadDetail)
    }

    /**
     * Post reply to thread
     * Multipart form with text and optional image
     */
    @PostMapping("/{threadId}", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun postReply(
        @PathVariable threadId: Long,
        @RequestParam("text") text: String,
        @RequestParam("image", required = false) image: MultipartFile?,
        @RequestParam("name", required = false) name: String?
    ): ResponseEntity<ReplyCreationResponse> {
        val post = boardService.postReply(threadId, text, image, name)

        return ResponseEntity.status(HttpStatus.CREATED).body(
            ReplyCreationResponse(
                postId = post.id,
                threadId = threadId,
                timestamp = post.timestamp.toString()
            )
        )
    }
}

/**
 * Response for reply creation
 */
data class ReplyCreationResponse(
    val postId: Long,
    val threadId: Long,
    val timestamp: String
)