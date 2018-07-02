package com.temas.protocols.benchmark.udp

import com.temas.protocols.benchmark.transport.Server
import io.netty.bootstrap.AbstractBootstrap
import io.netty.bootstrap.Bootstrap
import io.netty.buffer.ByteBuf
import io.netty.channel.*
import io.netty.channel.socket.DatagramPacket
import io.netty.channel.socket.nio.NioDatagramChannel


object UdpServer: Server() {

    val recipientInitializer = object : ChannelInboundHandlerAdapter() {
        override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
            val sender = (msg as DatagramPacket).sender()
            val pipeline = ctx.pipeline()
            pipeline.addFirst("datagramWriter", object: ChannelOutboundHandlerAdapter() {
                override fun write(ctx: ChannelHandlerContext, msg: Any, promise: ChannelPromise) {
                    val packet = (msg as ByteBuf)
                    ctx.writeAndFlush(DatagramPacket(packet, sender), promise)
                }
            })
            pipeline.remove(this)
            ctx.fireChannelRead(msg)
        }
    }

    override fun initBoostrap(bossGroup: EventLoopGroup, workerGroup: EventLoopGroup): AbstractBootstrap<*,*> {
        val b = Bootstrap()
        b.group(workerGroup)
                .channel(NioDatagramChannel::class.java)
                .handler(object : ChannelInitializer<NioDatagramChannel>() {
                    override fun initChannel(ch : NioDatagramChannel)  {
                        val p = ch.pipeline()
                        p.addLast("recipientInitializer", recipientInitializer)
                        p.addLast("datagramReader", object: ChannelInboundHandlerAdapter() {
                            override fun channelRead(ctx: ChannelHandlerContext, msg: Any?) {
                                ctx.fireChannelRead((msg as DatagramPacket).content())
                            }
                        })
                        appendDefaultHandlers(p)
                    }
                })
        return b
    }
}

fun main(args: Array<String>) {
    UdpServer.start()
}








