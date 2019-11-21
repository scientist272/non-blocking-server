## java nio服务器
如何运行：
创建主函数
```java
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
           //TODO：your code
		   /*
		   		在这里填入你想要服务器处理的逻辑
				example:获取request的消息,并在控制台打印,
				然后返回客户端hello world
				
				byte[] bytes = new byte[request.length];
				System.arraycopy(
						request.sharedArray,
						request.offset,bytes,0,request.length);
				System.out.println(new String(bytes));
				response.writeToMessage(httpResponseBytes);
		   */
            writeProxy.enqueue(response);
        };

        Server server = new Server(9999, new HttpMessageReaderFactory(), messageProcessor);

        server.start();

    }
}


```
这里的meesageReaderFactory和messageProcessor可自己编写，我的代码里是http对应的readerFactory。