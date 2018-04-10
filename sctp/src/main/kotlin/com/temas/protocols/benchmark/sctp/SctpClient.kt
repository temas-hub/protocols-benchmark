package com.temas.protocols.benchmark.sctp

import com.temas.protocols.benchmark.transport.Client
import io.netty.buffer.ByteBuf
import io.netty.channel.sctp.SctpMessage
import io.netty.channel.sctp.nio.NioSctpChannel
import java.net.InetSocketAddress

object SctpClient: Client<NioSctpChannel>(NioSctpChannel::class.java) {
    override fun createRequestMessage(requestBuffer: ByteBuf, toAddress: InetSocketAddress) =
            SctpMessage(0,0, requestBuffer)
}

fun main(args: Array<String>) {
    SctpClient.init()
}
