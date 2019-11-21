package service;

import entity.message.Message;
import entity.message.MessageBuffer;
import entity.Socket;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

public interface IMesseageReader {
     void init(MessageBuffer readMessageBuffer);
     void read(Socket socket, ByteBuffer byteBuffer) throws IOException;
     List<Message> getMessages();
}
