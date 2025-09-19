package dev.dn5s.lthread.controller.advice

import dev.dn5s.lthread.controller.ResourceNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.multipart.MaxUploadSizeExceededException

@ControllerAdvice(basePackageClasses = [dev.dn5s.lthread.controller.WebController::class])
class WebExceptionHandler {

    companion object {
        private val logger = LoggerFactory.getLogger(WebExceptionHandler::class.java)
    }

    /**
     * Handle resource didn't find exceptions
     */
    @ExceptionHandler(ResourceNotFoundException::class)
    fun handleResourceNotFound(
        e: ResourceNotFoundException,
        model: Model
    ): String {
        logger.debug("Resource not found: ${e.message}")
        model.addAttribute("pageTitle", "Signal Lost - L-Thread")
        model.addAttribute("errorMessage", "The data you seek has returned to the void.")
        return "error/404"
    }

    /**
     * Handle validation errors
     */
    @ExceptionHandler(IllegalArgumentException::class)
    fun handleValidationError(
        e: IllegalArgumentException,
        model: Model
    ): String {
        logger.warn("Validation error: ${e.message}")
        model.addAttribute("pageTitle", "Invalid Request - L-Thread")
        model.addAttribute("errorMessage", e.message ?: "Invalid transmission.")
        return "error/400"
    }

    /**
     * Handle file size exceeded
     */
    @ExceptionHandler(MaxUploadSizeExceededException::class)
    fun handleMaxUploadSize(
        e: MaxUploadSizeExceededException,
        model: Model
    ): String {
        logger.warn("File size exceeded: ${e.message}")
        model.addAttribute("pageTitle", "Data Overflow - L-Thread")
        model.addAttribute("errorMessage", "Data overflow. Maximum file size is 20MB.")
        return "error/413"
    }

    /**
     * Handle all other exceptions
     */
    @ExceptionHandler(Exception::class)
    fun handleGenericException(
        e: Exception,
        model: Model
    ): String {
        logger.error("Unexpected error", e)
        model.addAttribute("pageTitle", "System Malfunction - L-Thread")
        model.addAttribute("errorMessage", "System malfunction. Please try again.")
        return "error/500"
    }
}