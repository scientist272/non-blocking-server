package service;

import entity.message.Message;
import entity.message.WriteProxy;

import java.io.UnsupportedEncodingException;

public interface IMessageProcessor {
    void process(Message message, WriteProxy writeProxy) throws UnsupportedEncodingException;
}
