package dev.dn5s.lthread.service

import org.springframework.stereotype.Service
import java.security.MessageDigest
import java.util.Base64

@Service
class TripcodeService {

    companion object {
        private const val SALT = "L@1n#W1r3d"
        private const val TRIPCODE_LENGTH = 10
    }

    /**
     * Generate tripcode from name#password format
     * @param input The full input string (e.g., "Lain#secretpass")
     * @return Pair of (displayName, tripcode) or null if no tripcode
     */
    fun generateTripcode(input: String?): Pair<String, String?>? {
        if (input.isNullOrBlank()) return null

        val parts = input.split("#", limit = 2)
        if (parts.size != 2) {
            // No password provided, just return the name without tripcode
            return Pair(input, null)
        }

        val name = parts[0].ifBlank { "Anonymous" }
        val password = parts[1]

        if (password.isBlank()) {
            return Pair(name, null)
        }

        val tripcode = hashPassword(password)
        return Pair(name, tripcode)
    }

    /**
     * Generate hash from password
     */
    private fun hashPassword(password: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val saltedPassword = "$password$SALT"
        val hashBytes = digest.digest(saltedPassword.toByteArray())

        // Use URL-safe Base64 encoder without padding
        val base64 = Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(hashBytes)

        return base64.take(TRIPCODE_LENGTH)
    }

    /**
     * Format the display name with tripcode
     * @param name The display name
     * @param tripcode The generated tripcode
     * @return Formatted string like "Name ◆tripcode"
     */
    fun formatDisplay(name: String, tripcode: String?): String {
        return if (tripcode != null) {
            "$name ◆$tripcode"
        } else {
            name
        }
    }
}