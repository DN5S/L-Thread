package dev.dn5s.lthread.filter

import dev.dn5s.lthread.config.AppConfig
import dev.dn5s.lthread.service.RateLimitService
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
@Order(1)
class RateLimitFilter(
    @param:org.springframework.context.annotation.Lazy
    private val rateLimitService: RateLimitService,
    private val appConfig: AppConfig
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        // Skip rate limiting if disabled
        if (!appConfig.rateLimit.enabled) {
            filterChain.doFilter(request, response)
            return
        }

        val clientIp = getClientIp(request)
        val path = request.servletPath
        val method = request.method

        // Check rate limits for POST requests
        if (method == "POST") {
            val limitType = when {
                path.contains("/board/") && path.contains("/create") -> RateLimitService.LimitType.THREAD_CREATE
                path.contains("/thread/") && path.contains("/reply") -> RateLimitService.LimitType.POST_REPLY
                else -> RateLimitService.LimitType.GENERAL
            }

            if (!rateLimitService.allowRequest(clientIp, limitType)) {
                response.status = 429 // Too Many Requests
                response.writer.write("Rate limit exceeded. Please wait before posting again.")
                return
            }
        }

        // General rate limiting for all requests
        if (!rateLimitService.allowRequest(clientIp, RateLimitService.LimitType.GENERAL)) {
            response.status = 429 // Too Many Requests
            response.writer.write("Too many requests. Please slow down.")
            return
        }

        filterChain.doFilter(request, response)
    }

    private fun getClientIp(request: HttpServletRequest): String {
        // Check for proxied requests
        val xForwardedFor = request.getHeader("X-Forwarded-For")
        if (!xForwardedFor.isNullOrBlank()) {
            return xForwardedFor.split(",")[0].trim()
        }

        val xRealIp = request.getHeader("X-Real-IP")
        if (!xRealIp.isNullOrBlank()) {
            return xRealIp
        }

        return request.remoteAddr
    }
}