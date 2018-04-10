package com.temas.protocols.benchmark.sctp

import com.temas.protocols.benchmark.model.Generator
import com.temas.protocols.benchmark.transport.Server
import com.temas.protocols.benchmark.transport.encodeBuf
import com.temas.protocols.benchmark.transport.readObject
import io.netty.bootstrap.AbstractBootstrap
import io.netty.bootstrap.ServerBootstrap
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
                        p.addLast(inboundHandler)
                    }
                })
        return b
    }
    override fun readMessage(msg: Any, ctx: ChannelHandlerContext) {
        val packet = (msg as SctpMessage)
        val request = readObject(packet.content(), ::toProto)
        val response = prepareResponse(request)
        ctx.writeAndFlush(SctpMessage(0,0, encodeBuf(response)))
        msg.release()
    }
}








