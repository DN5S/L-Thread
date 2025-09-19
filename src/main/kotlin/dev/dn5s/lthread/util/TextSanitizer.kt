package dev.dn5s.lthread.util

import org.owasp.html.HtmlPolicyBuilder
import org.owasp.html.PolicyFactory
import org.springframework.stereotype.Component

@Component
class TextSanitizer {

    // No HTML tags allowed - plaintext only policy
    private val plaintextPolicy: PolicyFactory = HtmlPolicyBuilder()
        .toFactory()

    /**
     * Sanitize text input to prevent XSS attacks
     * Removes ALL HTML tags and dangerous content
     */
    fun sanitizeText(input: String?): String {
        if (input.isNullOrBlank()) return ""

        // Apply OWASP sanitization to remove all HTML
        val sanitized = plaintextPolicy.sanitize(input)

        // Additional safety: remove any remaining angle brackets
        return sanitized
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .trim()
    }

    /**
     * Sanitize name input for tripcode
     * More restrictive than general text
     */
    fun sanitizeName(input: String?): String? {
        if (input.isNullOrBlank()) return null

        // Remove any HTML tags first
        var sanitized = plaintextPolicy.sanitize(input)

        // Only allow alphanumeric, spaces, and # for tripcode
        sanitized = sanitized.replace(Regex("[^a-zA-Z0-9 #]"), "")

        // Limit length to prevent abuse
        if (sanitized.length > 50) {
            sanitized = sanitized.take(50)
        }

        return sanitized.ifBlank { null }
    }
}