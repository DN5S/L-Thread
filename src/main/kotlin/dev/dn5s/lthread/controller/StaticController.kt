package dev.dn5s.lthread.controller

import dev.dn5s.lthread.service.ImageService
import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.Resource
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/static")
class StaticController(
    private val imageService: ImageService
) {

    /**
     * Serve original image file
     */
    @GetMapping("/{filename}")
    fun getImage(@PathVariable filename: String): ResponseEntity<Resource> {
        // Sanitize filename to prevent path traversal
        if (!isValidFilename(filename)) {
            return ResponseEntity.badRequest().build()
        }

        if (!imageService.imageExists(filename)) {
            return ResponseEntity.notFound().build()
        }

        val imagePath = imageService.getImagePath(filename)
        val resource = FileSystemResource(imagePath)

        // Determine content type
        val contentType = determineContentType(filename)

        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(contentType))
            .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"${escapeFilenameForHeader(filename)}\"")
            .body(resource)
    }

    /**
     * Serve thumbnail image
     */
    @GetMapping("/thumbnails/{filename}")
    fun getThumbnail(@PathVariable filename: String): ResponseEntity<Resource> {
        // Sanitize filename to prevent path traversal
        if (!isValidFilename(filename)) {
            return ResponseEntity.badRequest().build()
        }

        if (!imageService.thumbnailExists(filename)) {
            return ResponseEntity.notFound().build()
        }

        val thumbnailPath = imageService.getThumbnailPath(filename)
        val resource = FileSystemResource(thumbnailPath)

        // Determine content type
        val contentType = determineContentType(filename)

        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(contentType))
            .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"${escapeFilenameForHeader(filename)}\"")
            .body(resource)
    }

    /**
     * Validate filename to prevent path traversal attacks
     */
    private fun isValidFilename(filename: String): Boolean {
        if (filename.length > 100) {
            return false
        }

        // Check for path traversal characters
        if (filename.contains("/") ||
            filename.contains("\\") ||
            filename.contains("..") ||
            filename.startsWith(".")) {
            return false
        }
        
        val regex = "^(thumb_)?\\d{1,20}_[a-zA-Z0-9]{1,20}\\.(jpg|jpeg|png|gif)$".toRegex()
        return regex.matches(filename)
    }

    /**
     * Escape filename for safe use in HTTP headers
     * Prevents header injection attacks
     */
    private fun escapeFilenameForHeader(filename: String): String {
        // Remove or encode potentially dangerous characters
        return filename
            .replace("\"", "\\\"")
            .replace("\r", "")
            .replace("\n", "")
            .replace("\\n", "")
            .replace("\\r", "")
    }

    /**
     * Determine content type based on file extension
     */
    private fun determineContentType(filename: String): String {
        return when {
            filename.endsWith(".jpg", ignoreCase = true) ||
            filename.endsWith(".jpeg", ignoreCase = true) -> "image/jpeg"
            filename.endsWith(".png", ignoreCase = true) -> "image/png"
            filename.endsWith(".gif", ignoreCase = true) -> "image/gif"
            else -> MediaType.APPLICATION_OCTET_STREAM_VALUE
        }
    }
}