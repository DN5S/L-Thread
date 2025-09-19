package dev.dn5s.lthread.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.csrf.CookieCsrfTokenRepository
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter
import org.springframework.security.web.header.writers.XXssProtectionHeaderWriter
import org.springframework.security.web.header.writers.StaticHeadersWriter

@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val appConfig: AppConfig
) {

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http
            // Stateless - no sessions
            .sessionManagement { session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            }

            // CSRF Protection with Double Submit Cookie (stateless)
            .csrf { csrf ->
                if (appConfig.security.csrf.enabled) {
                    csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .ignoringRequestMatchers("/api/**", "/robots.txt", "/static/**")
                } else {
                    csrf.disable()
                }
            }

            // Security headers
            .headers { headers ->
                headers
                    .frameOptions { it.deny() }
                    .xssProtection { xss ->
                        xss.headerValue(XXssProtectionHeaderWriter.HeaderValue.ENABLED_MODE_BLOCK)
                    }
                    .contentTypeOptions { }
                    .referrerPolicy { referrer ->
                        referrer.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER)
                    }
                    .contentSecurityPolicy { csp ->
                        csp.policyDirectives(
                            "default-src 'self'; " +
                            "script-src 'self'; " +
                            "style-src 'self' 'unsafe-inline'; " +
                            "img-src 'self' data:; " +
                            "font-src 'self'; " +
                            "connect-src 'self'; " +
                            "frame-ancestors 'none'; " +
                            "base-uri 'self'; " +
                            "form-action 'self'"
                        )
                    }
                    .addHeaderWriter(
                        StaticHeadersWriter(
                            "Permissions-Policy",
                            "geolocation=(), microphone=(), camera=(), payment=(), usb=(), magnetometer=(), accelerometer=()"
                        )
                    )
            }

            // Permit all requests (no authentication required)
            .authorizeHttpRequests { authorize ->
                authorize.anyRequest().permitAll()
            }

            // Disable features we don't need
            .httpBasic { it.disable() }
            .formLogin { it.disable() }
            .logout { it.disable() }
            .rememberMe { it.disable() }

        return http.build()
    }
}