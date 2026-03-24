package com.huawei.mobile;

import cn.hutool.core.lang.TypeReference;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.huawei.mobile.common.ID;
import com.huawei.mobile.common.R;
import com.huawei.mobile.common.Type;
import com.huawei.mobile.dto.ClientEvent;
import com.huawei.mobile.dto.DeviceLoginRequest;
import com.huawei.mobile.dto.DeviceLoginResponse;
import com.huawei.mobile.dto.UseTimesEvent;
import com.huawei.mobile.encode.TlvData;
import com.huawei.mobile.encode.TlvDecoder;
import com.huawei.mobile.encode.TlvEncoder;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.yeauty.pojo.Session;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class BrowserContext {
    private static final Log log = LogFactory.get();
    private String gidsAddr = "http://127.0.0.1:9090";
    private static final Integer default_play_mode = 1;
    private DeviceLoginRequest request = DeviceLoginRequest.newInstance();
    private DeviceLoginResponse response;

    private final ControlChannelHandler channelHandler = new ControlChannelHandler(this);
    private final MediaChannelHandler mediaHandler = new MediaChannelHandler(this);

    private AtomicReference<String> mediaAddr = new AtomicReference<>();

    private volatile Channel controlChannel;
    private volatile Channel mediaChannel;

    private volatile Session session;

    private volatile boolean isLogin;

//    private volatile FileOutputStream outputStream;

    public DeviceLoginRequest getRequest() {
        return request;
    }

    public void setSession(Session session) {
        this.session = session;
    }

    public void setGidsAddr(String gidsAddr) {
        this.gidsAddr = gidsAddr;
    }

    public void callbackVideo(byte[] data) throws IOException {
//        if (data[0] == 2) {
//            outputStream.write(data, 1, data.length -1);
//            outputStream.flush();
//        }
        session.sendBinary(data);
    }

    public void callbackMessage(Object obj) {
        String jsonStr = JSONUtil.toJsonStr(obj);
        session.sendText(jsonStr);
    }

    public void callbackMediaAddr(String addr) {
        log.info("receive media tcp addr: {}", addr);
        this.mediaAddr.set(addr);
        mediaLogin();
    }


    public void close() throws IOException {
//        if (outputStream != null) {
//            outputStream.close();
//        }
        if (controlChannel != null) {
            controlChannel.close();
        }
        if (mediaChannel != null) {
            mediaChannel.close();
        }
    }

    public void confirmInputHandle(int ut ,String s) throws InterruptedException {
        TlvData<Object> tlvData = new TlvData<>();
        tlvData.put(ID.TYPE, Type.MESSAGE);
        tlvData.put(ID.UPLOAD_TYPE, ut);
        tlvData.put(ID.CONTENT, s);
        tlvData.put(ID.SESSION_ID, this.request.getSessionId());
        channelHandler.send(controlChannel, tlvData, Type.MESSAGE);
    }

    public void handleUploadFile(String fa) throws InterruptedException {
        TlvData<Object> tlvData = new TlvData<>();
        tlvData.put(ID.TYPE, Type.UPLOAD_FILE);
        tlvData.put(ID.UPLOAD_FILE_RESULT, 0); // 0代表成功
        tlvData.put(ID.UPLOAD_FILE_TYPE, 1); // 1为默认值
        tlvData.put(ID.FILE_ADDR, fa);
        channelHandler.send(controlChannel, tlvData, Type.UPLOAD_FILE);
    }

    public void handleDirection(int ct, int cv) throws InterruptedException {
        TlvData<Object> tlvData = new TlvData<>();
        tlvData.put(ID.TYPE, Type.CONTROL);
        tlvData.put(ID.CTRL_TYPE, ct);
        tlvData.put(ID.CTRL_VAL, cv);
        tlvData.put(ID.SESSION_ID, this.request.getSessionId());
        channelHandler.send(controlChannel, tlvData, Type.CONTROL);
    }

    public void deviceLogin() throws FileNotFoundException {
//        outputStream = new FileOutputStream("C:\\Users\\w00607172\\Desktop\\ppp\\test.mp3");
        String reqStr = JSONUtil.toJsonStr(this.request);
        log.info("begin to device login {}", reqStr);

        String respStr = HttpUtil.post(this.gidsAddr + "/app-api/devicetcp/app/login/v1/gridLoginAuth", reqStr);
        R<DeviceLoginResponse> resp = JSONUtil.toBean(respStr, new TypeReference<>() {
        }, false);
        if (resp.getCode() != 200) {
            log.error("failed to device login {}", respStr);
            throw new RuntimeException(resp.getMsg());
        }

        respStr = HttpUtil.post(this.gidsAddr + "/app-api/devicetcp/app/login/v1/gridLoginAuthOpenBrowser", reqStr);
        resp = JSONUtil.toBean(respStr, new TypeReference<>() {
        }, false);
        if (resp.getCode() != 200) {
            log.error("failed to device login {}", respStr);
            throw new RuntimeException(resp.getMsg());
        }

        respStr = HttpUtil.post(this.gidsAddr + "/app-api/devicetcp/app/login/v1/deviceLoginAuth", reqStr);
        resp = JSONUtil.toBean(respStr, new TypeReference<>() {
        }, false);
        if (resp.getCode() != 200) {
            log.error("failed to device login {}", respStr);
            throw new RuntimeException(resp.getMsg());
        }
        log.info("success to device login {}", respStr);
        this.response = resp.getData();
        controlLogin();
    }

    private void mediaLogin() {
        log.info("begin to media login :{}", this.mediaAddr.get());
        String rawAddr = this.mediaAddr.get();
        if (rawAddr.startsWith("http://")) {
            rawAddr = rawAddr.substring("http://".length());
        } else if (rawAddr.startsWith("https://")) {
            rawAddr = rawAddr.substring("https://".length());
        }
        List<String> parts = StrUtil.split(rawAddr, ":");
        if (parts.size() != 2) {
            throw new RuntimeException(String.format("MEDIA TCP ADDR Error: %s", this.mediaAddr.get()));
        }
        String host = parts.get(0);
        int port = Integer.parseInt(parts.get(1));
        ThreadUtil.execute(() -> {
            EventLoopGroup group = new NioEventLoopGroup();
            try {
                Bootstrap bootstrap = new Bootstrap()
                        .group(group)
                        .channel(NioSocketChannel.class)
                        .handler(new ChannelInitializer<SocketChannel>() {
                            @Override
                            public void initChannel(SocketChannel ch) {
                                ChannelPipeline pipeline = ch.pipeline();
                                pipeline.addLast(new TlvDecoder(3145728));
                                pipeline.addLast(new TlvEncoder());
                                pipeline.addLast(mediaHandler);
                            }
                        });

                // 连接到服务器
                ChannelFuture future = bootstrap.connect(host, port).sync();
                log.info("success to connect edge media, begin to send login message");
                // 保存channel引用
                mediaChannel = future.channel();
                mediaChannel.writeAndFlush(getLoginData());
                // 等待连接关闭
                future.channel().closeFuture().sync();
            } catch (RuntimeException e) {
                log.error(e);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } finally {
                group.shutdownGracefully();
            }
        });

    }


    public void controlLogin() {
        if (isLogin) {
            log.info("has login");
            return;
        }
        isLogin = true;
        log.info("begin to control login :{}", this.response.getTcpAddr());
        List<String> parts = StrUtil.split(this.response.getTcpAddr(), ":");
        if (parts.size() != 2) {
            throw new RuntimeException(String.format("TCP ADDR Error: %s", this.response.getTcpAddr()));
        }
        String host = parts.get(0);
        int port = Integer.parseInt(parts.get(1));

        ThreadUtil.execute(() -> {
            EventLoopGroup group = new NioEventLoopGroup();
            try {
                Bootstrap bootstrap = new Bootstrap()
                        .group(group)
                        .channel(NioSocketChannel.class)
                        .handler(new ChannelInitializer<SocketChannel>() {
                            @Override
                            public void initChannel(SocketChannel ch) {
                                ChannelPipeline pipeline = ch.pipeline();
                                pipeline.addLast(new TlvDecoder(3145728));
                                pipeline.addLast(new TlvEncoder());
                                pipeline.addLast(channelHandler);
                            }
                        });

                // 连接到服务器
                ChannelFuture future = bootstrap.connect(host, port).sync();
                // 保存channel引用
                controlChannel = future.channel();
                log.info("success to connect edge control, begin to send login message");
                channelHandler.send(controlChannel, getLoginData(), Type.LOGIN);
                // 等待连接关闭
                future.channel().closeFuture().sync();
            } catch (RuntimeException e) {
                log.error(e);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } finally {
                group.shutdownGracefully();
            }
        });
    }

    private TlvData<Object> getLoginData() {
        TlvData<Object> tlvData = new TlvData<>();
        tlvData.put(ID.TYPE, Type.LOGIN);
        tlvData.put(ID.FACTORY, this.request.getManufacturer());
        tlvData.put(ID.DEV_TYPE, this.request.getModel());
        tlvData.put(ID.IMSI, this.request.getImsi());
        tlvData.put(ID.IMEI, this.request.getImei());
        tlvData.put(ID.LCD_WIDTH, Integer.parseInt(this.request.getWidth()));
        tlvData.put(ID.LCD_HEIGHT, Integer.parseInt(this.request.getHeight()));
        tlvData.put(ID.APP_TYPE, Integer.parseInt(this.request.getAppType()));
        tlvData.put(ID.TOKEN, this.response.getToken());
        tlvData.put(ID.APP_ID, Integer.parseInt(this.request.getAppType()));
        tlvData.put(ID.EXT_TYPE, this.request.getExtendModel());
        tlvData.put(ID.PLAT_TYPE, Integer.parseInt(this.request.getPlatform()));
        tlvData.put(ID.PLAY_MODE, default_play_mode);
        tlvData.put(ID.ABILITY, 1);
        tlvData.put(ID.DEVICE_TYPE, Integer.parseInt(this.request.getDeviceType()));
        tlvData.put(ID.CLIENT_LANGUAGE, this.request.getClientLanguage());
        tlvData.put(ID.AUD_TYPE, "mp3");
        tlvData.put(ID.AUD_SMPRATE, 46000);
        tlvData.put(ID.AUD_CHANNEL, 1);
        tlvData.put(ID.NETWORK_TYPE, 1);
        tlvData.put(ID.URL_TYPE, "1");
        return tlvData;
    }

    public void sendUseTime() {
        UseTimesEvent ue = new UseTimesEvent();
        ue.setHsman(this.request.getManufacturer());
        ue.setHstype(this.request.getModel());
        ue.setAppType(this.request.getAppType());
        ue.setAppId(this.request.getAppType());
        ue.setImei(this.request.getImei());
        ue.setImsi(this.request.getImsi());
        ue.setUseTimes(100000L);
        ue.setScwidth(Integer.parseInt(this.request.getWidth()));
        ue.setScheight(Integer.parseInt(this.request.getHeight()));
        ue.setExttype(this.request.getExtendModel());
        ue.setPlayMode(default_play_mode);


        String reqStr = JSONUtil.toJsonStr(ue);
        String respStr = HttpRequest.post(this.gidsAddr + "/app-api/center/public/client/sendAppUseTimesEvent").
                contentType("application/octet-stream").
                body(reqStr).
                execute().
                body();
        R<Object> resp = JSONUtil.toBean(respStr, new TypeReference<>() {
        }, false);
        if (resp.getCode() != 0) {
            log.error("failed to send error {}", respStr);
            throw new RuntimeException(resp.getMsg());
        }
    }

    public void sendError() {
        ClientEvent ce = new ClientEvent();
        ce.setHsman(this.request.getManufacturer());
        ce.setHstype(this.request.getModel());
        ce.setAppType(this.request.getAppType());
        ce.setImei(this.request.getImei());
        ce.setImsi(this.request.getImsi());
        ce.setType(1);

        String reqStr = JSONUtil.toJsonStr(ce);
        String respStr = HttpUtil.post(this.gidsAddr + "/app-api/center/public/client/sendClientEvent", reqStr);
        R<Object> resp = JSONUtil.toBean(respStr, new TypeReference<>() {
        }, false);
        if (resp.getCode() != 0) {
            log.error("failed to send error {}", respStr);
            throw new RuntimeException(resp.getMsg());
        }
        log.info("send error success {}", ce.getType());


        ce.setType(2);
        reqStr = JSONUtil.toJsonStr(ce);
        respStr = HttpUtil.post(this.gidsAddr + "/app-api/center/public/client/sendClientEvent", reqStr);
        resp = JSONUtil.toBean(respStr, new TypeReference<>() {
        }, false);
        if (resp.getCode() != 0) {
            log.error("failed to send error {}", respStr);
            throw new RuntimeException(resp.getMsg());
        }

        log.info("send error success {}", ce.getType());
    }
}
