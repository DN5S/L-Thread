package dev.dn5s.lthread

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class LThreadApplication

fun main(args: Array<String>) {
	runApplication<LThreadApplication>(*args)
}
