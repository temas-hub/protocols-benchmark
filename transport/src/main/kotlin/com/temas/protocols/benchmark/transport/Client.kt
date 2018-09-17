package com.temas.protocols.benchmark.transport

import com.codahale.metrics.*
import com.codahale.metrics.MetricRegistry.name
import com.github.javafaker.Faker
import com.temas.protocols.benchmark.Model
import com.temas.protocols.benchmark.model.Generator
import io.netty.bootstrap.Bootstrap
import io.netty.channel.*
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.handler.codec.protobuf.ProtobufDecoder
import io.netty.handler.codec.protobuf.ProtobufEncoder
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender
import java.net.InetSocketAddress
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

abstract class Client<T: Channel> (protected val channelClass: Class<T>,
                                   protected val host: String = System.getProperty("server", defaultHost),
                                   protected val port: Int = Integer.parseInt(System.getProperty("port", defaultPort.toString()))){
    companion object {
        const val defaultHost = "localhost"
        const val defaultPort = 11100
    }
    // properties
    private val MAX_USER_CNT: Int = 100
    private val METRICS_SLIDING_WINDOW_SEC: Long = 60
    private val tps = Integer.parseInt(System.getProperty("tps", "1500"))
    private val prototype = Model.GetUsersResponse.getDefaultInstance()
    private val executor = Executors.newSingleThreadScheduledExecutor()
    private val group = buildEventLoopGroup()
    private val faker = Faker()
    private val lostPacketCandidates = LinkedHashSet<Long>()

    private var lastRecdId : Long = 0

    var delayMicroSec: Long = ( (1f / tps.toFloat()) * 1_000_000).toLong()

    protected open fun buildEventLoopGroup() = NioEventLoopGroup()

    protected abstract fun appendLowerProtocolHandlers(p : ChannelPipeline)


    protected open fun initBootstap(remoteAddress: InetSocketAddress): Bootstrap {
        val b = Bootstrap()
        return b.group(group)
                .channel(channelClass)
                .remoteAddress(remoteAddress)
                .handler( object : ChannelInitializer<T>() {
                    override fun initChannel(ch: T) {
                        ch.config().recvByteBufAllocator = FixedRecvByteBufAllocator(5120)
                        val pipeline  = ch.pipeline()
                        appendLowerProtocolHandlers(pipeline)
                        appendProtobufHandlers(pipeline)
                        pipeline.addLast(responseListener)
                    }
                })
    }

    protected fun getRemoteAddress(host: String, port: Int): InetSocketAddress {
        return InetSocketAddress(host, port)
    }

    private val responseListener = object : ChannelInboundHandlerAdapter() {
        override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
            val readObject = (msg as Model.GetUsersResponse)
            val startTime = readObject.header.timestamp
            val count = readObject.userListCount
            val seqNum = readObject.header.seqNum
            assert(count > 0)
            val latency = System.nanoTime() - startTime
            requestTimer.update(latency, TimeUnit.NANOSECONDS)
            slidingRequestTimer.update(latency, TimeUnit.NANOSECONDS)
            successRequestConter.inc()
            inboundTrafficMeter.mark(readObject.serializedSize.toLong())

            handleLostPackets(seqNum)
        }
    }

    private fun handleLostPackets(seqNum: Long) {
        val iterator = lostPacketCandidates.iterator()
        while (iterator.hasNext()) {
            val oldSeqNo = iterator.next()
            if (seqNum > oldSeqNo + 1000 / (delayMicroSec + 1)) {
                iterator.remove()
                lostPacketsConter.inc()
                if (lostPacketsConter.count.rem(100) == 0L) {
                    println("Number of lost packets = $oldSeqNo")
                }
            }
        }

        if (seqNum > lastRecdId + 1) {
            lostPacketCandidates.addAll(lastRecdId until seqNum)
        }

        lastRecdId = seqNum
    }

    fun init() {
        // Start the connection attempt.
        try {
            val remoteAddress = getRemoteAddress(host, port)
            val bootstrap = initBootstap(remoteAddress)
            val channelFuture = bootstrap.connect()
            channelFuture.addListener { future ->
                if (!future.isSuccess) {
                    println("Error connecting to ${host} ${port}")
                }
            }
            val reqestId = AtomicLong(0)
            val channel = channelFuture.sync().channel()
            printChannelOptions(channel)

            // prepare request builders
            val requestBuilder = Model.GetUsersRequest.newBuilder()
            val headerBuilder = Model.Header.newBuilder()
            // 1000 bytes is estimated packet size, 4 bytes is size of one element
            val randomRequestData = Generator.randomRequestData(1000 / 4)
            requestBuilder.addAllRandomData(randomRequestData)


            executor.scheduleAtFixedRate({
                sendRequest(reqestId, channel, randomRequestData, requestBuilder, headerBuilder)
            }, 0, delayMicroSec, TimeUnit.MICROSECONDS)

            println("Client started to send requests to ${host}:${port} with reqested tps=${tps}")
            metricsReporter.start(10, TimeUnit.SECONDS)

        } catch(e: Exception) {
            e.printStackTrace()
        }
    }

    fun printChannelOptions(channel: Channel) {
        val sendBuffer = channel.config().getOption(ChannelOption.SO_SNDBUF)
        val recvBuffer = channel.config().getOption(ChannelOption.SO_RCVBUF)
        println("Send buffer = $sendBuffer, receive buffer = $recvBuffer")
    }


    private fun sendRequest(reqestId: AtomicLong, channel: Channel,
                            randomRequestData : List<Int>,
                            requestBuilder: Model.GetUsersRequest.Builder,
                            headerBuilder: Model.Header.Builder) {
        val startTime = System.nanoTime()
        val seqNum = reqestId.incrementAndGet()
        requestBuilder.header = headerBuilder.setSeqNum(seqNum).setTimestamp(startTime).build()
        //requestBuilder.count = faker.number().numberBetween(1, MAX_USER_CNT)
        requestBuilder.count = 25
        val request = requestBuilder.build()
        channel.writeAndFlush(request)
        outboundTrafficMeter.mark(request.serializedSize.toLong())
        totalRequestCounter.inc()
    }


    protected fun appendProtobufHandlers(p : ChannelPipeline) {
        p.addLast("frameDecoder", ProtobufVarint32FrameDecoder())
        p.addLast("protoDecoder", ProtobufDecoder(prototype))

        p.addLast("lengthPrepender", ProtobufVarint32LengthFieldPrepender())
        p.addLast("protoEncoder", ProtobufEncoder())
    }


    // >>>>benchMark
    val metricsRegistry = MetricRegistry()
    val requestTimer = metricsRegistry.timer(name(javaClass,"request-latency"))
    val slidingRequestTimer = metricsRegistry.register(name(javaClass, "request-latency", "sliding"),
            Timer(SlidingTimeWindowArrayReservoir(METRICS_SLIDING_WINDOW_SEC, TimeUnit.SECONDS)))
    val totalRequestCounter = metricsRegistry.counter(name(javaClass, "request-counter"))
    val successRequestConter = metricsRegistry.counter(name(javaClass, "request-counter","success"))
    val lostPacketsConter = metricsRegistry.counter(name(javaClass, "lost-packets"))
    val lostRatio = metricsRegistry.register(name(javaClass,"lost-ratio"),
            object : RatioGauge() {
                override fun getRatio(): Ratio {
                    return Ratio.of(lostPacketsConter.count.toDouble(),
                            totalRequestCounter.count.toDouble())
                }
            })
    val outboundTrafficMeter = metricsRegistry.meter(name(javaClass, "outbound-meter"))
    val inboundTrafficMeter = metricsRegistry.meter(name(javaClass, "inbound-meter"))

    val metricsReporter = ConsoleReporter.forRegistry(metricsRegistry)
            .convertDurationsTo(TimeUnit.MICROSECONDS)
            .filter { name, _ -> !name.contains("-counter")}
            .disabledMetricAttributes(setOf(MetricAttribute.M1_RATE,MetricAttribute.M5_RATE,
                                            MetricAttribute.M15_RATE, MetricAttribute.MEAN, MetricAttribute.STDDEV))
            .convertRatesTo(TimeUnit.SECONDS)
            .build()
    // <<<<<<benchMark
}
