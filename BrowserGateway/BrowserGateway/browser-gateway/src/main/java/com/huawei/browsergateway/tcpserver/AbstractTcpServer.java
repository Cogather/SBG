package com.huawei.browsergateway.tcpserver;

import cn.hutool.core.thread.ThreadUtil;
import com.huawei.browsergateway.tcpserver.cert.CertInfo;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import org.apache.logging.log4j.Logger;

import java.io.InputStream;
import java.util.Arrays;

public abstract class AbstractTcpServer {
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private ChannelFuture channelFuture;

    protected abstract Logger getLogger();

    protected abstract Integer getPort();
    protected abstract String getAddress();

    protected abstract ChannelHandler getHandler();

    protected abstract ChannelHandler getEncoder();

    protected abstract ChannelHandler getDecoder();


    public void start(boolean isTLS) {
        Integer port = getPort();
        String address = getAddress();
        getLogger().info("start {} server, port: {}", isTLS? "TLS": "TCP", port);

        // 创建两个事件循环组，bossGroup用于接收客户端连接，workerGroup用于处理客户端数据
        bossGroup = new NioEventLoopGroup();
        workerGroup = new NioEventLoopGroup();

        ThreadUtil.execute(() -> {
            try {
                SslContext sslCtx;
                CertInfo certInstance = CertInfo.getInstance();
                if (certInstance.isCertReady() && isTLS) {
                    getLogger().info("ca and device cert is ready, start Tls server");
                    InputStream caInfo = certInstance.Ca();
                    InputStream certInfo = certInstance.Device();
                    InputStream keyInfo = certInstance.Key();
                    sslCtx = SslContextBuilder.forServer(certInfo, keyInfo).
                            ciphers(Arrays.asList(
                                    "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
                                    "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384",
                                    "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256",
                                    "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384"
                            )).protocols("TLSv1.2", "TLSv1.3").trustManager(caInfo).build();
                } else {
                    getLogger().info("ca and device cert is null or not tls server, start Tlv server");
                    sslCtx = null;
                }
                // 服务器启动辅助类
                ServerBootstrap bootstrap = new ServerBootstrap();
                bootstrap.group(bossGroup, workerGroup)
                        .channel(NioServerSocketChannel.class)  // 使用NIO的服务器通道
                        .childHandler(new ChannelInitializer<SocketChannel>() {  // 客户端连接处理器
                            @Override
                            protected void initChannel(SocketChannel ch) {
                                if (certInstance.isCertReady() && isTLS) {
                                    getLogger().info("ca and device cert is ready, add sslContext handler");
                                    ch.pipeline().addLast(sslCtx.newHandler(ch.alloc()));
                                }
                                // 添加自定义处理器
                                ch.pipeline().
                                        addLast(getDecoder()).
                                        addLast(getEncoder()).
                                        addLast(getHandler());
                            }
                        })
                        .option(ChannelOption.SO_BACKLOG, 128)  // 连接队列大小
                        .childOption(ChannelOption.SO_KEEPALIVE, true);  // 保持连接

                // 绑定端口并启动服务器
                channelFuture = bootstrap.bind(address, port).sync();
                getLogger().info(String.format("tcp started on port(s): %d", port));
                channelFuture.channel().closeFuture().sync();
            } catch (Exception e) {
                getLogger().error("start tcp server error", e);
            }
        });
    }

    public void stop() {
        getLogger().info("stop tcp server");

        if (channelFuture != null) {
            try {
                // 等待服务器通道关闭
                channelFuture.channel().close().sync();
            } catch (InterruptedException e) {
                getLogger().warn("stop tcp server interrupted", e);
                Thread.currentThread().interrupt();
            }
        }

        // 优雅关闭事件循环组
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }

        getLogger().info("tcp server stopped");
    }
}