package com.temas.protocols.benchmark.udp

import com.codahale.metrics.*
import com.codahale.metrics.MetricRegistry.name
import com.github.javafaker.Faker
import com.temas.protocols.benchmark.Model
import com.temas.protocols.benchmark.model.User
import io.netty.bootstrap.Bootstrap
import io.netty.channel.*
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.DatagramPacket
import io.netty.channel.socket.nio.NioDatagramChannel
import java.net.InetSocketAddress
import java.time.LocalDate
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

object Client {
    // properties
    private val MAX_USER_CNT: Int = 100
    private val METRICS_SLIDING_WINDOW_SEC: Long = 60
    private val tps = Integer.parseInt(System.getProperty("tps", "30"))
    val HOST = System.getProperty("udpserver", "ec2-18-222-12-202.us-east-2.compute.amazonaws.com")
    //val HOST = System.getProperty("udpserver", "192.168.1.102")
    //val HOST = System.getProperty("udpserver", "temas-local.lan")
    val PORT = Integer.parseInt(System.getProperty("udpport", "11100"))
    val remoteAddress = InetSocketAddress(HOST, PORT)

    val executor = Executors.newSingleThreadScheduledExecutor()
    val group = NioEventLoopGroup(1)

    val b = Bootstrap()
    // benchMark
    val metricsRegistry = MetricRegistry()
    val requestTimer = metricsRegistry.timer(name(javaClass,"request-latency"))
    val slidingRequestTimer = metricsRegistry.register(name(javaClass, "request-latency", "sliding"),
                                    Timer(SlidingTimeWindowArrayReservoir(METRICS_SLIDING_WINDOW_SEC, TimeUnit.SECONDS)))
    val totalRequestCounter = metricsRegistry.counter(name(javaClass, "request-counter"))
    val successRequestConter = metricsRegistry.counter(name(javaClass, "request-counter","success"))
    val successRatio = metricsRegistry.register(name(javaClass,"success-ratio"),
            object : RatioGauge() {
                override fun getRatio(): Ratio {
                    return Ratio.of(successRequestConter.count.toDouble(),
                            totalRequestCounter.count.toDouble())
                }
            })
    val metricsReporter = ConsoleReporter.forRegistry(metricsRegistry)
            .convertDurationsTo(TimeUnit.MILLISECONDS).
                    convertRatesTo(TimeUnit.SECONDS).build()

    val responseListener = object : ChannelInboundHandlerAdapter() {
        private val prototype = Model.GetUsersResponse.getDefaultInstance()

        override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
            //registered = true
            val packet = (msg as DatagramPacket)
            val readObject = readObject(packet.content(), { buf -> convertToProto(prototype.parserForType, buf) })

            //val readObject = convertToProto(prototype.parserForType, packet.content())
            //println("Response ${readObject.userListCount}")
            val startTime = readObject.header.timestamp
            val latency = System.nanoTime() - startTime
            requestTimer.update(latency, TimeUnit.NANOSECONDS)
            slidingRequestTimer.update(latency, TimeUnit.NANOSECONDS)
            //readResponse(readObject)
            successRequestConter.inc()
            val u = readObject.getUserList(0)
            //println("Response ${u.secondName} ")
            msg.release()
        }
    }

    private fun readResponse(response: Model.GetUsersResponse): List<User> {
        return response.userListList.map { User(it.firstName, it.secondName, LocalDate.parse(it.birthdate), it.age.toShort(), it.city) }
    }

    val channelHandler = object : ChannelInitializer<NioDatagramChannel>() {
        override fun initChannel(ch: NioDatagramChannel) {
            ch.config().recvByteBufAllocator = FixedRecvByteBufAllocator(5120)
            val pipeline  = ch.pipeline()
//            pipeline.addLast(responseDecoder,
//                    ProtobufEncoder(),
//                    ProtobufDecoder(Model.UserResponse.getDefaultInstance()),
//                    responseListener)
            pipeline.addLast(responseListener)
        }
    }
    private val faker = Faker()

    init{
        b.group(group)
                .channel(NioDatagramChannel::class.java)
                .remoteAddress(remoteAddress)
                .handler(channelHandler)
    }

    fun init() {
        // Start the connection attempt.
        try {
            val channelFuture = b.connect()
            channelFuture.addListener({ future ->
                if (!future.isSuccess) {
                    println("Error connecting to ${HOST} ${PORT}")
                }
            })
            val reqestId = AtomicLong(0)
            val channel = channelFuture.sync().channel()
            val remoteAddress = InetSocketAddress(HOST, PORT)
            executor.scheduleAtFixedRate({
                sendRequest(reqestId, channel, remoteAddress)
            }, 0, (1f/ tps.toFloat() * 1000).toLong(), TimeUnit.MILLISECONDS)
            println("Client started to send requests to ${HOST}:${PORT} with reqested tps=${tps}")
            metricsReporter.start(15, TimeUnit.SECONDS)

        } catch(e: Exception) {
            e.printStackTrace()
        }
    }

    private fun sendRequest(reqestId: AtomicLong, channel: Channel, toAddress: InetSocketAddress) {
        val requestBuilder = Model.GetUsersRequest.newBuilder()
        val seqNum = reqestId.incrementAndGet()
        requestBuilder.header = Model.Header.newBuilder().setSeqNum(seqNum).setTimestamp(System.nanoTime()).build()
        //requestBuilder.count = faker.number().numberBetween(1, MAX_USER_CNT)
        requestBuilder.count = 25
        writeToChannel(channel, requestBuilder.build(), toAddress)
        totalRequestCounter.inc()
    }
}

fun main(args: Array<String>) {
    val client = Client
    Client.init()
}
