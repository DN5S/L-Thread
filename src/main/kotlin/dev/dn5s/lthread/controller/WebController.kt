package dev.dn5s.lthread.controller

import dev.dn5s.lthread.service.BoardService
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@Controller
class WebController(
    private val boardService: BoardService
) {

    /**
     * Home page - list all boards
     */
    @GetMapping("/")
    fun index(model: Model): String {
        model.addAttribute("boards", boardService.getBoards())
        model.addAttribute("pageTitle", "L-Thread")
        model.addAttribute("metaDescription", "Ephemeral anonymous imageboard where memories fade. No accounts, no persistence.")
        return "index"
    }

    /**
     * Board page - show a thread list
     */
    @GetMapping("/board/{boardName}")
    fun boardView(
        @PathVariable boardName: String,
        @RequestParam(defaultValue = "1") page: Int,
        model: Model
    ): String {
        if (!boardName.matches(Regex("^[a-z]{1,20}$"))) {
            throw ResourceNotFoundException("Invalid board name")
        }

        if (!boardService.boardExists(boardName)) {
            throw ResourceNotFoundException("Board /$boardName/ does not exist")
        }
        
        if (page !in 1..100) {
            throw IllegalArgumentException("Invalid page number")
        }

        val threadList = boardService.getThreadList(boardName, page)
        model.addAttribute("board", boardName)
        model.addAttribute("threads", threadList.threads)
        model.addAttribute("currentPage", threadList.currentPage)
        model.addAttribute("totalPages", threadList.totalPages)
        model.addAttribute("pageTitle", "/$boardName/ - L-Thread")
        model.addAttribute("metaDescription", "$boardName discussion board on L-Thread")

        return "board"
    }

    /**
     * Thread detail page
     */
    @GetMapping("/thread/{threadId}")
    fun threadView(
        @PathVariable threadId: Long,
        model: Model
    ): String {
        val thread = boardService.getThreadDetail(threadId)
            ?: throw ResourceNotFoundException("Thread #$threadId not found")

        model.addAttribute("thread", thread)
        model.addAttribute("pageTitle", "Thread #$threadId - /${thread.boardName}/ - L-Thread")
        model.addAttribute("metaDescription", "Anonymous discussion thread #$threadId on L-Thread imageboard")

        return "thread"
    }

    /**
     * Create a new thread (POST form submission)
     */
    @PostMapping("/board/{boardName}/create")
    fun createThread(
        @PathVariable boardName: String,
        @RequestParam("text") text: String,
        @RequestParam("image") image: MultipartFile,
        @RequestParam("name", required = false) name: String?,
        @RequestParam("website", required = false) honeypot: String?
    ): String {
        if (!honeypot.isNullOrBlank()) {
            Thread.sleep(3000) // Slow down bots
            return "redirect:/board/$boardName"
        }
        return try {
            val thread = boardService.createThread(boardName, text, image, name)
            "redirect:/thread/${thread.id}"
        } catch (e: IllegalArgumentException) {
            throw e // Let the exception handler deal with it
        } catch (e: Exception) {
            throw RuntimeException("Failed to create thread: ${e.message}", e)
        }
    }

    /**
     * Post reply to thread (POST form submission)
     */
    @PostMapping("/thread/{threadId}/reply")
    fun postReply(
        @PathVariable threadId: Long,
        @RequestParam("text") text: String,
        @RequestParam("image", required = false) image: MultipartFile?,
        @RequestParam("name", required = false) name: String?,
        @RequestParam("website", required = false) honeypot: String?
    ): String {
        if (!honeypot.isNullOrBlank()) {
            Thread.sleep(3000) // Slow down bots
            return "redirect:/thread/$threadId"
        }
        return try {
            boardService.postReply(threadId, text, image, name)
            "redirect:/thread/$threadId#bottom"
        } catch (e: IllegalArgumentException) {
            throw e // Let the exception handler deal with it
        } catch (e: Exception) {
            throw RuntimeException("Failed to post reply: ${e.message}", e)
        }
    }

    /**
     * Help page - usage instructions and rules
     */
    @GetMapping("/help")
    fun helpPage(model: Model): String {
        model.addAttribute("pageTitle", "Help")
        model.addAttribute("metaDescription", "How to use L-Thread")
        return "help"
    }

    /**
     * Robots.txt - Allow only main page indexing
     * Ephemeral content (boards/threads) remains unindexed
     */
    @GetMapping("/robots.txt", produces = ["text/plain"])
    @ResponseBody
    fun robotsTxt(): String {
        return """
            # L-Thread respects digital oblivion
            # Data here is ephemeral
            User-agent: *
            Allow: /$
            Disallow: /board/
            Disallow: /thread/
            Disallow: /static/
        """.trimIndent()
    }
}

/**
 * Custom exception for resource not found
 */
class ResourceNotFoundException(message: String) : RuntimeException(message)