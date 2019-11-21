package entity;

import entity.message.MessageWriter;
import service.IMesseageReader;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class Socket {
    public long socketId;
    public SocketChannel socketChannel;
    public IMesseageReader messeageReader;
    public boolean reachEndOfStream;
    public MessageWriter messageWriter;

    public Socket() {
    }

    public Socket(SocketChannel socketChannel) {
        this.socketChannel = socketChannel;
    }

    /*
     将client端的通道数据读进缓冲区
     */
    public int read(ByteBuffer byteBuffer) throws IOException {
        int bytesRead = this.socketChannel.read(byteBuffer);
        int totalRead = bytesRead;
        while (bytesRead > 0) {
            bytesRead = this.socketChannel.read(byteBuffer);
            totalRead += bytesRead;
        }

        //如果缓冲的大小不够，读取到缓冲区的消息不是完整的消息，则这个通道的数据还没读完
        //如果通道的数据读完了，将标志置为true
        if (bytesRead == -1) {
            this.reachEndOfStream = true;
        }
        return totalRead;

    }

    /*
        将缓冲区的数据写入客户端通道
     */
    public int write(ByteBuffer byteBuffer) throws IOException {
        int bytesWritten = this.socketChannel.write(byteBuffer);
        int totalWrite = bytesWritten;
        while (bytesWritten > 0 && byteBuffer.hasRemaining()) {
            bytesWritten = this.socketChannel.write(byteBuffer);
            totalWrite += bytesWritten;
        }
        return totalWrite;
    }
}
