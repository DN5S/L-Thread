package dev.dn5s.lthread.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "lthread")
class AppConfig {
    lateinit var boards: List<String>

    var thread = ThreadConfig()
    var storage = StorageConfig()
    var pruning = PruningConfig()
    var rateLimit = RateLimitConfig()
    var security = SecurityConfig()

    class ThreadConfig {
        var pageSize: Int = 15
        var previewReplies: Int = 3
    }

    class StorageConfig {
        lateinit var imagesPath: String
        var thumbnailsSubdir: String = "thumbnails"
    }

    class PruningConfig {
        var memoryThreshold: Double = 0.8
        var checkInterval: Long = 300000 // 5 minutes in ms
        var pruneCount: Int = 10
    }

    class RateLimitConfig {
        var enabled: Boolean = true
    }

    class SecurityConfig {
        var csrf = CsrfConfig()

        class CsrfConfig {
            var enabled: Boolean = true
        }
    }
}