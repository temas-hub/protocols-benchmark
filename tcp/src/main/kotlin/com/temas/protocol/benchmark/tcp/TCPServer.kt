package com.temas.protocol.benchmark.tcp

import com.temas.protocols.benchmark.transport.Server
import io.netty.bootstrap.AbstractBootstrap
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.*
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.channel.socket.nio.NioSocketChannel


/**
 * Created by azhdanov on 02.07.2018.
 */

class TCPServer : Server() {
    override fun initBoostrap(bossGroup: EventLoopGroup, workerGroup: EventLoopGroup): AbstractBootstrap<*, *> {
        val b = ServerBootstrap()
        b.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel::class.java)
                .childHandler(object : ChannelInitializer<NioSocketChannel>() {
                    override fun initChannel(ch: NioSocketChannel) {
                        val p = ch.pipeline()
                        appendDefaultHandlers(p)
                    }
                })
        return b
    }
}

fun main(args: Array<String>) {
    TCPServer().start()
}
