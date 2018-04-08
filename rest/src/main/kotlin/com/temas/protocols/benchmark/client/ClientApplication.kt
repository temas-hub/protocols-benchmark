package com.temas.protocols.benchmark.client

import com.codahale.metrics.ConsoleReporter
import com.codahale.metrics.MetricRegistry
import com.codahale.metrics.SlidingTimeWindowArrayReservoir
import com.codahale.metrics.Timer
import com.github.javafaker.Faker
import com.temas.protocols.benchmark.MetricsConfig
import com.temas.protocols.benchmark.model.GetUserResponse
import org.apache.http.client.HttpClient
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.WebApplicationType
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.context.annotation.Import
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import org.springframework.web.client.RestTemplate
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import javax.annotation.PostConstruct
import org.springframework.context.annotation.Bean
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager
import org.apache.http.client.config.RequestConfig
import org.apache.http.impl.client.HttpClientBuilder
import org.apache.http.impl.client.CloseableHttpClient
import org.omg.CORBA.Object
import java.lang.Thread.sleep
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue


@SpringBootConfiguration
@Import(value = [MetricsConfig::class])
class ClientApplication {
    // constants
    companion object {
        val MAX_USER_CNT = 100
        val MAX_CONNECTIONS = 16
        val METRICS_SLIDING_WINDOW_SEC = 60L
        val tps = Integer.parseInt(System.getProperty("tps", "30"))
        val HOST = System.getProperty("restserver", "ec2-18-222-12-202.us-east-2.compute.amazonaws.com")
        //val HOST = System.getProperty("restserver", "localhost")
        val PORT = Integer.parseInt(System.getProperty("restport", "8080"))
    }

    @Autowired
    lateinit var metricsRegistry: MetricRegistry
    @Autowired
    lateinit var metricsReporter: ConsoleReporter

    @Bean
    fun poolingHttpClientConnectionManager(): PoolingHttpClientConnectionManager {
        val result = PoolingHttpClientConnectionManager()
        result.maxTotal = MAX_CONNECTIONS
        return result
    }

    @Bean
    fun requestConfig(): RequestConfig {
        return RequestConfig.custom()
                .setConnectionRequestTimeout(2000)
                .setConnectTimeout(2000)
                .setSocketTimeout(2000)
                .build()
    }

    @Bean
    fun httpClient(poolingHttpClientConnectionManager: PoolingHttpClientConnectionManager, requestConfig: RequestConfig): CloseableHttpClient {
        return HttpClientBuilder
                .create()
                .setConnectionManager(poolingHttpClientConnectionManager)
                .setDefaultRequestConfig(requestConfig)
                .build()
    }

    @Bean
    fun restTemplate(httpClient: HttpClient): RestTemplate {
        val requestFactory = HttpComponentsClientHttpRequestFactory()
        requestFactory.httpClient = httpClient
        return RestTemplate(requestFactory)
    }


    val responseTimer by lazy {
        metricsRegistry.timer(MetricRegistry.name(javaClass, "latency"))}
    val responseSlidingTimer by lazy {
        metricsRegistry.register(MetricRegistry.name(javaClass, "latency", "sliding"),
                Timer(SlidingTimeWindowArrayReservoir(METRICS_SLIDING_WINDOW_SEC, TimeUnit.SECONDS)))}


//    val executor = Executors.newFixedThreadPool(MAX_CONNECTIONS)
    val executor = Executors.newScheduledThreadPool(16)
    val workQueue = ArrayBlockingQueue<Int>(MAX_CONNECTIONS)

    @Autowired
    lateinit var restTemplate: RestTemplate

    inner class Worker(val queue: BlockingQueue<Int>): Runnable {
        val faker = Faker()
        override fun run() {
            while (!Thread.currentThread().isInterrupted) {
                try {
//                    val count = queue.take()
                    val count = 25
                    responseTimer.time().use {
                        responseSlidingTimer.time().use {
                            //val count = faker.number().numberBetween(1, MAX_USER_CNT)
                            val response = restTemplate.getForObject("http://${HOST}:${PORT}/users?count=$count",
                                    GetUserResponse::class.java)
                            assert(response.users.isNotEmpty())
                        }
                    }
                    Thread.sleep(faker.number().numberBetween(10, 100).toLong())
                    // Process item
                } catch (ex: InterruptedException) {
                    Thread.currentThread().interrupt()
                    println(ex)
                    break
                }
            }
        }
    }

    @PostConstruct
    fun start() {
        val faker = Faker()

//        for (i in 0 until MAX_CONNECTIONS) {
//            executor.submit(Worker(workQueue))
//        }
        metricsReporter.start(15, TimeUnit.SECONDS)
        executor.scheduleAtFixedRate({
//            executor.execute({
                responseTimer.time().use {
                    responseSlidingTimer.time().use {
                        //val count = faker.number().numberBetween(1, MAX_USER_CNT)
                        val count = 25
                        val response = restTemplate.getForObject("http://${HOST}:${PORT}/users?count=$count",
                                GetUserResponse::class.java)
                        assert(response.users.isNotEmpty())
                    }
                }
//            })
        },0, (1f/ tps.toFloat() * 1000).toLong(), TimeUnit.MILLISECONDS)

    }
}

fun main(args: Array<String>) {
    SpringApplicationBuilder(ClientApplication::class.java)
            .web(WebApplicationType.NONE).run(*args)
}