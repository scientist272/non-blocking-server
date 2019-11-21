package example;

import entity.message.Message;
import http.HttpMessageReaderFactory;
import nio.Server;
import service.IMessageProcessor;
import java.io.IOException;


public class Main {
    public static void main(String[] args) throws IOException {

        String httpResponse = "HTTP/1.1 200 OK\r\n" +
                "Content-Length: 38\r\n" +
                "Content-Type: text/html\r\n" +
                "\r\n" +
                "<html><body>hello world!</body></html>";

        byte[] httpResponseBytes = httpResponse.getBytes("UTF-8");

        IMessageProcessor messageProcessor = (request, writeProxy) -> {
            System.out.println("Message Received from socket: " + request.socketId);

            Message response = writeProxy.getMessage();
            response.socketId = request.socketId;
            response.writeToMessage(httpResponseBytes);
            byte[] message = new byte[request.length];
            System.arraycopy(request.sharedArray,request.offset,message,0,request.length);
            System.out.println("---------------------");
            System.out.println(new String(message));
            System.out.println("---------------------");
            writeProxy.enqueue(response);
        };

        Server server = new Server(9999, new HttpMessageReaderFactory(), messageProcessor);

        server.start();

    }
}
