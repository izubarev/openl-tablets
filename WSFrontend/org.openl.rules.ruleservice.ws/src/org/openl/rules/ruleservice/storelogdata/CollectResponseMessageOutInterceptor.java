package org.openl.rules.ruleservice.storelogdata;

import java.io.*;
import java.time.ZonedDateTime;

import org.apache.cxf.common.injection.NoJSR250Annotations;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.LoggingMessage;
import org.apache.cxf.interceptor.StaxOutInterceptor;
import org.apache.cxf.io.CacheAndWriteOutputStream;
import org.apache.cxf.io.CachedOutputStream;
import org.apache.cxf.io.CachedOutputStreamCallback;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.Phase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * CXF interceptor for collecting response data for logging to external source feature.
 *
 * @author Marat Kamalov
 *
 */
@NoJSR250Annotations
public class CollectResponseMessageOutInterceptor extends AbstractProcessLoggingMessageInterceptor {
    private static final Logger LOG = LoggerFactory.getLogger(CollectResponseMessageOutInterceptor.class);

    private final StoreLogDataManager storeLoggingManager;

    public StoreLogDataManager getStoreLoggingManager() {
        return storeLoggingManager;
    }

    public CollectResponseMessageOutInterceptor(String phase, StoreLogDataManager storeLoggingManager) {
        super(phase);
        addBefore(StaxOutInterceptor.class.getName());
        this.storeLoggingManager = storeLoggingManager;
    }

    public CollectResponseMessageOutInterceptor(StoreLogDataManager storeLoggingManager) {
        this(Phase.PRE_STREAM, storeLoggingManager);
    }

    public CollectResponseMessageOutInterceptor(int lim, StoreLogDataManager storeLoggingManager) {
        this(storeLoggingManager);
        limit = lim;
    }

    @Override
    public void handleMessage(Message message) {
        handleAnyMessage(message);
    }

    @Override
    public void handleFault(Message message) {
        final StoreLogData storeLogData = StoreLogDataHolder.get();
        storeLogData.fault();
        handleAnyMessage(message);
    }

    private void handleAnyMessage(Message message) {
        final OutputStream os = message.getContent(OutputStream.class);
        final Writer iowriter = message.getContent(Writer.class);
        if (os == null && iowriter == null) {
            return;
        }
        if (os != null) {
            final CacheAndWriteOutputStream newOut = new CacheAndWriteOutputStream(os);
            if (threshold > 0) {
                newOut.setThreshold(threshold);
            }
            if (limit > 0) {
                newOut.setCacheLimit(limit);
            }
            message.setContent(OutputStream.class, newOut);
            newOut.registerCallback(new LoggingCallback(message, os));
        } else {
            message.setContent(Writer.class, new LogWriter(message, iowriter));
        }
    }

    private static LoggingMessage setupBuffer(Message message) {
        String id = (String) message.getExchange().get(LoggingMessage.ID_KEY);
        if (id == null) {
            id = LoggingMessage.nextId();
            message.getExchange().put(LoggingMessage.ID_KEY, id);
        }
        final LoggingMessage buffer = new LoggingMessage("Outbound Message\n---------------------------", id);

        Integer responseCode = (Integer) message.get(Message.RESPONSE_CODE);
        if (responseCode != null) {
            buffer.getResponseCode().append(responseCode);
        }

        String encoding = (String) message.get(Message.ENCODING);
        if (encoding != null) {
            buffer.getEncoding().append(encoding);
        }
        String httpMethod = (String) message.get(Message.HTTP_REQUEST_METHOD);
        if (httpMethod != null) {
            buffer.getHttpMethod().append(httpMethod);
        }
        String address = (String) message.get(Message.ENDPOINT_ADDRESS);
        if (address != null) {
            buffer.getAddress().append(address);
            String uri = (String) message.get(Message.REQUEST_URI);
            if (uri != null && !address.startsWith(uri)) {
                if (!address.endsWith("/") && !uri.startsWith("/")) {
                    buffer.getAddress().append("/");
                }
                buffer.getAddress().append(uri);
            }
        }
        String ct = (String) message.get(Message.CONTENT_TYPE);
        if (ct != null) {
            buffer.getContentType().append(ct);
        }
        Object headers = message.get(Message.PROTOCOL_HEADERS);
        if (headers != null) {
            buffer.getHeader().append(headers);
        }
        return buffer;
    }

