package nio;

import entity.Socket;
import entity.message.Message;
import entity.message.MessageBuffer;
import entity.message.MessageWriter;
import entity.message.WriteProxy;
import service.IMessageProcessor;
import service.IMessageReaderFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.*;

public class SocketProcessor implements Runnable {
    //保存入站socket连接的队列
    private Queue<Socket> inboundSocketQueue;

    //读消息的缓冲
    private MessageBuffer readMessageBuffer;
    //写消息的缓冲
    private MessageBuffer writeMessageBuffer;

    //消息读取器的工厂
    private IMessageReaderFactory messageReaderFactory;

    //保存出站socket连接的队列
    private Queue<Message> outboundMessageQueue = new LinkedList<>();

    private Map<Long, Socket> socketMap = new HashMap<>();

    private ByteBuffer readByteBuffer = ByteBuffer.allocate(1024 * 1024);
    private ByteBuffer writeByteBuffer = ByteBuffer.allocate(1024 * 1024);
    private Selector readSelector;
    private Selector writeSelector;

    private IMessageProcessor messageProcessor;
    private WriteProxy writeProxy;

    private long nextSocketId = 16 * 1024;

    private Set<Socket> emptyToNonEmptySockets = new HashSet<>();
    private Set<Socket> nonEmptyToEmptySockets = new HashSet<>();


    public SocketProcessor(Queue<Socket> inboundSocketQueue,
                           MessageBuffer readMessageBuffer,
                           MessageBuffer writeMessageBuffer,
                           IMessageReaderFactory messageReaderFactory,
                           IMessageProcessor messageProcessor) throws IOException {
        this.inboundSocketQueue = inboundSocketQueue;

        this.readMessageBuffer = readMessageBuffer;
        this.writeMessageBuffer = writeMessageBuffer;
        this.writeProxy = new WriteProxy(writeMessageBuffer, this.outboundMessageQueue);

        this.messageReaderFactory = messageReaderFactory;

        this.messageProcessor = messageProcessor;

        this.readSelector = Selector.open();
        this.writeSelector = Selector.open();
    }

    //每0.1秒进行一次处理循环
    @Override
    public void run() {
        while (true) {
            try {
                executeCycle();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void executeCycle() throws IOException {
        takeNewSocket();
        readFromSockets();
        writeToSockets();
    }

    //非阻塞的从socket队列中取socket连接并注册到选择器中
    public void takeNewSocket() throws IOException {
        Socket newSocket = this.inboundSocketQueue.poll();

        while (newSocket != null) {
            newSocket.socketId = this.nextSocketId++;
            //设置为非阻塞方式
            newSocket.socketChannel.configureBlocking(false);

            //为socket设置读取器
            newSocket.messeageReader = this.messageReaderFactory.createMessageReader();
            newSocket.messeageReader.init(this.readMessageBuffer);
            //为socket设置写入器
            newSocket.messageWriter = new MessageWriter();

            this.socketMap.put(newSocket.socketId, newSocket);

            //注册socket到选择器，监听读
            SelectionKey key = newSocket.socketChannel.register(this.readSelector, SelectionKey.OP_READ);
            key.attach(newSocket);

            newSocket = this.inboundSocketQueue.poll();
        }

    }

    public void readFromSockets() throws IOException {
        //监听有数据的通道
        int readyToRead = this.readSelector.selectNow();

        //如果任意socket有数据可读
        if (readyToRead > 0) {
            //或许选择器
            Set<SelectionKey> selectionKeys = this.readSelector.selectedKeys();
            Iterator<SelectionKey> keyIterator = selectionKeys.iterator();
            //遍历所有有数据可读的socket
            while (keyIterator.hasNext()) {
                SelectionKey key = keyIterator.next();
                readFromSocket(key);
                keyIterator.remove();
            }
            selectionKeys.clear();
        }

    }

    //从某个socket连接中读数据
    private void readFromSocket(SelectionKey key) throws IOException {
        Socket socket = (Socket) key.attachment();
        socket.messeageReader.read(socket, this.readByteBuffer);
        List<Message> fullMessages = socket.messeageReader.getMessages();
        if (fullMessages.size() > 0) {
            for (Message message : fullMessages) {
                message.socketId = socket.socketId;
                this.messageProcessor.process(message, this.writeProxy);
                //将处理后的消息所在的缓冲区位置清空
                message.free();
            }
            fullMessages.clear();
        }
        if (socket.reachEndOfStream) {
            System.out.println("socket closed:" + socket.socketId);
            this.socketMap.remove(socket.socketId);
            key.attach(null);
            key.cancel();
            key.channel().close();
        }
    }

    public void writeToSockets() throws IOException {

        //将所有要写的消息出队并加入到它们对应的socket中，将socket加入等待注册的socket队列中
        takeNewOutbountMessages();
        //取消所有无消息可写的socket
        cancelEmptySockets();
        //注册所有有消息可写的socket
        registerNonEmptySockets();

        int readyTowrite = this.writeSelector.selectNow();
        if (readyTowrite > 0) {
            Set<SelectionKey> selectionKeys = this.writeSelector.selectedKeys();
            Iterator<SelectionKey> keyIterator = selectionKeys.iterator();
            while (keyIterator.hasNext()) {
                SelectionKey key = keyIterator.next();
                Socket socket = (Socket) key.attachment();
                socket.messageWriter.wrtie(this.writeByteBuffer, socket);
                //如果当前socket已经写完，则将它加入到等待取消的集合中
                if (socket.messageWriter.isEmpty()) {
                    this.nonEmptyToEmptySockets.add(socket);
                }
                keyIterator.remove();
            }
            selectionKeys.clear();
        }
    }


    //将所有要写的message放进它们对应socket对象的messageWriter中
    private void takeNewOutbountMessages() {
        Message outMessage = this.outboundMessageQueue.poll();
        while (outMessage != null) {
            Socket socket = this.socketMap.get(outMessage.socketId);
            if (socket != null) {
                MessageWriter messageWriter = socket.messageWriter;
                if (messageWriter.isEmpty()) {
                    messageWriter.enqueue(outMessage);
                    //如果messageWriter为空，说明当前socket已经加入到等待取消的集合中，应将它删除
                    this.nonEmptyToEmptySockets.remove(socket);
                    this.emptyToNonEmptySockets.add(socket);
                } else {
                    messageWriter.enqueue(outMessage);
                }
            }
            outMessage = this.outboundMessageQueue.poll();
        }
    }

    //将所有已经写完的socket取消掉
    private void cancelEmptySockets() {
        for (Socket socket : this.nonEmptyToEmptySockets) {
            SelectionKey key = socket.socketChannel.keyFor(this.writeSelector);
            key.cancel();
        }
        this.nonEmptyToEmptySockets.clear();
    }

    private void registerNonEmptySockets() throws IOException {
        for (Socket socket : this.emptyToNonEmptySockets) {
            socket.socketChannel.register(this.writeSelector, SelectionKey.OP_WRITE, socket);
            socket.socketChannel.configureBlocking(false);
        }
        this.emptyToNonEmptySockets.clear();
    }


}
