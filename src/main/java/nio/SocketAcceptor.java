package nio;

import entity.Socket;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Queue;

public class SocketAcceptor implements Runnable {

    private int port;
    private Queue<Socket> socketQueue;
    private ServerSocketChannel serverSocketChannel;

    public SocketAcceptor(int port,Queue<Socket> socketQueue){
            this.port = port;
            this.socketQueue = socketQueue;
    }
    @Override
    public void run() {
        try {
            this.serverSocketChannel = ServerSocketChannel.open();
            this.serverSocketChannel.bind(new InetSocketAddress(port));
        } catch (IOException e) {
            e.printStackTrace();
        }
        while (true){
            try {
                SocketChannel socketChannel = this.serverSocketChannel.accept();
                System.out.println("accept socket:"+socketChannel);
                this.socketQueue.add(new Socket(socketChannel));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
