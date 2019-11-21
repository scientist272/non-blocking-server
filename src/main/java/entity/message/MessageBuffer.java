package entity.message;

/*
一个共享缓冲区，其中可以包含许多消息。一条消息获取要使用的缓冲区的一部分。
如果消息的大小超出了该部分的大小，则该消息将请求一个较大的部分，然后将消息复制到该较大的部分。
然后，较小的部分再次被释放。
 */
public class MessageBuffer {

    public static final int KB = 1024;
    public static final int MB = 1024 * 1024;

    public static final int CAPACITY_SMALL = 4 * KB;
    public static final int CAPACITY_MEDIUM = 128 * KB;
    public static final int CAPACITY_LARGE = 1024 * KB;

    //package scope (default) - so they can be accessed from unit tests.
    private byte[] smallMessageBuffer = new byte[1024 * 4 * KB];   //1024 x   4KB messages =  4MB.
    private byte[] mediumMessageBuffer = new byte[128 * 128 * KB];   // 128 x 128KB messages = 16MB.
    private byte[] largeMessageBuffer = new byte[16 * 1 * MB];   //  16 *   1MB messages = 16MB.

    private QueueIntFlip smallMessageBufferFreeBlocks = new QueueIntFlip(1024); // 1024 free sections
    private QueueIntFlip mediumMessageBufferFreeBlocks = new QueueIntFlip(128);  // 128  free sections
    private QueueIntFlip largeMessageBufferFreeBlocks = new QueueIntFlip(16);   // 16   free sections


    /*
        初始化三个循环队列用来记录buffer的空闲块信息
     */
    public MessageBuffer() {
        for (int i = 0; i < smallMessageBuffer.length; i += CAPACITY_SMALL) {
            smallMessageBufferFreeBlocks.put(i);
        }
        for (int i = 0; i < mediumMessageBuffer.length; i += CAPACITY_MEDIUM) {
            mediumMessageBufferFreeBlocks.put(i);
        }
        for (int i = 0; i < largeMessageBuffer.length; i += CAPACITY_LARGE) {
            largeMessageBufferFreeBlocks.put(i);
        }
    }

    //获取message的工厂方法
    public Message getMessage() {
        int nextFreeSmallBlock = this.smallMessageBufferFreeBlocks.take();
        if (nextFreeSmallBlock == -1) {
            return null;
        }
        Message message = new Message(this);

        message.srcBlockQueue = this.smallMessageBufferFreeBlocks;
        message.sharedArray = this.smallMessageBuffer;
        message.offset = nextFreeSmallBlock;
        message.length = 0;
        message.capacity = CAPACITY_SMALL;
        return message;
    }

    //当消息的长度超过存储其数组长度时，调用扩容方法，最大消息长度1M
    public boolean expandMessage(Message message) {
        if (message.capacity == CAPACITY_SMALL) {
            return moveMessage(message, this.smallMessageBufferFreeBlocks, this.mediumMessageBufferFreeBlocks, this.mediumMessageBuffer, CAPACITY_MEDIUM);
        } else if (message.capacity == CAPACITY_MEDIUM) {
            return moveMessage(message, this.mediumMessageBufferFreeBlocks, this.largeMessageBufferFreeBlocks, this.largeMessageBuffer, CAPACITY_LARGE);
        } else {
            return false;
        }
    }

    //helper方法，将当前消息转移到另一个存储数组中,在message需要扩容的时候才使用
    private boolean moveMessage(Message message, QueueIntFlip srcBlockQueue, QueueIntFlip destBlockQueue, byte[] dest, int newCapacity) {
        int nextFreeBlock = destBlockQueue.take();
        if (nextFreeBlock == -1) {
            return false;
        }
        System.arraycopy(message.sharedArray, message.offset, dest, nextFreeBlock, message.length);

        //将原来数组中存储的该条message的空间清理，分配给之后的message
        srcBlockQueue.put(message.offset);

        message.offset = nextFreeBlock;
        message.sharedArray = dest;
        message.capacity = newCapacity;
        message.srcBlockQueue = destBlockQueue;
        return true;
    }
}
