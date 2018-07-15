package com.temas.protocols.benchmark.udp

import com.temas.protocols.benchmark.transport.Client
import io.netty.buffer.ByteBuf
import io.netty.channel.*
import io.netty.channel.socket.DatagramPacket
import io.netty.channel.socket.nio.NioDatagramChannel

class UdpClient : Client<NioDatagramChannel> {
    constructor(host: String, port: Int) : super(NioDatagramChannel::class.java, host, port)
    constructor(): super(NioDatagramChannel::class.java)

    override fun appendLowerProtocolHandlers(p : ChannelPipeline) {
        p.addLast("datagramReader", object: ChannelInboundHandlerAdapter() {
            override fun channelRead(ctx: ChannelHandlerContext, msg: Any?) {
                ctx.fireChannelRead((msg as DatagramPacket).content())
            }
        })
        p.addLast("datagramWriter", object : ChannelOutboundHandlerAdapter() {
            override fun write(ctx: ChannelHandlerContext, msg: Any, promise: ChannelPromise) {
                ctx.writeAndFlush(DatagramPacket(msg as ByteBuf, remoteAddress), promise)
            }
        })
    }
}

fun main(args: Array<String>) {
    UdpClient().init()
}
