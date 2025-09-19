package dev.dn5s.lthread

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class LThreadApplication

fun main(args: Array<String>) {
	runApplication<LThreadApplication>(*args)
}
