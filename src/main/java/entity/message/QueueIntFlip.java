package entity.message;

/*
循环队列，readPos表示下一个出队的位置，writePos表示下一个入队的位置。
capacity表示队列容量，flipped表示writePos是否超过了capacity，如果
超过了，flipped置位true,且将writePos置零，如果writePos小于readPos，
则可以继续入队到readPos位置。
 */
public class QueueIntFlip {
    public int[] elements;
    public int capacity;
    public int readPos;
    public int writePos;
    public boolean flipped;

    public QueueIntFlip(int capacity) {
        this.elements = new int[capacity];
        this.capacity = capacity;
    }

    public void reset() {
        this.writePos = 0;
        this.readPos = 0;
        this.flipped = false;
    }

    /*
    返回队列中可用的元素个数
     */
    public int available() {
        if (!flipped) {
            return writePos - readPos;
        } else {
            return capacity - readPos + writePos;
        }

    }

    public int remainingCapacity() {
        if (!flipped) {
            return capacity - writePos;
        } else {
            return readPos - writePos;
        }
    }

    /*
        向循环队列中添加元素
     */
    public boolean put(int element) {
        if (!flipped) {
            if (writePos == capacity) {
                writePos = 0;
                flipped = true;
                if (writePos < readPos) {
                    elements[writePos++] = element;
                    return true;
                } else {
                    return false;
                }
            } else {
                elements[writePos++] = element;
                return true;
            }
        } else {
            if (writePos < readPos) {
                elements[writePos++] = element;
                return true;
            } else {
                return false;
            }
        }
    }

    /*
        从循环队列中取元素
     */
    public int take() {
        if (!flipped) {
            if (readPos < writePos) {
                return elements[readPos++];
            } else {
                return -1;
            }
        } else {
            if (readPos == capacity) {
                readPos = 0;
                flipped = false;
                if (readPos < writePos) {
                    return elements[readPos++];
                } else {
                    return -1;
                }
            } else {
                return elements[readPos++];
            }
        }
    }
}




