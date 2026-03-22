package com.huawei.mobile;

import cn.hutool.core.thread.ThreadUtil;
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
    private Channel controlChannel;

    private Session session;
    private Bootstrap bootstrap;
    private EventLoopGroup group;

    private ScheduledThreadPoolExecutor heartbeatExecutor;
    private ScheduledFuture<?> heartbeatFuture;

    public String getGidsAddr() { return gidsAddr; }
    public void setGidsAddr(String gidsAddr) { this.gidsAddr = gidsAddr; }
    public DeviceLoginRequest getRequest() { return request; }
    public void setRequest(DeviceLoginRequest request) { this.request = request; }
    public DeviceLoginResponse getResponse() { return response; }
    public void setResponse(DeviceLoginResponse response) { this.response = response; }
    public ControlChannelHandler getChannelHandler() { return channelHandler; }
    public MediaChannelHandler getMediaHandler() { return mediaHandler; }
    public AtomicReference<String> getMediaAddr() { return mediaAddr; }
    public void setMediaAddr(AtomicReference<String> mediaAddr) { this.mediaAddr = mediaAddr; }
    public Session getSession() { return session; }
    public void setSession(Session session) { this.session = session; }
    public Bootstrap getBootstrap() { return bootstrap; }
    public void setBootstrap(Bootstrap bootstrap) { this.bootstrap = bootstrap; }
    public EventLoopGroup getGroup() { return group; }
    public void setGroup(EventLoopGroup group) { this.group = group; }
    public Channel getControlChannel() { return controlChannel; }
    public void setControlChannel(Channel controlChannel) { this.controlChannel = controlChannel; }

    public void deviceLogin() {
        R<DeviceLoginResponse> result = doDeviceLogin();
        if (result.getCode() == 0) {
            this.response = result.getData();
            bootstrap = new Bootstrap();
            group = new NioEventLoopGroup();
            bootstrap.group(group)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ch.pipeline().addLast(new TlvDecoder());
                            ch.pipeline().addLast(new TlvEncoder());
                            ch.pipeline().addLast(channelHandler);
                        }
                    });
            doConnectControlChannel();
        } else {
            log.error("login fail {}, reason {}", result.getCode(), result.getMsg());
            R<Void> err = new R<>();
            err.setCode(result.getCode());
            err.setMsg(result.getMsg());
            session.sendText(JSONUtil.toJsonStr(err));
            close();
        }
    }

    @SuppressWarnings("unchecked")
    private R<DeviceLoginResponse> doDeviceLogin() {
        try {
            String body = JSONUtil.toJsonStr(request);
            // Step 1
            String resp1 = HttpUtil.createPost(gidsAddr + "/app-api/devicetcp/app/login/v1/gridLoginAuth")
                    .contentType("application/json")
                    .body(body)
                    .execute().body();
            R<DeviceLoginResponse> r1 = JSONUtil.toBean(resp1,
                    new cn.hutool.core.lang.TypeReference<R<DeviceLoginResponse>>(){}, false);
            if (r1.getCode() != 200 && r1.getCode() != 0) {
                R<DeviceLoginResponse> err = new R<>();
                err.setCode(r1.getCode());
                err.setMsg("step1 fail: " + r1.getMsg());
                return err;
            }
            // Step 2
            String resp2 = HttpUtil.createPost(gidsAddr + "/app-api/devicetcp/app/login/v1/gridLoginAuthOpenBrowser")
                    .contentType("application/json")
                    .body(body)
                    .execute().body();
            R<DeviceLoginResponse> r2 = JSONUtil.toBean(resp2,
                    new cn.hutool.core.lang.TypeReference<R<DeviceLoginResponse>>(){}, false);
            if (r2.getCode() != 200 && r2.getCode() != 0) {
                R<DeviceLoginResponse> err = new R<>();
                err.setCode(r2.getCode());
                err.setMsg("step2 fail: " + r2.getMsg());
                return err;
            }
            // Step 3
            String resp3 = HttpUtil.createPost(gidsAddr + "/app-api/devicetcp/app/login/v1/deviceLoginAuth")
                    .contentType("application/json")
                    .body(body)
                    .execute().body();
            R<DeviceLoginResponse> r3 = JSONUtil.toBean(resp3,
                    new cn.hutool.core.lang.TypeReference<R<DeviceLoginResponse>>(){}, false);
            if (r3.getCode() != 200 && r3.getCode() != 0) {
                R<DeviceLoginResponse> err = new R<>();
                err.setCode(r3.getCode());
                err.setMsg("step3 fail: " + r3.getMsg());
                return err;
            }
            R<DeviceLoginResponse> ok = new R<>();
            ok.setCode(0);
            ok.setData(r3.getData());
            ok.setMsg("success");
            return ok;
        } catch (Exception e) {
            log.error("doDeviceLogin error", e);
            R<DeviceLoginResponse> err = new R<>();
            err.setCode(500);
            err.setMsg("login fail: " + e.getMessage());
            return err;
        }
    }

    public void doConnectControlChannel() {
        ChannelFuture future = bootstrap.connect(response.getTcpAddr().split(":")[0],
                Integer.parseInt(response.getTcpAddr().split(":")[1]));
        future.addListener((ChannelFutureListener) futureListener -> {
            if (futureListener.isSuccess()) {
                controlChannel = futureListener.channel();
                loginToDevice(controlChannel);
                String addr = response.getTcpAddr();
                mediaAddr.set(addr);
                ThreadUtil.execute(() -> doConnectMediaChannel(
                        addr.split(":")[0],
                        Integer.parseInt(addr.split(":")[1])));
                startHeartbeat();
            } else {
                log.error("control channel connect fail");
                close();
            }
        });
    }

    private void loginToDevice(Channel channel) {
        TlvData<Object> data = new TlvData<>();
        data.put(ID.TYPE, Type.LOGIN);
        data.put(ID.FACTORY, request.getManufacturer());
        data.put(ID.DEV_TYPE, request.getModel());
        data.put(ID.IMSI, request.getImsi());
        data.put(ID.IMEI, request.getImei());
        data.put(ID.LCD_WIDTH, Integer.parseInt(request.getWidth()));
        data.put(ID.LCD_HEIGHT, Integer.parseInt(request.getHeight()));
        data.put(ID.APP_TYPE, Integer.parseInt(request.getAppType()));
        data.put(ID.TOKEN, response.getToken());
        data.put(ID.SESSION_ID, request.getSessionId());
        data.put(ID.APP_ID, Integer.parseInt(request.getAppType()));
        data.put(ID.EXT_TYPE, request.getExtendModel());
        data.put(ID.PLAT_TYPE, Integer.parseInt(request.getPlatform()));
        data.put(ID.PLAY_MODE, default_play_mode);
        data.put(ID.ABILITY, 1);
        data.put(ID.DEVICE_TYPE, Integer.parseInt(request.getDeviceType()));
        data.put(ID.CLIENT_LANGUAGE, request.getClientLanguage());
        data.put(ID.AUD_TYPE, "mp3");
        data.put(ID.AUD_SMPRATE, 46000);
        data.put(ID.AUD_CHANNEL, 1);
        data.put(ID.NETWORK_TYPE, 1);
        data.put(ID.URL_TYPE, "1");
        channel.writeAndFlush(data);
    }

    public void doConnectMediaChannel(String ip, int port) {
        mediaHandler.connect(ip, port);
    }

    private void startHeartbeat() {
        heartbeatExecutor = new ScheduledThreadPoolExecutor(1);
        heartbeatFuture = heartbeatExecutor.scheduleAtFixedRate(() -> {
            if (controlChannel != null && controlChannel.isActive()) {
                TlvData<Object> hb = new TlvData<>();
                hb.put(ID.TYPE, Type.HEARTBEATS);
                hb.put(ID.SEQ, System.currentTimeMillis());
                controlChannel.writeAndFlush(hb);
            }
        }, 15, 15, TimeUnit.SECONDS);
    }

    public void close() {
        if (heartbeatFuture != null) {
            heartbeatFuture.cancel(true);
        }
        if (heartbeatExecutor != null) {
            heartbeatExecutor.shutdownNow();
        }
        if (group != null) {
            group.shutdownGracefully();
        }
        mediaHandler.close();
        if (session != null) {
            session.close();
        }
    }

    public void handleDirection(int ct, int cv) {
        if (controlChannel == null || !controlChannel.isActive()) return;
        TlvData<Object> data = new TlvData<>();
        data.put(ID.TYPE, Type.CONTROL);
        data.put(ID.CTRL_TYPE, ct);
        data.put(ID.CTRL_VAL, cv);
        data.put(ID.SESSION_ID, request.getSessionId());
        controlChannel.writeAndFlush(data);
    }

    public void confirmInputHandle(int ut, String content) {
        if (controlChannel == null || !controlChannel.isActive()) return;
        TlvData<Object> data = new TlvData<>();
        data.put(ID.TYPE, Type.MESSAGE);
        data.put(ID.UPLOAD_TYPE, ut);
        data.put(ID.CONTENT, content);
        data.put(ID.SESSION_ID, request.getSessionId());
        controlChannel.writeAndFlush(data);
    }

    public void handleUploadFile(String fa) {
        if (controlChannel == null || !controlChannel.isActive()) return;
        TlvData<Object> data = new TlvData<>();
        data.put(ID.TYPE, Type.UPLOAD_FILE);
        data.put(ID.FILE_ADDR, fa);
        data.put(ID.SESSION_ID, request.getSessionId());
        controlChannel.writeAndFlush(data);
    }

    public void sendError() {
        ThreadUtil.execute(() -> {
            try {
                ClientEvent event = new ClientEvent();
                event.setHsman(request.getManufacturer());
                event.setHstype(request.getModel());
                event.setAppType(request.getAppType());
                event.setImei(request.getImei());
                event.setImsi(request.getImsi());
                // type 1 = busy
                event.setType(1);
                HttpUtil.createPost(gidsAddr + "/app-api/center/public/client/sendClientEvent")
                        .contentType("application/json")
                        .body(JSONUtil.toJsonStr(event))
                        .execute();
                // type 2 = error
                event.setType(2);
                HttpUtil.createPost(gidsAddr + "/app-api/center/public/client/sendClientEvent")
                        .contentType("application/json")
                        .body(JSONUtil.toJsonStr(event))
                        .execute();
            } catch (Exception e) {
                log.error("sendError fail", e);
            }
        });
    }

    public void sendUseTime() {
        ThreadUtil.execute(() -> {
            try {
                UseTimesEvent event = new UseTimesEvent();
                event.setUseTimes(100000L);
                event.setHsman(request.getManufacturer());
                event.setHstype(request.getModel());
                event.setAppType(request.getAppType());
                event.setAppId(request.getAppType());
                event.setScheight(Integer.parseInt(request.getHeight()));
                event.setScwidth(Integer.parseInt(request.getWidth()));
                event.setExttype(request.getExtendModel());
                event.setImei(request.getImei());
                event.setImsi(request.getImsi());
                event.setPlayMode(default_play_mode);
                HttpUtil.createPost(gidsAddr + "/app-api/center/public/client/sendAppUseTimesEvent")
                        .contentType("application/octet-stream")
                        .body(JSONUtil.toJsonStr(event))
                        .execute();
            } catch (Exception e) {
                log.error("sendUseTime fail", e);
            }
        });
    }
}
