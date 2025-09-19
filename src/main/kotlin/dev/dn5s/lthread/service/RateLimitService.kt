package dev.dn5s.lthread.service

import io.github.bucket4j.Bandwidth
import io.github.bucket4j.BucketConfiguration
import io.github.bucket4j.Refill
import io.github.bucket4j.distributed.proxy.ProxyManager
import org.springframework.stereotype.Service
import java.time.Duration

@Service
@org.springframework.context.annotation.Lazy
class RateLimitService(
    private val bucketProxyManager: ProxyManager<String>
) {
    enum class LimitType {
        GENERAL,        // General requests: 60 per minute
        THREAD_CREATE,  // Thread creation: 1 per 30 seconds
        POST_REPLY      // Reply posting: 1 per 10 seconds
    }

    fun allowRequest(clientIp: String, limitType: LimitType): Boolean {
        val key = "rate_limit:$limitType:$clientIp"

        val bucket = bucketProxyManager.builder()
            .build(key) {
                createBucketConfiguration(limitType)
            }

        return bucket.tryConsume(1)
    }

    private fun createBucketConfiguration(limitType: LimitType): BucketConfiguration {
        val bandwidth = when (limitType) {
            LimitType.GENERAL -> Bandwidth.classic(
                60,
                Refill.intervally(60, Duration.ofMinutes(1))
            )
            LimitType.THREAD_CREATE -> Bandwidth.classic(
                1,
                Refill.intervally(1, Duration.ofSeconds(30))
            )
            LimitType.POST_REPLY -> Bandwidth.classic(
                1,
                Refill.intervally(1, Duration.ofSeconds(10))
            )
        }

        return BucketConfiguration.builder()
            .addLimit(bandwidth)
            .build()
    }
}