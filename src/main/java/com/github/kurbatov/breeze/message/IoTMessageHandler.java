package com.github.kurbatov.breeze.message;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.flink.shaded.netty4.io.netty.channel.ChannelHandler;
import org.apache.flink.shaded.netty4.io.netty.channel.ChannelHandlerContext;
import org.apache.flink.shaded.netty4.io.netty.channel.ChannelInboundHandlerAdapter;
import org.apache.flink.streaming.api.functions.source.SourceFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Oleg Kurbatov &lt;o.v.kurbatov@gmail.com&gt;
 */
@ChannelHandler.Sharable
public class IoTMessageHandler extends ChannelInboundHandlerAdapter implements Serializable {

    private final List<SourceFunction.SourceContext<IoTMessage>> contexts = new CopyOnWriteArrayList<>();
    
    private final AtomicInteger idx = new AtomicInteger();
    
    private static final Logger LOGGER = LoggerFactory.getLogger(IoTMessageHandler.class);
    
    private static final long serialVersionUID = -9097649975248916124L;
    
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        IoTMessage message = (IoTMessage) msg;
        if (!contexts.isEmpty()) {
            //TODO devise better distribution method
            int current = idx.getAndUpdate(i -> i >= contexts.size() - 1 ? 0 : i + 1);
            contexts.get(current).collectWithTimestamp(message, message.getTimestamp());
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (cause instanceof IOException) {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Connection dropped: {}", ctx.channel().remoteAddress(), cause);
            } else {
                LOGGER.debug("Connection dropped: {}", ctx.channel().remoteAddress());
            }
        } else {
            LOGGER.error("Processing IoTMessage error.", cause);
        }
        ctx.close();
    }
    
    public void addContext(SourceFunction.SourceContext<IoTMessage> ctx) {
        contexts.add(ctx);
    }
    
    public void removeContext(SourceFunction.SourceContext<IoTMessage> ctx) {
        idx.set(0);
        contexts.remove(ctx);
    }
    
}
