package dev.dn5s.lthread.controller

import jakarta.servlet.RequestDispatcher
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.boot.web.servlet.error.ErrorController
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody
import dev.dn5s.lthread.controller.advice.ErrorResponse

@Controller
class CustomErrorController : ErrorController {

    companion object {
        private val logger = LoggerFactory.getLogger(CustomErrorController::class.java)
        private const val ERROR_PATH = "/error"
    }

    @RequestMapping(ERROR_PATH, produces = [MediaType.TEXT_HTML_VALUE])
    fun handleErrorHtml(
        request: HttpServletRequest,
        model: Model
    ): String {
        val status = getStatus(request)
        val error = getError(request)

        logger.debug("HTML Error handling - Status: {}, Error: {}", status, error)

        return when (status) {
            HttpStatus.NOT_FOUND -> {
                model.addAttribute("pageTitle", "Signal Lost - L-Thread")
                model.addAttribute("errorMessage", "The data you seek has returned to the void.")
                "error/404"
            }
            HttpStatus.BAD_REQUEST -> {
                model.addAttribute("pageTitle", "Invalid Request - L-Thread")
                model.addAttribute("errorMessage", error ?: "Invalid transmission.")
                "error/400"
            }
            HttpStatus.PAYLOAD_TOO_LARGE -> {
                model.addAttribute("pageTitle", "Data Overflow - L-Thread")
                model.addAttribute("errorMessage", "Data overflow. Maximum file size is 20MB.")
                "error/413"
            }
            HttpStatus.TOO_MANY_REQUESTS -> {
                model.addAttribute("pageTitle", "Rate Limited - L-Thread")
                model.addAttribute("errorMessage", "Connection throttled. Please wait.")
                "error/429"
            }
            else -> {
                model.addAttribute("pageTitle", "System Malfunction - L-Thread")
                model.addAttribute("errorMessage", "System malfunction. Please try again.")
                "error/500"
            }
        }
    }

    @RequestMapping(ERROR_PATH, produces = [MediaType.APPLICATION_JSON_VALUE])
    @ResponseBody
    fun handleErrorJson(request: HttpServletRequest): ResponseEntity<ErrorResponse> {
        val status = getStatus(request)
        val error = getError(request)

        logger.debug("JSON Error handling - Status: {}, Error: {}", status, error)

        val message = when (status) {
            HttpStatus.NOT_FOUND -> "Signal Lost."
            HttpStatus.BAD_REQUEST -> error ?: "Invalid transmission."
            HttpStatus.PAYLOAD_TOO_LARGE -> "Data overflow."
            HttpStatus.TOO_MANY_REQUESTS -> "Connection throttled."
            else -> "System malfunction."
        }

        return ResponseEntity.status(status).body(
            ErrorResponse(
                error = message,
                status = status.value()
            )
        )
    }

    private fun getStatus(request: HttpServletRequest): HttpStatus {
        val statusCode = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE) as? Int
            ?: return HttpStatus.INTERNAL_SERVER_ERROR

        return try {
            HttpStatus.valueOf(statusCode)
        } catch (ex: Exception) {
            HttpStatus.INTERNAL_SERVER_ERROR
        }
    }

    private fun getError(request: HttpServletRequest): String? {
        val message = request.getAttribute(RequestDispatcher.ERROR_MESSAGE) as? String
        val exception = request.getAttribute(RequestDispatcher.ERROR_EXCEPTION) as? Throwable

        return when {
            !message.isNullOrBlank() -> message
            exception != null -> exception.message
            else -> null
        }
    }
}