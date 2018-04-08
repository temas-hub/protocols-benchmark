package com.temas.protocols.benchmark.server

import com.codahale.metrics.ConsoleReporter
import com.temas.protocols.benchmark.MetricsConfig
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Import
import java.util.concurrent.TimeUnit
import javax.annotation.PostConstruct

@SpringBootApplication
@Import(value = [MetricsConfig::class])
class ServerApplication {

    @Autowired
    lateinit var consoleReporter: ConsoleReporter

    @PostConstruct
    fun init() {
        consoleReporter.start(20, TimeUnit.SECONDS)
    }
}

fun main(args: Array<String>) {
    SpringApplication.run(ServerApplication::class.java, *args)
}