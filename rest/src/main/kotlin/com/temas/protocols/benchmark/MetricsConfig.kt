package com.temas.protocols.benchmark

import com.codahale.metrics.ConsoleReporter
import com.codahale.metrics.MetricRegistry
import com.codahale.metrics.servlets.MetricsServlet
import org.springframework.boot.web.servlet.ServletRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.concurrent.TimeUnit


@Configuration
class MetricsConfig {
    @Bean
    fun metricRegistry() = MetricRegistry()

    @Bean
    fun metricsServlet(metricRegistry: MetricRegistry) =
            ServletRegistrationBean(MetricsServlet(metricRegistry),"/metrics/*")

    @Bean
    fun metricsReporter(metricRegistry: MetricRegistry): ConsoleReporter =
            ConsoleReporter.forRegistry(metricRegistry)
                    .convertRatesTo(TimeUnit.SECONDS)
                    .convertDurationsTo(TimeUnit.MILLISECONDS)
                    .build()
}