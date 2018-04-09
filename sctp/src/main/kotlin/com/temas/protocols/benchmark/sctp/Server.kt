package com.temas.protocols.benchmark.sctp

import com.codahale.metrics.ConsoleReporter
import com.codahale.metrics.MetricRegistry
import com.codahale.metrics.MetricRegistry.name
import com.codahale.metrics.SlidingTimeWindowArrayReservoir
import com.codahale.metrics.Timer
import com.temas.protocols.benchmark.Model
import com.temas.protocols.benchmark.model.Generator
import com.temas.protocols.benchmark.model.User
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.sctp.SctpChannel
import io.netty.channel.sctp.SctpMessage
import io.netty.channel.sctp.nio.NioSctpServerChannel
import java.net.InetAddress
import java.util.concurrent.TimeUnit

fun main(args: Array<String>) {
    Server.start()
}

object Server {
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

    private val inboundHandler = object : ChannelInboundHandlerAdapter() {
        override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
            responseRequestHandlingSlidingTimer.time().use {
                responseRequestHandlingTimer.time().use {
                    //println("${msg.javaClass}")
                    val packet = (msg as SctpMessage)
                    val readObject = readObject(packet.content(), { buf -> convertToProto(prototype.parserForType, buf) })
                    //println("Request ${readObject.count}")
                    handleRequest(ctx, readObject.count, readObject.header)
                    msg.release()
                }
            }
        }
    }
    fun start() {
        val bossGroup = NioEventLoopGroup(1)
        val workerGroup = NioEventLoopGroup()
        try {
            val b = ServerBootstrap()
            b.group(bossGroup, workerGroup)
                    .channel(NioSctpServerChannel::class.java)
                    .option(ChannelOption.SO_BACKLOG, 100)
                    .childHandler(object : ChannelInitializer<SctpChannel>() {
                        override fun initChannel(ch : SctpChannel)  {
                            val p = ch.pipeline()
                            p.addLast(
                                    inboundHandler
                            )
                        }
                    })

            // Bind and start to accept incoming connections.
            val localHost = InetAddress.getLocalHost()
            println("Started to listen host:$localHost port: ${PORT}")
            metricsReporter.start(20, TimeUnit.SECONDS)

            val channel = b.bind(PORT).sync().channel()
            //val channel = b.bind(PORT).sync().channel()
            channel.closeFuture().sync()
        } finally {
            workerGroup.shutdownGracefully()
        }
    }


    fun handleRequest(ctx: ChannelHandlerContext, numberOfRecords: Int, header: Model.Header) {
        //TODO reliabilty
        val userList = Generator.generateUsers(numberOfRecords)
        val response = buildResponse(userList, header)
        ctx.writeAndFlush(SctpMessage(0,0, encodeBuf(response)))
    }

    private fun buildResponse(userList: List<User>, header: Model.Header): Model.GetUsersResponse {
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








