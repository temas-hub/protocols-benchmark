package com.temas.protocols.benchmark.sctp

import com.temas.protocols.benchmark.transport.Client
import io.netty.buffer.ByteBuf
import io.netty.channel.*
import io.netty.channel.sctp.SctpMessage
import io.netty.channel.sctp.nio.NioSctpChannel
import io.netty.channel.udt.UdtMessage
import java.net.InetSocketAddress

object SctpClient: Client<NioSctpChannel>(NioSctpChannel::class.java) {
    override fun appendLowerProtocolHandlers(p : ChannelPipeline) {
        p.addLast("messageReader", object: ChannelInboundHandlerAdapter() {
            override fun channelRead(ctx: ChannelHandlerContext, msg: Any?) {
                ctx.fireChannelRead((msg as SctpMessage).content())
            }
        })
        p.addLast("messageWriter", object : ChannelOutboundHandlerAdapter() {
            override fun write(ctx: ChannelHandlerContext, msg: Any, promise: ChannelPromise) {
                ctx.writeAndFlush(SctpMessage(0,0, msg as ByteBuf), promise)
            }
        })
    }
}

fun main(args: Array<String>) {
    SctpClient.init()
}
