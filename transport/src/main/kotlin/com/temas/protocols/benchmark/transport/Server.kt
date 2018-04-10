package com.temas.protocols.benchmark.transport

import com.codahale.metrics.ConsoleReporter
import com.codahale.metrics.MetricRegistry
import com.codahale.metrics.MetricRegistry.name
import com.codahale.metrics.SlidingTimeWindowArrayReservoir
import com.codahale.metrics.Timer
import com.temas.protocols.benchmark.Model
import com.temas.protocols.benchmark.model.Generator
import com.temas.protocols.benchmark.model.User
import io.netty.bootstrap.AbstractBootstrap
import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.channel.EventLoopGroup
import io.netty.channel.nio.NioEventLoopGroup
import java.net.InetAddress
import java.util.concurrent.TimeUnit


abstract class Server {
    //properties
    private val METRICS_SLIDING_WINDOW_SEC: Long = 60
    val PORT = Integer.parseInt(System.getProperty("port", "11100"))

    val metricsRegistry = MetricRegistry()
    val responseRequestHandlingTimer = metricsRegistry.timer(name(javaClass, "request-handling"))
    val responseRequestHandlingSlidingTimer = metricsRegistry.register(name(javaClass, "request-handling","sliding"),
            Timer(SlidingTimeWindowArrayReservoir(METRICS_SLIDING_WINDOW_SEC, TimeUnit.SECONDS)))
    var metricsReporter = ConsoleReporter.forRegistry(metricsRegistry)
            .convertRatesTo(TimeUnit.SECONDS)
            .convertDurationsTo(TimeUnit.MILLISECONDS)
            .build()

    private val prototype = Model.GetUsersRequest.getDefaultInstance()

    protected val inboundHandler = object : ChannelInboundHandlerAdapter() {
        override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
            responseRequestHandlingSlidingTimer.time().use {
                responseRequestHandlingTimer.time().use {
                    readMessage(msg, ctx)
                }
            }
        }
    }

    protected abstract fun readMessage(msg: Any, ctx: ChannelHandlerContext)

    protected fun prepareResponse(request: Model.GetUsersRequest): Model.GetUsersResponse {
        val userList = Generator.generateUsers(request.count)
        return buildResponse(userList, request.header)
    }

    protected fun toProto(msg: ByteBuf) = convertToProto(prototype.parserForType, msg)

    fun start() {
        val bossGroup = NioEventLoopGroup(1)
        val workerGroup = NioEventLoopGroup()
        try {

            val bootstrap = initBoostrap(bossGroup, workerGroup)
            val localHost = InetAddress.getLocalHost()
            println("Started to listen host:$localHost port: ${PORT}")
            metricsReporter.start(20, TimeUnit.SECONDS)

            // Bind and start to accept incoming connections.
            val channel = bootstrap.bind(PORT).sync().channel()
            channel.closeFuture().sync()
        } finally {
            workerGroup.shutdownGracefully()
            bossGroup.shutdownGracefully()
        }
    }

    protected abstract fun initBoostrap(bossGroup: EventLoopGroup, workerGroup: EventLoopGroup): AbstractBootstrap<*,*>

    protected fun buildResponse(userList: List<User>, header: Model.Header): Model.GetUsersResponse {
        val respBuilder = Model.GetUsersResponse.newBuilder()
        // TODO
        //respBuilder.header
        val userBuilder = Model.User.newBuilder()
        userList.forEach { u ->
            userBuilder.firstName = u.firstName
            userBuilder.secondName = u.secondName
            userBuilder.age = u.age.toInt()
            userBuilder.birthdate = u.birthDate.toString()
            userBuilder.city = u.city
            respBuilder.addUserList(userBuilder.build())
            userBuilder.clear()
        }
        respBuilder.header = header

        return respBuilder.build()
    }
}








