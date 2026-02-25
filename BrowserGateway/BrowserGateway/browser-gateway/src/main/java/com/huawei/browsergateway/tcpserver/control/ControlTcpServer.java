package com.huawei.browsergateway.tcpserver.control;

import com.huawei.browsergateway.tcpserver.cert.CertInfo;
import com.huawei.browsergateway.tcpserver.common.TlvDecoder;
import com.huawei.browsergateway.tcpserver.common.TlvEncoder;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.net.ssl.SSLException;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

/**
 * TCP控制流服务器
 * 使用Netty实现，支持TLS加密
 */
@Component
public class ControlTcpServer {
    private static final Logger log = LoggerFactory.getLogger(ControlTcpServer.class);
    
    @Value("${browsergw.tcp.control-port:18601}")
    private int controlPort;
    
    @Value("${server.address:0.0.0.0}")
    private String serverAddress;
    
    @Autowired
    private ControlMessageHandler messageHandler;
    
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;
    private SslContext sslContext;
    
    @PostConstruct
    public void startServer() {
        startServer(false);
    }
    
    /**
     * 启动服务器
     * 
     * @param useTls 是否使用TLS加密
     */
    public void startServer(boolean useTls) {
        if (serverChannel != null && serverChannel.isActive()) {
            log.warn("控制流TCP服务器已在运行");
            return;
        }
        
        try {
            // 初始化SSL上下文（如果需要）
            if (useTls) {
                initSslContext();
            }
            
            // 创建EventLoopGroup
            bossGroup = new NioEventLoopGroup(1);
            workerGroup = new NioEventLoopGroup();
            
            // 创建ServerBootstrap
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ChannelPipeline pipeline = ch.pipeline();
                            
                            // 添加SSL处理器（如果需要）
                            if (useTls && sslContext != null) {
                                pipeline.addLast("ssl", sslContext.newHandler(ch.alloc()));
                            }
                            
                            // 添加TLV编解码器
                            pipeline.addLast("decoder", new TlvDecoder());
                            pipeline.addLast("encoder", new TlvEncoder());
                            
                            // 添加消息处理器
                            pipeline.addLast("handler", messageHandler);
                        }
                    });
            
            // 绑定端口并启动服务器
            ChannelFuture future = bootstrap.bind(serverAddress, controlPort).sync();
            serverChannel = future.channel();
            
            log.info("控制流TCP服务器启动成功: address={}, port={}, tls={}", 
                    serverAddress, controlPort, useTls);
            
        } catch (Exception e) {
            log.error("控制流TCP服务器启动失败", e);
            throw new RuntimeException("控制流TCP服务器启动失败", e);
        }
    }
    
    /**
     * 初始化SSL上下文
     * 从CertInfo单例获取证书信息，支持证书热更新
     */
    private void initSslContext() {
        try {
            CertInfo certInfo = CertInfo.getInstance();
            if (!certInfo.isReady()) {
                log.warn("证书未就绪，无法启用TLS");
                return;
            }
            
            String caCert = certInfo.getCaContent();
            String deviceCert = certInfo.getDeviceContent();
            String privateKey = certInfo.getPrivateKey();
            
            if (caCert == null || caCert.isEmpty() || 
                deviceCert == null || deviceCert.isEmpty() || 
                privateKey == null || privateKey.isEmpty()) {
                log.warn("证书信息不完整，无法启用TLS");
                return;
            }
            
            // 构建SSL上下文
            sslContext = SslContextBuilder.forServer(
                    new ByteArrayInputStream(deviceCert.getBytes(StandardCharsets.UTF_8)),
                    new ByteArrayInputStream(privateKey.getBytes(StandardCharsets.UTF_8))
            ).trustManager(new ByteArrayInputStream(caCert.getBytes(StandardCharsets.UTF_8)))
             .build();
            
            log.info("控制流SSL上下文初始化成功，证书更新时间: {}", certInfo.getLastUpdateTime());
            
        } catch (SSLException e) {
            log.error("控制流SSL上下文初始化失败", e);
            throw new RuntimeException("控制流SSL上下文初始化失败", e);
        }
    }
    
    /**
     * 停止服务器
     */
    @PreDestroy
    public void stopServer() {
        if (serverChannel != null) {
            try {
                serverChannel.close().sync();
                log.info("控制流TCP服务器已关闭");
            } catch (InterruptedException e) {
                log.error("关闭控制流TCP服务器失败", e);
                Thread.currentThread().interrupt();
            } finally {
                serverChannel = null;
            }
        }
        
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
            workerGroup = null;
        }
        
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
            bossGroup = null;
        }
        
        // 清空SSL上下文，以便在重启时重新初始化
        sslContext = null;
        log.debug("控制流TCP服务器资源已清理");
    }
    
    /**
     * 重启服务器（用于证书更新后）
     */
    public void restartServer(boolean useTls) {
        log.info("重启控制流TCP服务器");
        stopServer();
        try {
            Thread.sleep(1000); // 等待1秒
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        startServer(useTls);
    }
}
