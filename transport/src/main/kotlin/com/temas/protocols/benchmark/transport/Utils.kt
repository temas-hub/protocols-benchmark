package com.temas.protocols.benchmark.transport

import com.google.protobuf.MessageLite
import com.google.protobuf.MessageOrBuilder
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.channel.Channel
import io.netty.channel.socket.DatagramPacket
import java.net.InetSocketAddress

fun convertToBuffer(obj : MessageOrBuilder): ByteBuf {
    return Unpooled.wrappedBuffer((obj as MessageLite).toByteArray())
}

fun writeToChannel(channel: Channel, message: MessageOrBuilder, toAddress: InetSocketAddress) {
    val payLoadBuffer = encodeBuf(message)
    channel.writeAndFlush(DatagramPacket(payLoadBuffer, toAddress))
}

fun encodeBuf(message: MessageOrBuilder): ByteBuf {
    val objectBuffer = convertToBuffer(message)
    //val length = objectBuffer.readableBytes()
    //val lengthBuffer = Unpooled.buffer(2)
    //lengthBuffer.writeShort(length)
    //return Unpooled.wrappedBuffer(lengthBuffer, objectBuffer)
    return objectBuffer;
}

fun<T> convertToProto(parser: com.google.protobuf.Parser<T>, msg: ByteBuf): T {
    val array: ByteArray
    val offset: Int
    val length = msg.readableBytes()
    if (msg.hasArray()) {
        array = msg.array()
        offset = msg.arrayOffset() + msg.readerIndex()
    } else {
        array = ByteArray(length)
        msg.getBytes(msg.readerIndex(), array, 0, length)
        offset = 0
    }

    return parser.parseFrom(array, offset, length)
}

fun<T> readObject(buffer: ByteBuf, decoder: (objBuffer: ByteBuf) -> T): T {
//    var length: Int
//    if (buffer.readableBytes() > 2) {
//        length = buffer.readUnsignedShort()
//    } else {
//        throw IllegalStateException("Expected 2 bytes length header, but found ${buffer.readableBytes()}")
//    }
//    val objBuffer = buffer.readSlice(length)

    try {
        return decoder.invoke(buffer)
    } catch (e: Exception) {
        throw RuntimeException(e)
    }
}