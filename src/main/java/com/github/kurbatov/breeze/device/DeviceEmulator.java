package com.github.kurbatov.breeze.device;

import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.shaded.netty4.io.netty.bootstrap.Bootstrap;
import org.apache.flink.shaded.netty4.io.netty.buffer.ByteBuf;
import org.apache.flink.shaded.netty4.io.netty.channel.ChannelHandlerContext;
import org.apache.flink.shaded.netty4.io.netty.channel.ChannelInboundHandlerAdapter;
import org.apache.flink.shaded.netty4.io.netty.channel.ChannelInitializer;
import org.apache.flink.shaded.netty4.io.netty.channel.ChannelOption;
import org.apache.flink.shaded.netty4.io.netty.channel.EventLoopGroup;
import org.apache.flink.shaded.netty4.io.netty.channel.nio.NioEventLoopGroup;
import org.apache.flink.shaded.netty4.io.netty.channel.socket.SocketChannel;
import org.apache.flink.shaded.netty4.io.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Emulates IoT device sending data to a specified server.
 *
 * @author Oleg Kurbatov &lt;o.v.kurbatov@gmail.com&gt;
 */
public class DeviceEmulator {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final IoTMessageGenerator messageProvider;

    private final AtomicBoolean isRunning = new AtomicBoolean(false);

    private final EventLoopGroup workerGroup = new NioEventLoopGroup();

    private static final Logger LOGGER = LoggerFactory.getLogger(DeviceEmulator.class);

    public DeviceEmulator(IoTMessageGenerator messageProvider) {
        this.messageProvider = messageProvider;
    }

    public void start(String host, int port) {
        if (isRunning.getAndSet(true)) {
            return;
        }
        LOGGER.debug("Connecting to server...");
        Bootstrap b = new Bootstrap();
        b.group(workerGroup);
        b.channel(NioSocketChannel.class);
        b.option(ChannelOption.SO_KEEPALIVE, true);
        b.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            public void initChannel(SocketChannel ch) throws Exception {
                ch.pipeline().addLast(new ChannelInboundHandlerAdapter() {
                    @Override
                    public void channelActive(ChannelHandlerContext ctx) throws Exception {
                        Thread sender = new Thread(() -> {
                            while (isRunning.get()) {
                                try {
                                    byte[] msg = objectMapper.writeValueAsBytes(messageProvider.getNextMessage());
                                    ByteBuf buffer = ctx.alloc().buffer(msg.length + 1).writeBytes(msg).writeChar('\n');
                                    ctx.writeAndFlush(buffer);
                                    System.out.println(String.format("%s\n", new String(msg)));
                                    Thread.sleep(1000);
                                } catch (JsonProcessingException e) {
                                    LOGGER.error("Cannot serialize a message.", e);
                                } catch (InterruptedException ex) {
                                    Thread.currentThread().interrupt();
                                    LOGGER.trace("IoTMessage sender thread is interrupted.");
                                }
                            }
                        }, "IoTMessage-sender");
                        sender.start();
                    }
                });
            }
        });
        b.connect(host, port);
    }

    public void stop() {
        if (isRunning.getAndSet(false)) {
            workerGroup.shutdownGracefully();
        }
    }

}
