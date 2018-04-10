package com.temas.protocols.benchmark.udp

import com.temas.protocols.benchmark.Model
import com.temas.protocols.benchmark.model.Generator
import com.temas.protocols.benchmark.transport.Server
import com.temas.protocols.benchmark.transport.encodeBuf
import com.temas.protocols.benchmark.transport.readObject
import io.netty.bootstrap.AbstractBootstrap
import io.netty.bootstrap.Bootstrap
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInitializer
import io.netty.channel.EventLoopGroup
import io.netty.channel.socket.DatagramPacket
import io.netty.channel.socket.nio.NioDatagramChannel


object UdpServer: Server(){

    override fun initBoostrap(bossGroup: EventLoopGroup, workerGroup: EventLoopGroup): AbstractBootstrap<*,*> {
        val b = Bootstrap()
        b.group(workerGroup)
                .channel(NioDatagramChannel::class.java)
                .handler(object : ChannelInitializer<NioDatagramChannel>() {
                    override fun initChannel(ch : NioDatagramChannel)  {
                        val p = ch.pipeline()
                        p.addLast(inboundHandler)
                    }
                })
        return b
    }

    override fun readMessage(msg: Any, ctx: ChannelHandlerContext){
        //TODO reliabilty
        val packet = (msg as DatagramPacket)
        val request = readObject(packet.content(), ::toProto)
        val response = prepareResponse(request)
        ctx.writeAndFlush(DatagramPacket(encodeBuf(response), packet.sender()))
        msg.release()
    }
}

fun main(args: Array<String>) {
    UdpServer.start()
}








