package entity.message;

import java.nio.ByteBuffer;

public class Message {
    public MessageBuffer messageBuffer;

    public QueueIntFlip srcBlockQueue;
    public long socketId;
    //messageBuffer里存储message数据的数组
    public byte[] sharedArray;

    //message再sharedArray里的开始位置
    public int offset;

    //message被分配的最大size,从CAPACITY_SMALL,CAPACITY_MEDIUM,CAPACITY_LARGE中选
    public int capacity;

    //当前message的长度
    public int length;

    //保存的元数据
    public Object metaData;


    public Message(MessageBuffer messageBuffer) {
        this.messageBuffer = messageBuffer;
    }

    //将缓冲区中的数据写入message中
    public int writeToMessage(ByteBuffer byteBuffer){
        int remaining = byteBuffer.remaining();
        //如果message的剩余容量不够存储缓冲区中剩余的数据，则扩容
        while(this.length+remaining>this.capacity){
            if(!this.messageBuffer.expandMessage(this)){
                return -1;
            }
        }

        //将要从缓冲区中读取的字节数
        int bytesToCopy = Math.min(remaining,this.capacity-this.length);

        byteBuffer.get(this.sharedArray,this.offset+this.length,bytesToCopy);
        this.length+=bytesToCopy;

        return bytesToCopy;
    }

    //将这个字节数组读取到消息中
    public int writeToMessage(byte[] byteArray){
        return this.writeToMessage(byteArray,0,byteArray.length);
    }

    //从字节数组中读取自定义长度的数据到消息中
    public int writeToMessage(byte[] btyeArray,int offset,int length){
            int remaining = length;
            while(this.length+remaining>this.capacity){
                if(!this.messageBuffer.expandMessage(this)){
                    return -1;
                }
            }
            int bytesToCopy = Math.min(remaining,this.capacity-this.length);
            System.arraycopy(btyeArray,offset,this.sharedArray,this.offset+this.length,bytesToCopy);
            this.length+=bytesToCopy;
            return bytesToCopy;
    }

    //将传入消息的一部分复制到一个新的消息
    public void writePartialMessageToMessage(Message message,int endIndex){
        int startIndexOfPartialMessage = message.offset+endIndex;
        int lengthOfPartailMessage = message.offset+message.length-endIndex;
        System.arraycopy(message.sharedArray,startIndexOfPartialMessage,this.sharedArray,this.offset,lengthOfPartailMessage);
    }

    //将容量归还给缓冲区
    public void free(){
        this.srcBlockQueue.put(this.offset);
    }
}
