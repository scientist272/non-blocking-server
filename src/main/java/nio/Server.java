package nio;

import entity.Socket;
import entity.message.MessageBuffer;
import service.IMessageProcessor;
import service.IMessageReaderFactory;

import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

public class Server {
    private SocketAcceptor socketAcceptor;
    private SocketProcessor socketProcessor;
    private IMessageReaderFactory messageReaderFactory;
    private IMessageProcessor messageProcessor;

    private int tcpPort;

    public Server(int tcpPort,IMessageReaderFactory messageReaderFactory,IMessageProcessor messageProcessor){
        this.tcpPort = tcpPort;
        this.messageReaderFactory = messageReaderFactory;
        this.messageProcessor = messageProcessor;
    }

    public void start() throws IOException {
        //最大连接为1024个
        Queue<Socket> socketQueue = new ArrayBlockingQueue<>(1024);
        this.socketAcceptor = new SocketAcceptor(this.tcpPort,socketQueue);

        MessageBuffer messageReadBuffer = new MessageBuffer();
        MessageBuffer messageWriteBuffer = new MessageBuffer();

        this.socketProcessor = new SocketProcessor(socketQueue,messageReadBuffer,messageWriteBuffer,messageReaderFactory,messageProcessor);

        Thread acceptor = new Thread(this.socketAcceptor);
        Thread processor = new Thread(this.socketProcessor);
        acceptor.start();
        processor.start();
    }
}
