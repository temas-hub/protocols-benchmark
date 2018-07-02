package com.temas.protocols.benchmark.udt

import com.temas.protocols.benchmark.transport.Client
import io.netty.bootstrap.Bootstrap
import io.netty.buffer.ByteBuf
import io.netty.channel.*
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.DatagramPacket
import io.netty.channel.socket.nio.NioDatagramChannel
import io.netty.channel.udt.UdtMessage
import io.netty.channel.udt.nio.NioUdtMessageConnectorChannel
import java.net.InetSocketAddress
import io.netty.channel.udt.nio.NioUdtProvider
import io.netty.util.concurrent.DefaultThreadFactory



object UdtClient : Client<NioUdtMessageConnectorChannel>(NioUdtMessageConnectorChannel::class.java) {
    override fun buildEventLoopGroup(): NioEventLoopGroup {
        val connectFactory = DefaultThreadFactory("connect")
        return NioEventLoopGroup(0, connectFactory, NioUdtProvider.MESSAGE_PROVIDER)
    }

    override fun appendLowerProtocolHandlers(p : ChannelPipeline) {
        p.addLast("messageReader", object: ChannelInboundHandlerAdapter() {
            override fun channelRead(ctx: ChannelHandlerContext, msg: Any?) {
                ctx.fireChannelRead((msg as UdtMessage).content())
            }
        })
        p.addLast("messageWriter", object : ChannelOutboundHandlerAdapter() {
            override fun write(ctx: ChannelHandlerContext, msg: Any, promise: ChannelPromise) {
                ctx.writeAndFlush(UdtMessage(msg as ByteBuf), promise)
            }
        })
    }

}

fun main(args: Array<String>) {
    UdtClient.init()
}


