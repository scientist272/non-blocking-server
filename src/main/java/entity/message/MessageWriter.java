package entity.message;

import entity.Socket;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

//处理给socket请求写消息的类
public class MessageWriter {

    //存储要写到socket的消息的队列
    private List<Message> writeQueue=new ArrayList<>();
    //正在处理中的消息
    private Message messageInProccess;
    //记录正在处理中的消息写了多少到socket中的指针
    private int bytesWriten;

    //添加等待处理的消息
    public void enqueue(Message message) {
        if (this.messageInProccess == null) {
            this.messageInProccess = message;
        } else {
            this.writeQueue.add(message);
        }
    }


    //向socket写消息
    public void wrtie(ByteBuffer byteBuffer, Socket socket) throws IOException {
        byteBuffer.put(this.messageInProccess.sharedArray,
                this.messageInProccess.offset + this.bytesWriten,
                this.messageInProccess.length - this.bytesWriten);

        byteBuffer.flip();
        this.bytesWriten += socket.write(byteBuffer);
        byteBuffer.clear();

        if (this.bytesWriten >= this.messageInProccess.length) {
            if (this.writeQueue.size() > 0) {
                this.messageInProccess = this.writeQueue.remove(0);
            } else {
                this.messageInProccess = null;
            }
        }
    }




    public boolean isEmpty() {
        return this.writeQueue.isEmpty() && this.messageInProccess == null;
    }

}
