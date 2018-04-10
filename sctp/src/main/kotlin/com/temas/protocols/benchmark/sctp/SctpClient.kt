package com.temas.protocols.benchmark.sctp

import com.temas.protocols.benchmark.transport.Client
import io.netty.buffer.ByteBuf
import io.netty.channel.sctp.SctpChannel
import io.netty.channel.sctp.SctpMessage
import java.net.InetSocketAddress

object SctpClient: Client<SctpChannel>(SctpChannel::class.java) {
    override fun createRequestMessage(requestBuffer: ByteBuf, toAddress: InetSocketAddress) =
            SctpMessage(0,0, requestBuffer)
}

fun main(args: Array<String>) {
    SctpClient.init()
}
