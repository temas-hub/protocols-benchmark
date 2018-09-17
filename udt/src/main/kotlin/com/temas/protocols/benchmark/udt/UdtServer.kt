package com.temas.protocols.benchmark.udt

import com.temas.protocols.benchmark.transport.Server
import com.temas.protocols.benchmark.transport.encodeBuf
import com.temas.protocols.benchmark.transport.readObject
import io.netty.bootstrap.AbstractBootstrap
import io.netty.bootstrap.ServerBootstrap
import io.netty.buffer.ByteBuf
import io.netty.channel.*
import io.netty.channel.udt.UdtChannel
import io.netty.channel.udt.UdtMessage
import io.netty.channel.udt.nio.NioUdtProvider
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.DatagramPacket
import io.netty.handler.codec.protobuf.ProtobufDecoder
import io.netty.handler.codec.protobuf.ProtobufEncoder
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender
import io.netty.util.concurrent.DefaultThreadFactory
import java.net.InetSocketAddress
import java.util.concurrent.ThreadFactory




object UdtServer: Server(){

    override fun initBoostrap(bossGroup: EventLoopGroup, workerGroup: EventLoopGroup): AbstractBootstrap<*,*> {
        val acceptFactory = DefaultThreadFactory("accept")
        val connectFactory = DefaultThreadFactory("connect")
        val acceptGroup = NioEventLoopGroup(0, acceptFactory, NioUdtProvider.MESSAGE_PROVIDER)
        val connectGroup = NioEventLoopGroup(0, connectFactory, NioUdtProvider.MESSAGE_PROVIDER)

        val b = ServerBootstrap()
        b.group(acceptGroup, connectGroup)
                .channelFactory(NioUdtProvider.MESSAGE_ACCEPTOR)
                .option(ChannelOption.SO_BACKLOG, 10)
                .childHandler(object : ChannelInitializer<UdtChannel>() {
                    override fun initChannel(ch: UdtChannel) {
                        val p = ch.pipeline()
                        ch.config().recvByteBufAllocator = FixedRecvByteBufAllocator(5120)
                        p.addLast("messageReader", object: ChannelInboundHandlerAdapter() {
                            override fun channelRead(ctx: ChannelHandlerContext, msg: Any?) {
                                ctx.fireChannelRead((msg as UdtMessage).content())
                            }
                        })
                        appendDefaultHandlers(p)
                        p.addFirst("messageWriter", object: ChannelOutboundHandlerAdapter() {
                            override fun write(ctx: ChannelHandlerContext, msg: Any, promise: ChannelPromise) {
                                val packet = (msg as ByteBuf)
                                ctx.writeAndFlush(UdtMessage(packet), promise)
                            }
                        })
                    }
                })
        return b
    }
}

fun main(args: Array<String>) {
    UdtServer.start()
}








