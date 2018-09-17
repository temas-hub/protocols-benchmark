package com.temas.protocols.benchmark.sctp

import com.temas.protocols.benchmark.transport.Server
import io.netty.bootstrap.AbstractBootstrap
import io.netty.bootstrap.ServerBootstrap
import io.netty.buffer.ByteBuf
import io.netty.channel.*
import io.netty.channel.sctp.SctpChannel
import io.netty.channel.sctp.SctpMessage
import io.netty.channel.sctp.nio.NioSctpServerChannel

fun main(args: Array<String>) {
    SctpServer.start()
}

object SctpServer: Server() {

    override fun initBoostrap(bossGroup: EventLoopGroup, workerGroup: EventLoopGroup):AbstractBootstrap<*,*> {
        val b = ServerBootstrap()
        b.group(bossGroup, workerGroup)
                .channel(NioSctpServerChannel::class.java)
                .option(ChannelOption.SO_BACKLOG, 100)
                .childHandler(object : ChannelInitializer<SctpChannel>() {
                    override fun initChannel(ch: SctpChannel) {
                        val p = ch.pipeline()
                        ch.config().recvByteBufAllocator = FixedRecvByteBufAllocator(5120)
                        p.addLast("messageReader", object: ChannelInboundHandlerAdapter() {
                            override fun channelRead(ctx: ChannelHandlerContext, msg: Any?) {
                                ctx.fireChannelRead((msg as SctpMessage).content())
                            }
                        })
                        appendDefaultHandlers(p)
                        p.addFirst("messageWriter", object: ChannelOutboundHandlerAdapter() {
                            override fun write(ctx: ChannelHandlerContext, msg: Any, promise: ChannelPromise) {
                                val packet = (msg as ByteBuf)
                                ctx.writeAndFlush(SctpMessage(0,0, packet), promise)
                            }
                        })
                    }
                })
        return b
    }
}








