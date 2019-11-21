package http;


import service.IMessageReaderFactory;
import service.IMesseageReader;

/**
 * Created by jjenkov on 18-10-2015.
 */
public class HttpMessageReaderFactory implements IMessageReaderFactory {

    public HttpMessageReaderFactory() {
    }

    @Override
    public IMesseageReader createMessageReader() {
        return new HttpMessageReader();
    }
}
