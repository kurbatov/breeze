package com.github.kurbatov.breeze.conf;

import com.github.kurbatov.breeze.message.IoTMessageHandler;
import com.github.kurbatov.breeze.message.IoTMessage;
import java.util.List;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.core.type.TypeReference;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.shaded.netty4.io.netty.bootstrap.ServerBootstrap;
import org.apache.flink.shaded.netty4.io.netty.buffer.ByteBuf;
import org.apache.flink.shaded.netty4.io.netty.buffer.ByteBufInputStream;
import org.apache.flink.shaded.netty4.io.netty.channel.ChannelHandlerContext;
import org.apache.flink.shaded.netty4.io.netty.channel.ChannelInitializer;
import org.apache.flink.shaded.netty4.io.netty.channel.ChannelOption;
import org.apache.flink.shaded.netty4.io.netty.channel.EventLoopGroup;
import org.apache.flink.shaded.netty4.io.netty.channel.nio.NioEventLoopGroup;
import org.apache.flink.shaded.netty4.io.netty.channel.socket.SocketChannel;
import org.apache.flink.shaded.netty4.io.netty.channel.socket.nio.NioServerSocketChannel;
import org.apache.flink.shaded.netty4.io.netty.handler.codec.ByteToMessageDecoder;
import org.apache.flink.shaded.netty4.io.netty.handler.codec.DelimiterBasedFrameDecoder;
import org.apache.flink.shaded.netty4.io.netty.handler.codec.Delimiters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.http.server.reactive.ReactorHttpHandlerAdapter;
import org.springframework.web.reactive.DispatcherHandler;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import org.springframework.web.server.WebHandler;
import org.springframework.web.server.adapter.WebHttpHandlerBuilder;
import reactor.ipc.netty.NettyContext;
import reactor.ipc.netty.http.server.HttpServer;

/**
 *
 * @author Oleg Kurbatov &lt;o.v.kurbatov@gmail.com&gt;
 */
@Configuration
@EnableWebFlux
public class ServerConfiguration implements WebFluxConfigurer {
    
    @Value("${server.iot.port:9909}")
    private int iotServerPort;
    
    @Value("${server.http.port:8080}")
    private int httpServerPort;
    
    private static final Logger LOGGER = LoggerFactory.getLogger(ServerConfiguration.class);
    
    @Bean(destroyMethod = "shutdownGracefully")
    public EventLoopGroup getBossGroup() {
        return new NioEventLoopGroup();
    }
    
    @Bean(destroyMethod = "shutdownGracefully")
    public EventLoopGroup getWorkerGroup() {
        return new NioEventLoopGroup();
    }
    
    @Bean
    public IoTMessageHandler getIoTMessageHandler() {
        return new IoTMessageHandler();
    }
    
    @Bean
    public ServerBootstrap getIoTServer() {
        ObjectMapper objectMapper = new ObjectMapper();
        TypeReference<IoTMessage> typeRefence = new TypeReference<IoTMessage>() {};
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(getBossGroup(), getWorkerGroup())
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline().addLast(new DelimiterBasedFrameDecoder(8192, Delimiters.lineDelimiter()));
                        ch.pipeline().addLast(new ByteToMessageDecoder() {
                            @Override
                            protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> list) throws Exception {
                                if (in.isReadable()) {
                                    IoTMessage message = objectMapper.readValue(new ByteBufInputStream(in), typeRefence);
                                    list.add(message);
                                }
                            }
                        });
                        ch.pipeline().addLast(getIoTMessageHandler());
                    }
                })
                .option(ChannelOption.SO_BACKLOG, 128)
                .childOption(ChannelOption.SO_KEEPALIVE, true);
        bootstrap.bind(iotServerPort);
        LOGGER.debug("Listening IoTMessages on port {}", iotServerPort);
        return bootstrap;
    }
    
    @Bean
    public WebHandler getWebHandler(ApplicationContext applicationContext) {
        return new DispatcherHandler(applicationContext);
    }
    
    @Bean(destroyMethod = "dispose")
    public NettyContext getHttpServer(ApplicationContext applicationContext) {
        HttpHandler handler = WebHttpHandlerBuilder
                .applicationContext(applicationContext)
                .build();
        ReactorHttpHandlerAdapter adapter = new ReactorHttpHandlerAdapter(handler);
        return HttpServer.create(httpServerPort).newHandler(adapter).block();
    }

    @Override
    public void configureHttpMessageCodecs(ServerCodecConfigurer configurer) {
        configurer.defaultCodecs().jackson2JsonDecoder(new Jackson2JsonDecoder());
        configurer.defaultCodecs().jackson2JsonEncoder(new Jackson2JsonEncoder());
    }
    
}
