package com.temas.protocols.benchmark.udp

import com.temas.protocols.benchmark.transport.Client
import io.netty.buffer.ByteBuf
import io.netty.channel.socket.DatagramPacket
import io.netty.channel.socket.nio.NioDatagramChannel
import java.net.InetSocketAddress

object UdpClient : Client<NioDatagramChannel>(NioDatagramChannel::class.java) {
    override fun createRequestMessage(requestBuffer: ByteBuf, toAddress: InetSocketAddress) =
            DatagramPacket(requestBuffer, toAddress)
}

fun main(args: Array<String>) {
    UdpClient.init()
}
