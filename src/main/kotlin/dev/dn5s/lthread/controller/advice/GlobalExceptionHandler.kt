package dev.dn5s.lthread.controller.advice

import dev.dn5s.lthread.controller.StaticController
import dev.dn5s.lthread.controller.BoardController
import dev.dn5s.lthread.controller.ThreadController
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.multipart.MaxUploadSizeExceededException

@RestControllerAdvice(assignableTypes = [StaticController::class, BoardController::class, ThreadController::class])
class GlobalExceptionHandler {

    /**
     * Handle IllegalArgumentException for validation errors
     */
    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgumentException(e: IllegalArgumentException): ResponseEntity<ErrorResponse> {
        val status = when {
            e.message?.contains("not found", ignoreCase = true) == true -> HttpStatus.NOT_FOUND
            e.message?.contains("does not exist", ignoreCase = true) == true -> HttpStatus.NOT_FOUND
            else -> HttpStatus.BAD_REQUEST
        }

        val message = when (status) {
            HttpStatus.NOT_FOUND -> "Signal Lost."
            else -> e.message ?: "Invalid transmission."
        }

        return ResponseEntity.status(status).body(
            ErrorResponse(
                error = message,
                status = status.value()
            )
        )
    }

    /**
     * Handle file size exceeded exception
     */
    @ExceptionHandler(MaxUploadSizeExceededException::class)
    fun handleMaxUploadSizeExceededException(e: MaxUploadSizeExceededException): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(
            ErrorResponse(
                error = "Data overflow.",
                status = HttpStatus.PAYLOAD_TOO_LARGE.value()
            )
        )
    }

    /**
     * Handle generic exceptions
     */
    @ExceptionHandler(Exception::class)
    fun handleGenericException(e: Exception): ResponseEntity<ErrorResponse> {
        // Log the actual error for debugging
        e.printStackTrace()

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
            ErrorResponse(
                error = "System malfunction.",
                status = HttpStatus.INTERNAL_SERVER_ERROR.value()
            )
        )
    }
}

/**
 * Unified error response structure
 */
data class ErrorResponse(
    val error: String,
    val status: Int,
    val timestamp: Long = System.currentTimeMillis()
)