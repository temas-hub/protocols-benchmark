package com.temas.protocols.benchmark.server

import com.codahale.metrics.MetricRegistry
import com.codahale.metrics.SlidingTimeWindowArrayReservoir
import com.codahale.metrics.Timer
import com.temas.protocols.benchmark.model.Generator.generateUsers
import com.temas.protocols.benchmark.model.GetUserResponse
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.concurrent.TimeUnit

@RestController
class GetUserController {
    companion object {
        val METRICS_SLIDING_WINDOW_SEC = 60L
    }

    @Autowired
    lateinit var metricsRegistry: MetricRegistry

    val responseRequestHandlingTimer: Timer by lazy {
                metricsRegistry.timer(MetricRegistry.name(javaClass, "request-handling")) }

    val responseRequestHandlingSlidingTimer: Timer by lazy {
        metricsRegistry.register(MetricRegistry.name(javaClass, "request-handling", "sliding"),
            Timer(SlidingTimeWindowArrayReservoir(METRICS_SLIDING_WINDOW_SEC, TimeUnit.SECONDS))) }


    @GetMapping("/users")
    fun getUsers(@RequestParam(value = "count") count: Int): GetUserResponse {
        responseRequestHandlingTimer.time().use {
            responseRequestHandlingSlidingTimer.time().use {
                return GetUserResponse(generateUsers(count))
            }
        }
    }
}