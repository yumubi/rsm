package io.hika.rsm

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class RsmApplication

fun main(args: Array<String>) {
    runApplication<RsmApplication>(*args)
}
