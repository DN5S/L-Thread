package dev.dn5s.lthread.controller

import dev.dn5s.lthread.model.Board
import dev.dn5s.lthread.service.BoardService
import dev.dn5s.lthread.service.ThreadListResult
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/board")
class BoardController(
    private val boardService: BoardService
) {

    /**
     * Get list of all boards
     */
    @GetMapping
    fun getBoards(): ResponseEntity<List<Board>> {
        return ResponseEntity.ok(boardService.getBoards())
    }

    /**
     * Get paginated thread list for a board
     */
    @GetMapping("/{boardName}")
    fun getThreadList(
        @PathVariable boardName: String,
        @RequestParam(defaultValue = "1") page: Int
    ): ResponseEntity<ThreadListResult> {
        val result = boardService.getThreadList(boardName, page)
        return ResponseEntity.ok(result)
    }

    /**
     * Create new thread on a board
     * Requires multipart form with text and image
     */
    @PostMapping("/{boardName}", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun createThread(
        @PathVariable boardName: String,
        @RequestParam("text") text: String,
        @RequestParam("image") image: MultipartFile,
        @RequestParam("name", required = false) name: String?
    ): ResponseEntity<ThreadCreationResponse> {
        // Sanitize boardName to prevent XSS
        val sanitizedBoardName = boardName.replace(Regex("[<>\"'&]"), "")
        val thread = boardService.createThread(sanitizedBoardName, text, image, name)

        return ResponseEntity.status(HttpStatus.CREATED).body(
            ThreadCreationResponse(
                threadId = thread.id,
                postId = thread.postIds.first(),
                boardName = sanitizedBoardName
            )
        )
    }
}

/**
 * Response for thread creation
 */
data class ThreadCreationResponse(
    val threadId: Long,
    val postId: Long,
    val boardName: String
)