    private class LogWriter extends FilterWriter {
        StringWriter out2;
        int count;
        Message message;
        final int lim;

        public LogWriter(Message message, Writer writer) {
            super(writer);
            this.message = message;
            if (!(writer instanceof StringWriter)) {
                out2 = new StringWriter();
            }
            lim = limit == -1 ? Integer.MAX_VALUE : limit;
        }

        @Override
        public void write(int c) throws IOException {
            super.write(c);
            if (out2 != null && count < lim) {
                out2.write(c);
            }
            count++;
        }

        @Override
        public void write(char[] cbuf, int off, int len) throws IOException {
            super.write(cbuf, off, len);
            if (out2 != null && count < lim) {
                out2.write(cbuf, off, len);
            }
            count += len;
        }

        @Override
        public void write(String str, int off, int len) throws IOException {
            super.write(str, off, len);
            if (out2 != null && count < lim) {
                out2.write(str, off, len);
            }
            count += len;
        }

        @Override
        public void close() throws IOException {
            LoggingMessage buffer = setupBuffer(message);
            if (count >= lim) {
                buffer.getMessage().append("(message truncated to ").append(lim).append(" bytes)\n");
            }
            StringWriter w2 = out2;
            if (w2 == null) {
                w2 = (StringWriter) out;
            }
            String ct = (String) message.get(Message.CONTENT_TYPE);
            try {
                writePayload(buffer.getPayload(), w2, ct);
            } catch (Exception e) {
                LOG.debug("Ignored error: ", e);
            }
            String id = (String) message.getExchange().get(LoggingMessage.ID_KEY);
            LoggingMessage loggingMessage = new LoggingMessage(null, id);
            loggingMessage.getContentType().append(ct);
            loggingMessage.getPayload().append(buffer.toString());
            handleMessage(loggingMessage);
            message.setContent(Writer.class, out);
            super.close();
        }
    }

    @Override
    protected void handleMessage(LoggingMessage message) throws Fault {
        final StoreLogData storeLogData = StoreLogDataHolder.get();
        try {
            storeLogData.setResponseMessage(message);
            storeLogData.setOutcomingMessageTime(ZonedDateTime.now());
            getStoreLoggingManager().store(storeLogData);
        } finally {
            StoreLogDataHolder.remove();
        }
    }

    protected String formatLoggingMessage(LoggingMessage buffer) {
        return buffer.toString();
    }

    class LoggingCallback implements CachedOutputStreamCallback {

        private final Message message;
        private final OutputStream origStream;
        private final int lim;

        public LoggingCallback(final Message msg, final OutputStream os) {
            this.message = msg;
            this.origStream = os;
            this.lim = limit == -1 ? Integer.MAX_VALUE : limit;
        }

        @Override
        public void onFlush(CachedOutputStream cos) {
            //nothing to do
        }

        @Override
        public void onClose(CachedOutputStream cos) {
            LoggingMessage buffer = setupBuffer(message);

            String ct = (String) message.get(Message.CONTENT_TYPE);
            if (!isSaveBinaryContent() && isBinaryContent(ct)) {
                buffer.getMessage().append(BINARY_CONTENT_MESSAGE).append('\n');
                handleMessage(buffer);
                return;
            }

            if (cos.getTempFile() == null) {
                // buffer.append("Outbound Message:\n");
                if (cos.size() >= lim) {
                    buffer.getMessage().append("(message truncated to ").append(lim).append(" bytes)\n");
                }
            } else {
                buffer.getMessage().append("Outbound Message (saved to tmp file):\n");
                buffer.getMessage().append("Filename: ").append(cos.getTempFile().getAbsolutePath()).append("\n");
                if (cos.size() >= lim) {
                    buffer.getMessage().append("(message truncated to ").append(lim).append(" bytes)\n");
                }
            }
            try {
                String encoding = (String) message.get(Message.ENCODING);
                writePayload(buffer.getPayload(), cos, encoding, ct);
            } catch (Exception e) {
                LOG.debug("Error occurred: ", e);
            }

            handleMessage(buffer);
            try {
                // empty out the cache
                cos.lockOutputStream();
                cos.resetOut(null, false);
            } catch (Exception e) {
                LOG.debug("Ignored error: ", e);
            }
            message.setContent(OutputStream.class, origStream);
        }
    }
}
