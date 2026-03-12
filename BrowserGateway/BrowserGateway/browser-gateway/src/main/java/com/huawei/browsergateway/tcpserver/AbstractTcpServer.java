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

/**
 * TCP服务器抽象基类
 * 提供TCP/TLS服务器的通用启动和停止逻辑
 */
public abstract class AbstractTcpServer {
    private static final int CONNECTION_QUEUE_SIZE = 128;
    private static final String[] TLS_CIPHER_SUITES = {
            "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
            "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384",
            "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256",
            "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384"
    };
    private static final String[] TLS_PROTOCOLS = {"TLSv1.2", "TLSv1.3"};

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private ChannelFuture channelFuture;

    /**
     * 获取日志记录器
     */
    protected abstract Logger getLogger();

    /**
     * 获取服务器监听端口
     */
    protected abstract Integer getPort();

    /**
     * 获取服务器绑定地址
     */
    protected abstract String getAddress();

    /**
     * 获取业务处理器
     */
    protected abstract ChannelHandler getHandler();

    /**
     * 获取编码器
     */
    protected abstract ChannelHandler getEncoder();

    /**
     * 获取解码器
     */
    protected abstract ChannelHandler getDecoder();

    /**
     * 启动TCP服务器
     *
     * @param enableTls 是否启用TLS加密
     */
    public void start(boolean enableTls) {
        Integer port = getPort();
        String address = getAddress();
        String serverType = enableTls ? "TLS" : "TCP";
        getLogger().info("Starting {} server on {}:{}", serverType, address, port);

        bossGroup = new NioEventLoopGroup();
        workerGroup = new NioEventLoopGroup();

        ThreadUtil.execute(() -> startServerAsync(enableTls, address, port));
    }

    /**
     * 异步启动服务器
     */
    private void startServerAsync(boolean enableTls, String address, Integer port) {
        try {
            SslContext sslContext = createSslContext(enableTls);
            ServerBootstrap bootstrap = configureServerBootstrap(sslContext, enableTls);

            channelFuture = bootstrap.bind(address, port).sync();
            getLogger().info("TCP server started successfully on port: {}", port);
            channelFuture.channel().closeFuture().sync();
        } catch (Exception e) {
            getLogger().error("Failed to start TCP server", e);
        }
    }

    /**
     * 创建SSL上下文
     */
    private SslContext createSslContext(boolean enableTls) throws Exception {
        CertInfo certInfo = CertInfo.getInstance();

        if (!certInfo.isCertReady() || !enableTls) {
            getLogger().info("Certificate not ready or TLS disabled, starting plain TCP server");
            return null;
        }

        getLogger().info("Certificate ready, configuring TLS server");
        InputStream caStream = certInfo.Ca();
        InputStream certStream = certInfo.Device();
        InputStream keyStream = certInfo.Key();

        return SslContextBuilder.forServer(certStream, keyStream)
                .ciphers(Arrays.asList(TLS_CIPHER_SUITES))
                .protocols(TLS_PROTOCOLS)
                .trustManager(caStream)
                .build();
    }

    /**
     * 配置服务器启动器
     */
    private ServerBootstrap configureServerBootstrap(SslContext sslContext, boolean enableTls) {
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ChannelPipeline pipeline = ch.pipeline();

                        if (sslContext != null && enableTls) {
                            getLogger().info("Adding SSL handler to pipeline");
                            pipeline.addLast(sslContext.newHandler(ch.alloc()));
                        }

                        pipeline.addLast(getDecoder())
                                .addLast(getEncoder())
                                .addLast(getHandler());
                    }
                })
                .option(ChannelOption.SO_BACKLOG, CONNECTION_QUEUE_SIZE)
                .childOption(ChannelOption.SO_KEEPALIVE, true);

        return bootstrap;
    }

    /**
     * 停止TCP服务器
     */
    public void stop() {
        getLogger().info("Stopping TCP server");

        if (channelFuture != null) {
            try {
                channelFuture.channel().close().sync();
            } catch (InterruptedException e) {
                getLogger().warn("TCP server stop interrupted", e);
                Thread.currentThread().interrupt();
            }
        }

        shutdownEventLoopGroups();

        getLogger().info("TCP server stopped successfully");
    }

    /**
     * 优雅关闭事件循环组
     */
    private void shutdownEventLoopGroups() {
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
    }
}
