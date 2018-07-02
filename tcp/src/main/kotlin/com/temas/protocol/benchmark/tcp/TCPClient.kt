package com.temas.protocol.benchmark.tcp

import com.temas.protocols.benchmark.transport.Client
import io.netty.channel.ChannelPipeline
import io.netty.channel.socket.nio.NioSocketChannel

/**
 * Created by azhdanov on 02.07.2018.
 */

class TCPClient : Client<NioSocketChannel>(NioSocketChannel::class.java) {
    override fun appendLowerProtocolHandlers(p: ChannelPipeline) {
        // empty
    }

}

fun main(args: Array<String>) {
    TCPClient().init()
}