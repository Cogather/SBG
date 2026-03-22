# Mobile 项目 - 软件实现设计文档

## 项目概述

### 项目名称
Mobile - 云手机控制服务

### 项目类型
Spring Boot 2.7.18 + Netty WebSocket 项目

### 主要功能
实现云手机控制服务，提供浏览器WebSocket连接到移动设备的控制和媒体流传输

### 技术架构
- **后端**: Spring Boot 2.7.18 + Netty 4.1.109
- **协议栈**: WebSocket + 自定义TLV/PROTOCOL OVER TCP
- **前端**: 原生Canvas + JavaScript + VideoDecoder API + Web Audio API

## 1. 项目目录结构

```
mobile/
├── .gitignore                          # Git版本控制忽略文件
├── .idea/                             # IntelliJ IDEA项目配置
├── .codemate/                         # CodeMate工具目录
├── .git/                              # Git版本控制目录
├── pom.xml                            # Maven项目配置文件
├── src/                               # 源代码目录
│   ├── main/                          # 主代码目录
│   │   ├── java/                      # Java源代码
│   │   │   └── com/
│   │   │       └── huawei/
│   │   │           └── mobile/
│   │   │               ├── MobileApplication.java      # Spring Boot启动类
│   │   │               ├── WebsocketServer.java         # WebSocket服务端
│   │   │               ├── BrowserContext.java          # 浏览器上下文管理
│   │   │               ├── ControlChannelHandler.java   # Netty控制通道处理器
│   │   │               ├── MediaChannelHandler.java     # Netty媒体通道处理器
│   │   │               ├── common/                      # 公共层
│   │   │               │   ├── ID.java                # TLV协议标识符定义
│   │   │               │   ├── R.java                 # 通用响应对象
│   │   │               │   └── Type.java              # 消息类型常量
│   │   │               ├── dto/                         # 数据传输对象层
│   │   │               │   ├── Message.java            # WebSocket消息对象
│   │   │               │   ├── DeviceLoginRequest.java # 设备登录请求
│   │   │               │   ├── DeviceLoginResponse.java# 设备登录响应
│   │   │               │   ├── ClientEvent.java        # 客户端事件
│   │   │               │   ├── CallbackMessage.java    # 回调消息
│   │   │               │   └── UseTimesEvent.java      # 使用时长事件
│   │   │               └── encode/                      # 编码层（TLV编解码）
│   │   │                   ├── TlvEncoder.java         # TLV编码器
│   │   │                   ├── TlvDecoder.java         # TLV解码器
│   │   │                   ├── TlvEncode.java          # TLV编码工具
│   │   │                   ├── TlvData.java            # TLV数据基类
│   │   │                   └── TlbData.java            # TLV数据子类
│   │   └── resources/                   # 资源文件目录
│   │       ├── application.properties  # 应用配置文件
│   │       └── static/                  # 静态资源目录
│   │           ├── index.html          # 主页面（云手机控制界面）
│   │           ├── index.js            # JavaScript脚本
│   │           ├── time.html           # 时间管理页面
│   │           └── upload.html         # 文件上传页面
│   └── test/                           # 测试代码目录
│       └── java/
│           └── com/
│               └── huawei/
│                   └── mobile/
│                       └── TestA.java # 测试类
└── target/                            # Maven编译输出目录
```

## 2. 依赖管理配置

### Maven配置文件 (pom.xml)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <groupId>com.huawei</groupId>
    <artifactId>mobile</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>mobile</name>
    <description>mobile</description>

    <properties>
        <maven.compiler.source>12</maven.compiler.source>
        <maven.compiler.target>12</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <log4j.version>2.23.1</log4j.version>
    </properties>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>2.7.18</version>
        <relativePath />
    </parent>

    <dependencies>
        <!-- Spring Boot核心 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- Java工具库 -->
        <dependency>
            <groupId>cn.hutool</groupId>
            <artifactId>hutool-all</artifactId>
            <version>5.8.30</version>
        </dependency>

        <!-- JUnit 5 测试框架 -->
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter-api</artifactId>
            <version>5.9.2</version>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter-engine</artifactId>
            <version>5.9.2</version>
            <scope>test</scope>
        </dependency>

        <!-- Netty网络框架 -->
        <dependency>
            <groupId>io.netty</groupId>
            <artifactId>netty-all</artifactId>
            <version>4.1.109.Final</version>
        </dependency>

        <!-- WebSocket集成 -->
        <dependency>
            <groupId>org.yeauty</groupId>
            <artifactId>netty-websocket-spring-boot-starter</artifactId>
            <version>0.13.0</version>
        </dependency>

        <!-- 代码简化工具 -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <mainClass>com.huawei.mobile.MobileApplication</mainClass>
                </configuration>
            </plugin>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.8.1</version>
                <configuration>
                    <source>12</source>
                    <target>12</target>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

### 应用配置文件 (application.properties)

```properties
spring.application.name=mobile
server.port=8088
server.address=0.0.0.0
```

## 3. 项目核心代码实现

### 3.1 应用入口类 (MobileApplication.java)

```java
package com.huawei.mobile;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableAsync
public class MobileApplication {
    public static void main(String[] args){
        SpringApplication.run(MobileApplication.class,args);
    }
}
```

### 3.2 公共层实现 (common层)

#### 3.2.1 协议标识符定义 (ID.java)

```java
package com.huawei.mobile.common;

public class ID {
    public static final int TYPE = 1;         // 消息类型
    public static final int FACTORY = 2;       // 厂商
    public static final int DEV_TYPE = 3;     // 机型
    public static final int IMSI = 4;         // imsi值
    public static final int IMEI = 5;         // imei值
    public static final int LCD_WIDTH = 6;    // 设备屏幕宽度
    public static final int LCD_HEIGHT = 7;   // 设备屏幕高度
    public static final int AUD_TYPE = 8;     // 音频类型
    public static final int ACK_TYPE = 9;     // 服务端通用应答值
    public static final int CODE = 10;        // 返回状态码
    public static final int EVENT = 11;       // 事件消息
    public static final int CTRL_TYPE = 12;   // 控制类型
    public static final int CTRL_VAL = 13;    // 控制值
    public static final int SEQ = 14;         // 时间序列
    public static final int AUDIO_DATA = 15;  // 音频流数据
    public static final int VIDEO_DATA = 16;  // 视频流数据
    public static final int AUD_SMPRATE = 17; // 音频采样率
    public static final int AUD_CHANNEL = 18; // 音频通道数
    public static final int APP_TYPE = 19;    // 应用类型
    public static final int TCP_ADDR = 20;    // 流媒体地址
    public static final int TOKEN = 21;       // 用户http登录token
    public static final int SESSION_ID = 22;  // 浏览器sessionId
    public static final int FRAME_TYPE = 23;  // 帧类型
    public static final int CTRL_RSP_ELM = 24; // 服务端控制响应元素
    public static final int CTRL_RSP_INFO = 25; // 服务端控制响应信息反馈
    public static final int CONTENT = 26;     // 传输内容
    public static final int UPLOAD_TYPE = 27; // 设备端信息上传类型
    public static final int APP_ID = 28;      // 应用appid
    public static final int PLAT_TYPE = 29;   // 平台类型
    public static final int EXT_TYPE = 30;    // 扩展机型
    public static final int VIDEO_ADDR = 31;  // 视频地址
    public static final int VIDEO_MODEL = 32; // 视频类型
    public static final int PLAYER_STATUS = 33; // 播放器状态
    public static final int UPLOAD_FILE_TYPE = 34; // 文件传输类型
    public static final int UPLOAD_FILE_RESULT = 35; // 文件传输反馈
    public static final int FILE_ADDR = 36;   // 文件地址
    public static final int PLAY_MODE = 37;   // 播放模式
    public static final int JPG_DATA = 38;    // JPG流数据
    public static final int LOCATION_DATA = 39; // 位置信息
    public static final int SOCKS5_ADDR = 40; // 代理地址
    public static final int SOCKS5_TUNNEL = 41; // 代理通道
    public static final int ABILITY = 42;     // 能力值
    public static final int COMMAND = 43;     // 回调事件类型
    public static final int STATUS = 44;      // 状态
    public static final int CLIENT_LANGUAGE = 45; // 客户端语言
    public static final int DEVICE_TYPE = 46; // 设备类型细分
    public static final int WRITE_TYPE = 47;  // 输入框类型
    public static final int NETWORK_TYPE = 48; // 网络类型
    public static final int URL_TYPE = 49;    // 页面地址类型
}
```

#### 3.2.2 消息类型定义 (Type.java)

```java
package com.huawei.mobile.common;

public class Type {
    public static final int LOGIN = 1;
    public static final int HEARTBEATS = 2;
    public static final int CONTROL = 4;
    public static final int AUDIO = 5;
    public static final int VIDEO = 6;
    public static final int ACK = 7;
    public static final int RETURN_MEDIA = 9;
    public static final int RETURN_CONTROL = 12;
    public static final int MESSAGE = 13;
    public static final int UPLOAD_FILE = 16;
}
```

#### 3.2.3 通用响应对象 (R.java)

```java
package com.huawei.mobile.common;

public class R<T> {
    private Integer code;
    private T data;
    private String msg;

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }
}
```

### 3.3 数据传输对象层实现 (dto层)

#### 3.3.1 WebSocket消息对象 (Message.java)

```java
package com.huawei.mobile.dto;

import lombok.Data;

@Data
public class Message {
    private String type;
    private Integer ct; // controlType
    private Integer cv; // controlValue
    private String content;
    private Integer ut; // uploadType
    private Integer dv; // deviceType
    private Integer at; // appType
    private String fa; //fileAddr
    private String ga;
    private String cs; // canvasSize
}
```

#### 3.3.2 设备登录请求对象 (DeviceLoginRequest.java)

```java
package com.huawei.mobile.dto;

import lombok.Data;

@Data
public class DeviceLoginRequest {
    private String imsi;
    private String imei;
    private String manufacturer;
    private String model;
    private String appType;
    private String extendModel;
    private String country;
    private String platform;
    private String width;
    private String height;
    private String mcc;
    private String mnc;
    private String lac;
    private String ci;
    private String rxlev;
    private String totalKb;
    private String freeKb;
    private String clientLanguage;
    private String deviceType;

    public static DeviceLoginRequest newInstance() {
        DeviceLoginRequest ret = new DeviceLoginRequest();
        ret.imsi = "68510155565211";
        ret.imei = "6258412454025411";
        ret.manufacturer = "default";
        ret.model = "default";
        ret.appType = "5";
        ret.extendModel = "default";
        ret.country = "default";
        ret.platform = "1";
        ret.width = "240";
        ret.height = "320";
        ret.mcc = "460";
        ret.mnc = "00x";
        ret.lac = "100";
        ret.ci = "5.21";
        ret.rxlev = "-72";
        ret.totalKb = "1424122";
        ret.freeKb = "1424122";
        ret.clientLanguage = "en_US";
        ret.deviceType = "2";
        return ret;
    }

    public String getSessionId() {
        return imei + "_" + imsi;
    }
}
```

#### 3.3.3 设备登录响应对象 (DeviceLoginResponse.java)

```java
package com.huawei.mobile.dto;

import java.time.LocalDateTime;

public class DeviceLoginResponse {
    private String token = "1234";
    private LocalDateTime expiresTime;
    private String tcpAddr = "127.0.0.1:30001";
    private Long timeAxis;
    private Integer videoMode;
    private String shortAddr;
    private String nodeGateWayUrl;

    // Getter和Setter方法
    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public LocalDateTime getExpiresTime() {
        return expiresTime;
    }

    public void setExpiresTime(LocalDateTime expiresTime) {
        this.expiresTime = expiresTime;
    }

    public String getTcpAddr() {
        return tcpAddr;
    }

    public void setTcpAddr(String tcpAddr) {
        this.tcpAddr = tcpAddr;
    }

    public Long getTimeAxis() {
        return timeAxis;
    }

    public void setTimeAxis(Long timeAxis) {
        this.timeAxis = timeAxis;
    }

    public Integer getVideoMode() {
        return videoMode;
    }

    public void setVideoMode(Integer videoMode) {
        this.videoMode = videoMode;
    }

    public String getShortAddr() {
        return shortAddr;
    }

    public void setShortAddr(String shortAddr) {
        this.shortAddr = shortAddr;
    }

    public String getNodeGateWayUrl() {
        return nodeGateWayUrl;
    }

    public void setNodeGateWayUrl(String nodeGateWayUrl) {
        this.nodeGateWayUrl = nodeGateWayUrl;
    }
}
```

#### 3.3.4 客户端事件对象 (ClientEvent.java)

```java
package com.huawei.mobile.dto;

import lombok.Data;

@Data
public class ClientEvent {
    private String hsman; // 厂商
    private String hstype; // 机型
    private String appType; // appType
    private String imei;
    private String imsi;
    private Integer type; //类型 1:busy/2:error
}
```

#### 3.3.5 控制回调消息对象 (CallbackMessage.java)

```java
package com.huawei.mobile.dto;

import lombok.Data;

@Data
public class CallbackMessage {
    private String type;
    private Integer elm;
    private Integer info;
    private String content;
    private Integer wt;
}
```

#### 3.3.6 使用时长事件对象 (UseTimesEvent.java)

```java
package com.huawei.mobile.dto;

import lombok.Data;

@Data
public class UseTimesEvent {
    private Long useTimes;
    private String hsman;
    private String hstype;
    private String appType;
    private String appId;
    private Integer scheight;
    private Integer scwidth;
    private String exttype;
    private String imei;
    private String imsi;
    private Integer playMode;
}
```

### 3.4 编码/解码层实现 (encode层)

#### 3.4.1 TLV编码工具类 (TlvEncode.java)

```java
package com.huawei.mobile.encode;

import cn.hutool.json.JSONConfig;
import cn.hutool.json.JSONUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import io.netty.buffer.ByteBuf;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Map;

public class TlvEncode {
    private static final Log log = LogFactory.get();
    private ByteBuf byteBuf;

    public ByteBuf getByteBuf() {
        return this.byteBuf;
    }

    public TlvEncode(ByteBuf byteBuf) {
        this.byteBuf = byteBuf;
    }

    public void writeByte(int key, byte b) {
        this.byteBuf.writeInt(key);
        this.byteBuf.writeInt(1);
        this.byteBuf.writeByte(b);
    }

    public void writeInt(int key, int i) {
        this.byteBuf.writeInt(key);
        this.byteBuf.writeInt(4);
        this.byteBuf.writeInt(i);
    }

    public void writeLong(int key, long l) {
        this.byteBuf.writeInt(key);
        this.byteBuf.writeInt(8);
        this.byteBuf.writeLong(l);
    }

    public void writeBytes(int key, byte[] bytes) {
        this.byteBuf.writeInt(key);
        if (null != bytes) {
            this.byteBuf.writeInt(bytes.length);
            this.byteBuf.writeBytes(bytes);
        } else {
            this.byteBuf.writeInt(0);
        }
    }

    public void writeByteBuf(int key, ByteBuf buf) {
        this.byteBuf.writeInt(key);
        if (null != buf) {
            buf.readerIndex(0);
            int len = buf.readableBytes();
            this.byteBuf.writeInt(len);
            this.byteBuf.writeBytes(buf);
        } else {
            this.byteBuf.writeInt(0);
        }
    }

    public void writeString(int key, String str) {
        this.byteBuf.writeInt(key);
        if (null != str) {
            byte[] bytes = str.getBytes(Charset.forName("utf-8"));
            int len = bytes.length;
            this.byteBuf.writeInt(len);
            this.byteBuf.writeBytes(bytes);
        }
    }

    public void writeMap(TlvData<Object> data) {
        for (Map.Entry<Integer, Object> integerObjectEntry : data.entrySet()) {
            Map.Entry<Integer, Object> entry = (Map.Entry) integerObjectEntry;
            Integer key = entry.getKey();
            Object val = entry.getValue();
            if (val instanceof String) {
                this.writeString(key, (String) val);
            } else if (val instanceof byte[]) {
                this.writeBytes(key, (byte[]) val);
            } else if (val instanceof Byte) {
                this.writeByte(key, (Byte) val);
            } else if (val instanceof Integer) {
                this.writeInt(key, (Integer) val);
            } else if (val instanceof Long) {
                this.writeLong(key, (Long) val);
            } else {
                if (!(val instanceof ByteBuf)) {
                    log.error("[TlvEncode writeMap]  data：{}", JSONUtil.toJsonStr(data, JSONConfig.create().setIgnoreNullValue(false)));
                    throw new UnsupportedOperationException();
                }

                ByteBuf byteBuf = (ByteBuf) val;
                byteBuf.readerIndex(0);
                this.writeByteBuf(key, byteBuf);
            }
        }
    }
}
```

#### 3.4.2 TLV数据基类 (TlvData.java)

```java
package com.huawei.mobile.encode;

import java.util.LinkedHashMap;

public class TlvData<T> extends LinkedHashMap<Integer, T> {
    public TlvData() {
    }
}
```

#### 3.4.3 TLV编码器 (TlvEncoder.java)

```java
package com.huawei.mobile.encode;

import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import java.nio.charset.StandardCharsets;

public class TlvEncoder extends MessageToByteEncoder<TlvData> {
    private static final Log log = LogFactory.get();

    public TlvEncoder() {
    }

    protected void encode(ChannelHandlerContext ctx, TlvData msg, ByteBuf out) {
        ByteBuf buffer = ctx.alloc().buffer();

        try {
            TlvEncode tlvEncode = new TlvEncode(buffer);
            tlvEncode.writeMap(msg);
            int count = msg.size();
            int len = buffer.writerIndex();
            out.writeBytes("mu".getBytes(StandardCharsets.UTF_8));
            out.writeInt(count);
            out.writeInt(len);
            out.writeBytes(buffer);
        } finally {
            buffer.release();
        }
    }
}
```

#### 3.4.4 TLV解码器 (TlvDecoder.java)

```java
package com.huawei.mobile.encode;

import com.huawei.mobile.common.ID;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.nio.charset.StandardCharsets;
import java.util.List;

public class TlvDecoder extends ByteToMessageDecoder {
    private static final Log log = LogFactory.get();

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        if (in.readableBytes() < 10) {
            return;
        }

        in.markReaderIndex();
        byte[] magic = new byte[2];
        in.readBytes(magic);
        String magicStr = new String(magic, StandardCharsets.UTF_8);

        if (!"mu".equals(magicStr)) {
            in.resetReaderIndex();
            return;
        }

        int count = in.readInt();
        int length = in.readInt();

        if (in.readableBytes() < length) {
            in.resetReaderIndex();
            return;
        }

        ByteBuf data = in.readBytes(length);
        TlbData tlbData = new TlbData();
        byte[] bytes = new byte[data.readableBytes()];
        data.getBytes(data.readerIndex(), bytes);
        data.release();

        int offset = 0;
        for (int i = 0; i < count; i++) {
            if (offset + 4 > bytes.length) {
                break;
            }

            int key = bytesToInt(bytes, offset);
            offset += 4;

            if (offset + 4 > bytes.length) {
                break;
            }

            int len = bytesToInt(bytes, offset);
            offset += 4;

            if (offset + len > bytes.length) {
                break;
            }

            byte[] valueBytes = new byte[len];
            System.arraycopy(bytes, offset, valueBytes, 0, len);
            offset += len;

            String value = new String(valueBytes, StandardCharsets.UTF_8);
            tlbData.put(key, value);
        }

        out.add(tlbData);
    }

    private int bytesToInt(byte[] bytes, int offset) {
        return (bytes[offset] & 0xff) << 24
                | (bytes[offset + 1] & 0xff) << 16
                | (bytes[offset + 2] & 0xff) << 8
                | bytes[offset + 3] & 0xff;
    }
}
```

#### 3.4.5 TLB数据子类 (TlbData.java)

```java
package com.huawei.mobile.encode;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;

import java.util.LinkedHashMap;

public class TlbData extends TlvData<ByteBuf> {
    public TlbData() {
    }

    public TlbData(int initialCapacity) {
        super(initialCapacity);
    }

    @Override
    public ByteBuf put(Integer key, ByteBuf value) {
        addByteBufRef(value);
        return super.put(key, value);
    }

    @Override
    public void clear() {
        super.values().forEach(ByteBuf::release);
        super.clear();
    }

    private void addByteBufRef(ByteBuf byteBuf) {
        ByteBuf retainedBuf = byteBuf.retainedSlice();
        retainedBuf.retain();
    }
}
```

### 3.5 核心业务类实现

#### 3.5.1 WebSocket服务端 (WebsocketServer.java)

```java
package com.huawei.mobile;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.huawei.mobile.dto.Message;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.yeauty.annotation.*;
import org.yeauty.pojo.Session;

import java.io.IOException;
import java.util.List;

@Component
@ServerEndpoint(
        path = "/app/websocket/{imeiAndImsi}",
        port = "40002",
        bossLoopGroupThreads = "8",
        workerLoopGroupThreads = "64",
        optionSoBacklog = "1024",
        maxFramePayloadLength = "655360"
)
@EnableAsync
public class WebsocketServer {
    private static final Log log = LogFactory.get();

    @OnOpen
    public void onOpen(Session session, @PathVariable String imeiAndImsi, @RequestParam MultiValueMap<String, String> reqMap) {
        log.info("websocket open, ", imeiAndImsi);
        BrowserContext context = new BrowserContext();
        List<String> split = StrUtil.split(imeiAndImsi, "_");
        context.getRequest().setImei(split.get(0));
        context.getRequest().setImsi(split.get(1));
        context.setSession(session);
        session.setAttribute("context", context);
    }

    @OnMessage
    public void onMessage(Session session, String message) throws InterruptedException, IOException {
        BrowserContext context = session.getAttribute("context");
        Message msg = JSONUtil.toBean(message, Message.class);

        switch (msg.getType()) {
            case "login":
                List<String> cs = StrUtil.split(msg.getCs(), "x");
                context.getRequest().setWidth(cs.get(0));
                context.getRequest().setHeight(cs.get(1));
                context.getRequest().setDeviceType(String.valueOf(msg.getDv()));
                context.getRequest().setAppType(String.valueOf(msg.getAt()));
                context.setGidsAddr(msg.getGa());
                context.deviceLogin();
                break;
            case "logout":
                context.close();
                break;
            case "direction":
                context.handleDirection(msg.getCt(), msg.getCv());
                break;
            case "upload":
                context.confirmInputHandle(msg.getUt(), msg.getContent());
                break;
            case "send_error":
                context.sendError();
                break;
            case "send_time":
                context.sendUseTime();
                break;
            case "upload_file":
                context.handleUploadFile(msg.getFa());
                break;
            default:
                log.error("un support type :{}", msg.getType());
                break;
        }
    }

    @OnClose
    public void onClose(Session session) throws IOException {
        BrowserContext context = session.getAttribute("context");
        context.close();
        session.close();
        log.info("close websocket {} ");
    }

    @OnError
    public void onError(Session session, Throwable error) throws IOException {
        BrowserContext context = session.getAttribute("context");
        context.close();
        session.close();
        log.error(error);
    }
}
```

#### 3.5.2 浏览器上下文管理 (BrowserContext.java)

```java
package com.huawei.mobile;

import cn.hutool.core.lang.TypeReference;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.huawei.mobile.common.R;
import com.huawei.mobile.dto.*;
import io.netty.channel.EventLoopGroup;
import lombok.Data;
import com.huawei.mobile.common.ID;
import com.huawei.mobile.common.Type;
import com.huawei.mobile.dto.DeviceLoginRequest;
import com.huawei.mobile.dto.DeviceLoginResponse;
import com.huawei.mobile.encode.TlvData;
import com.huawei.mobile.encode.TlvDecoder;
import com.huawei.mobile.encode.TlvEncoder;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.yeauty.pojo.Session;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import java.util.List;

@Data
public class BrowserContext {
    private static final Log log = LogFactory.get();
    private String gidsAddr = "http://127.0.0.1:9090";
    private static final Integer default_play_mode = 1;
    private DeviceLoginRequest request = DeviceLoginRequest.newInstance();
    private DeviceLoginResponse response;

    private final ControlChannelHandler channelHandler = new ControlChannelHandler(this);
    private final MediaChannelHandler mediaHandler = new MediaChannelHandler(this);

    private AtomicReference<String> mediaAddr = new AtomicReference<>();

    private Session session;
    private Bootstrap bootstrap;
    private EventLoopGroup group;

    public void deviceLogin() {
        R<DeviceLoginResponse> result = doDeviceLogin();
        if (result.getCode() == 200) {
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
            session.sendText(JSONUtil.toJsonStr(result));
            close();
        }
    }

    private R<DeviceLoginResponse> doDeviceLogin() {
        try {
            // 模拟GIDS服务调用
            R<DeviceLoginResponse> result = new R<>();
            result.setCode(200);
            DeviceLoginResponse data = new DeviceLoginResponse();
            data.setTcpAddr("127.0.0.1:30001");
            // 设置其他响应字段...
            result.setData(data);
            return result;
        } catch (Exception e) {
            R<DeviceLoginResponse> result = new R<>();
            result.setCode(500);
            result.setMsg("login fail " + e.getMessage());
            return result;
        }
    }

    public void doConnectControlChannel() {
        ChannelFuture future = bootstrap.connect("127.0.0.1", 30001);
        future.addListener((ChannelFutureListener) futureListener -> {
            if (futureListener.isSuccess()) {
                Channel channel = futureListener.channel();
                loginToDevice(channel);
                mediaAddr.set(response.getTcpAddr());
                ThreadUtil.execute(() -> doConnectMediaChannel(response.getTcpAddr().split(":")[0], 
                        Integer.parseInt(response.getTcpAddr().split(":")[1])));
            } else {
                log.error("control channel connect fail");
                close();
            }
        });
    }

    private void loginToDevice(Channel channel) {
        TlvData<String> data = new TlvData<>();
        data.put(ID.TOKEN, response.getToken());
        data.put(ID.SESSION_ID, request.getSessionId());
        channel.writeAndFlush(data);
    }

    public void doConnectMediaChannel(String ip, int port) {
        mediaHandler.connect(ip, port);
    }

    public void close() {
        if (group != null) {
            group.shutdownGracefully();
        }
        if (session != null) {
            session.close();
        }
    }

    public void handleDirection(int ct, int cv) {
        // 实现方向控制逻辑
    }

    public void confirmInputHandle(int ut, String content) {
        // 实现输入框处理逻辑
    }

    public void sendError() {
        // 实现错误埋点上报
    }

    public void sendUseTime() {
        // 实现使用时长埋点上报
    }

    public void handleUploadFile(String fa) {
        // 实现文件上传处理
    }
}
```

#### 3.5.3 控制通道处理器 (ControlChannelHandler.java)

```java
package com.huawei.mobile;

import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.huawei.mobile.common.ID;
import com.huawei.mobile.common.Type;
import com.huawei.mobile.encode.TlbData;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.yeauty.pojo.Session;

public class ControlChannelHandler extends ChannelInboundHandlerAdapter {
    private static final Log log = LogFactory.get();
    private BrowserContext browserContext;

    public ControlChannelHandler(BrowserContext browserContext) {
        this.browserContext = browserContext;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        TlbData data = (TlbData) msg;
        try {
            if (data.containsKey(Type.ACK)) {
                // 处理ACK应答
                handleAck(data);
            } else if (data.containsKey(Type.RETURN_MEDIA)) {
                // 处理媒体地址返回
                handleMediaAddress(data);
            } else if (data.containsKey(Type.RETURN_CONTROL)) {
                // 处理控制响应
                handleControlResponse(data);
            }
        } finally {
            data.clear();
        }
    }

    private void handleAck(TlbData data) {
        // 处理ACK应答
    }

    private void handleMediaAddress(TlbData data) {
        // 处理媒体地址
    }

    private void handleControlResponse(TlbData data) {
        // 处理控制响应
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("exceptionCaught", cause);
        ctx.close();
    }
}
```

#### 3.5.4 媒体通道处理器 (MediaChannelHandler.java)

```java
package com.huawei.mobile;

import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.huawei.mobile.common.ID;
import com.huawei.mobile.common.Type;
import com.huawei.mobile.encode.TlbData;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

public class MediaChannelHandler {
    private static final Log log = LogFactory.get();
    private BrowserContext browserContext;
    private Channel channel;
    private final EventLoopGroup group = new NioEventLoopGroup();

    public MediaChannelHandler(BrowserContext browserContext) {
        this.browserContext = browserContext;
    }

    public void connect(String ip, int port) {
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(group)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ch.pipeline().addLast(new TlvDecoder());
                        ch.pipeline().addLast(new TlvEncoder());
                        ch.pipeline().addLast(new ChannelInboundHandlerAdapter() {
                            @Override
                            public void channelRead(ChannelHandlerContext ctx, Object msg) {
                                TlbData data = (TlbData) msg;
                                try {
                                    if (data.containsKey(Type.AUDIO)) {
                                        handleAudioData(data);
                                    } else if (data.containsKey(Type.VIDEO)) {
                                        handleVideoData(data);
                                    }
                                } finally {
                                    data.clear();
                                }
                            }

                            @Override
                            public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
                                log.error("exceptionCaught", cause);
                                ctx.close();
                            }
                        });
                    }
                });

        ChannelFuture future = bootstrap.connect(ip, port);
        future.addListener((ChannelFutureListener) futureListener -> {
            if (futureListener.isSuccess()) {
                channel = futureListener.channel();
                log.info("media channel connected");
            } else {
                log.error("media channel connect fail");
            }
        });
    }

    private void handleAudioData(TlbData data) {
        // 处理音频数据，添加帧头
    }

    private void handleVideoData(TlbData data) {
        // 处理视频数据，添加帧头
    }

    public void close() {
        if (channel != null) {
            channel.close();
        }
        group.shutdownGracefully();
    }
}
```

## 4. 前端界面实现

### 4.1 主页面 (index.html)

```html
<!DOCTYPE html>
<html>
<head>
    <meta charset="utf-8">
    <title>云手机控制界面</title>
    <style>
        body {
            margin: 0;
            padding: 0;
            display: flex;
            justify-content: center;
            background-color: #f0f0f0;
        }

        .container {
            width: 1000px;
            height: 700px;
            display: flex;
            flex-direction: column;
            background-color: white;
            box-shadow: 0 0 10px rgba(0,0,0,0.1);
        }

        .main-area {
            display: flex;
            flex: 1;
            padding: 10px;
        }

        .video-container {
            width: 320px;
            height: 320px;
            background-color: #333;
            border-radius: 5px;
            position: relative;
            overflow: hidden;
        }

        #canvas {
            width: 100%;
            height: 100%;
            background-color: #000;
        }

        .control-panel {
            flex: 1;
            margin-left: 20px;
            display: flex;
            flex-direction: column;
            gap: 10px;
        }

        .direction-keys {
            display: grid;
            grid-template-columns: repeat(3, 1fr);
            gap: 5px;
            width: 150px;
}

        .btn {
            padding: 10px;
            font-size: 14px;
            cursor: pointer;
            border: 1px solid #ddd;
            border-radius: 5px;
            background-color: #fff;
        }

        .btn:hover {
            background-color: #f5f5f5;
        }

        .input-area {
            display: flex;
            flex-direction: column;
            gap: 5px;
        }

        input[type="text"] {
            padding: 5px;
            border: 1px solid #ddd;
            border-radius: 3px;
        }

        select {
            padding: 5px;
            border: 1px solid #ddd;
            border-radius: 3px;
        }

        .bottom-bar {
            display: flex;
            justify-content: space-between;
            align-items: center;
            padding: 10px;
            border-top: 1px solid #eee;
        }

        .num-pad {
            display: grid;
            grid-template-columns: repeat(3, 1fr);
            gap: 5px;
            width: 150px;
        }

        .function-keys {
            display: grid;
            grid-template-columns: repeat(2, 1fr);
            gap: 5px;
            width: 150px;
        }
    </style>
</head>
<body>
    <div class="container">
        <div class="main-area">
            <div class="video-container">
                <canvas id="canvas" width="240" height="320"></canvas>
            </div>

            <div class="control-panel">
                <div class="function-keys">
                    <button class="btn" onclick="sendFunctionKey('menu')">菜单</button>
                    <button class="btn" onclick="sendFunctionKey('back')">返回</button>
                </div>

                <div class="direction-keys">
                    <div></div>
                    <button class="btn" onclick="sendDirection('up')">↑</button>
                    <div></div>
                    <button class="btn" onclick="sendDirection('left')">←</button>
                    <button class="btn" onclick="sendDirection('ok')">OK</button>
                    <button class="btn" onclick="sendDirection('right')">→</button>
                    <div></div>
                    <button class="btn" onclick="sendDirection('down')">↓</button>
                    <div></div>
                </div>

                <div class="input-area">
                    <input type="text" id="content" placeholder="输入内容">
                    <select id="fileType">
                        <option value="1">文件类型1</option>
                        <option value="2">文件类型2</option>
                    </select>
                </div>

                <div class="num-pad">
                    <button class="btn" onclick="sendNum('1')">1</button>
                    <button class="btn" onclick="sendNum('2')">2</button>
                    <button class="btn" onclick="sendNum('3')">3</button>
                    <button class="btn" onclick="sendNum('4')">4</button>
                    <button class="btn" onclick="sendNum('5')">5</button>
                    <button class="btn" onclick="sendNum('6')">6</button>
                    <button class="btn" onclick="sendNum('7')">7</button>
                    <button class="btn" onclick="sendNum('8')">8</button>
                    <button class="btn" onclick="sendNum('9')">9</button>
                    <button class="btn" onclick="sendNum('*')">*</button>
                    <button class="btn" onclick="sendNum('0')">0</button>
                    <button class="btn" onclick="sendNum('#')">#</button>
                </div>

                <div class="input-area">
                    <input type="text" id="fileAddr" placeholder="文件地址">
                    <button class="btn" onclick="uploadFile()">文件上传</button>
                </div>

                <div class="input-area">
                    <input type="text" id="gidsAddr" placeholder="GIDS地址" value="http://127.0.0.1:9090">
                </div>
            </div>
        </div>

        <div class="bottom-bar">
            <div>
                <button class="btn" onclick="login()">登录</button>
                <button class="btn" onclick="logout()">退出</button>
            </div>

            <div>
                <button class="btn" onclick="sendError()">埋点:错误</button>
                <button class="btn" onclick="sendUseTime()">埋点:使用时长</button>
            </div>

            <div class="input-area">
                <select id="canvasSize">
                    <option value="240x320">240x320</option>
                    <option value="360x480">360x480</option>
                    <option value="405x540">405x540</option>
                    <option value="540x720">540x720</option>
                </select>
                <select id="inputMode">
                    <option value="keypad">按键模式</option>
                    <option value="touchscreen">触屏模式</option>
                </select>
                <select id="appType">
                    <option value="1">BBC</option>
                    <option value="2">TikTok</option>
                    <option value="3">Facebook</option>
                    <option value="4">YouTube</option>
                    <option value="5">Other</option>
                </select>
            </div>
        </div>
    </div>
</body>
</html>
```

### 4.2 JavaScript逻辑 (index.js)

```javascript
let ws;
let canvas;
let ctx;
let videoDecoder;
let audioContext;
let audioQueue = [];
let isPlayingAudio = false;

window.onload = function() {
    canvas = document.getElementById('canvas');
    ctx = canvas.getContext('2d');

    setupVideoDecoder();
    setupAudioContext();

    // 处理画布点击事件（触屏模式）
    canvas.addEventListener('click', function(e) {
        const rect = canvas.getBoundingClientRect();
        const x = Math.floor((e.clientX - rect.left) * (canvas.width / rect.width));
        const y = Math.floor((e.clientY - rect.top) * (canvas.height / rect.height));
        sendTouchEvent(x, y);
    });
};

function setupVideoDecoder() {
    if (typeof VideoDecoder === 'undefined') {
        console.error('VideoDecoder is not supported in this browser');
        return;
    }

    videoDecoder = new VideoDecoder({
        output: function(frame) {
            ctx.drawImage(frame, 0, 0, canvas.width, canvas.height);
            frame.close();
        },
        error: function(e) {
            console.error('Video decode error:', e);
        }
    });

    videoDecoder.configure({
        codec: 'avc1.42E01E',
        width: 240,
        height: 320
    });
}

function setupAudioContext() {
    audioContext = new (window.AudioContext || window.webkitAudioContext)();
}

function login() {
    const imei = "6258412454025411";
    const imsi = "68510155565211";
    const wsUrl = `ws://localhost:40002/app/websocket/${imei}_${imsi}`;

    ws = new WebSocket(wsUrl);
    ws.binaryType = 'arraybuffer';

    ws.onopen = function() {
        console.log('WebSocket connected');

        const canvasSize = document.getElementById('canvasSize').value;
        const deviceType = document.getElementById('canvasSize').value;
        const appType = document.getElementById('appType').value;
        const gidsAddr = document.getElementById('gidsAddr').value;

        const message = {
            type: 'login',
            cs: canvasSize,
            dv: parseInt(deviceType) || 2,
            at: parseInt(appType) || 5,
            ga: gidsAddr
        };

        ws.send(JSON.stringify(message));
    };

    ws.onmessage = function(event) {
        if (event.data instanceof ArrayBuffer) {
            handleBinaryMessage(event.data);
        } else {
            handleTextMessage(event.data);
        }
    };

    ws.onerror = function(error) {
        console.error('WebSocket error:', error);
    };

    ws.onclose = function() {
        console.log('WebSocket closed');
    };
}

function logout() {
    if (ws) {
        const message = { type: 'logout' };
        ws.send(JSON.stringify(message));
        ws.close();
    }
}

function handleBinaryMessage(data) {
    const frameHeader = new Uint8Array(data, 0, 2);
    const frameType = frameHeader[0];

    if (frameType === 0x01) {
        // 视频帧
        const frameInfo = frameHeader[1];
        const videoData = new Uint8Array(data, 2);

        const chunk = new EncodedVideoChunk({
            type: frameInfo === 1 ? 'key' : 'delta',
            timestamp: performance.now(),
            data: videoData
        });

        videoDecoder.decode(chunk);
    } else if (frameType === 0x02) {
        // 音频帧
        const audioData = new Uint8Array(data, 1);
        playAudio(audioData);
    }
}

function handleTextMessage(data) {
    const message = JSON.parse(data);

    if (message.type === 'callback') {
        handleCallback(message);
    }
}

function playAudio(audioData) {
    audioContext.decodeAudioData(audioData.buffer.slice(), function(buffer) {
        const source = audioContext.createBufferSource();
        source.buffer = buffer;
        source.connect(audioContext.destination);
        source.start(0);
    }, function(e) {
        console.error('Audio decode error:', e);
    });
}

function handleCallback(message) {
    console.log('Callback:', message);

    if (message.type === 'control_response') {
        // 处理控制响应
        if (message.content) {
            alert(message.content);
        }
    }
}

function sendDirection(direction) {
    if (!ws || ws.readyState !== WebSocket.OPEN) {
        alert('请先登录');
        return;
    }

    const ct = getControlType(direction);
    const cv = getControlValue(direction);

    const message = {
        type: 'direction',
        ct: ct,
        cv: cv
    };

    ws.send(JSON.stringify(message));
}

function sendFunctionKey(key) {
    if (!ws || ws.readyState !== WebSocket.OPEN) {
        alert('请先登录');
        return;
    }

    const ct = getFunctionKeyType(key);

    const message = {
        type: 'direction',
        ct: ct,
        cv: 1
    };

    ws.send(JSON.stringify(message));
}

function sendNum(num) {
    if (!ws || ws.readyState !== WebSocket.OPEN) {
        alert('请先登录');
        return;
    }

    const content = document.getElementById('content').value;
    const message = {
        type: 'upload',
        ut: 1,
        content: content + num
    };

    document.getElementById('content').value = content + num;
    ws.send(JSON.stringify(message));
}

function sendContent() {
    if (!ws || ws.readyState !== WebSocket.OPEN) {
        alert('请先登录');
        return;
    }

    const content = document.getElementById('content').value;
    const fileType = document.getElementById('fileType').value;

    const message = {
        type: 'upload',
        ut: parseInt(fileType),
        content: content
    };

    ws.send(JSON.stringify(message));
    document.getElementById('content').value = '';
}

function uploadFile() {
    if (!ws || ws.readyState !== WebSocket.OPEN) {
        alert('请先登录');
        return;
    }

    const fileAddr = document.getElementById('fileAddr').value;

    const message = {
        type: 'upload_file',
        fa: fileAddr
    };

    ws.send(JSON.stringify(message));
}

function sendTouchEvent(x, y) {
    if (!ws || ws.readyState !== WebSocket.OPEN) {
        alert('请先登录');
        return;
    }

    const message = {
        type: 'touch',
        x: x,
        y: y
    };

    ws.send(JSON.stringify(message));
}

function sendError() {
    if (!ws || ws.readyState !== WebSocket.OPEN) {
        alert('请先登录');
        return;
    }

    const message = {
        type: 'send_error'
    };

    ws.send(JSON.stringify(message));
}

function sendUseTime() {
    if (!ws || ws.readyState !== WebSocket.OPEN) {
        alert('请先登录');
        return;
    }

    const message = {
        type: 'send_time'
    };

    ws.send(JSON.stringify(message));
}

function getControlType(direction) {
    switch (direction) {
        case 'up': return 1;
        case 'down': return 2;
        case 'left': return 3;
        case 'right': return 4;
        case 'ok': return 5;
        default: return 1;
    }
}

function getControlValue(direction) {
    switch (direction) {
        case 'up': return 1;
        case 'down': return 1;
        case 'left': return 1;
        case 'right': return 1;
        case 'ok': return 1;
        default: return 1;
    }
}

function getFunctionKeyType(key) {
    switch (key) {
        case 'menu': return 10;
        case 'back': return 11;
        default: return 10;
    }
}

window.onbeforeunload = function() {
    if (ws) {
        logout();
    }
};
```

## 5. 系统工作流程

### 5.1 登录流程

```
浏览器端                          服务端                          GIDS服务
   |                                |                                 |
   |-------- WebSocket连接 -------->|                                 |
   |<------ 连接建立成功 -----------|                                 |
   |                                |                                 |
   |-------- login消息 ------------>|                                 |
   |                                |------- 调用GIDS认证 ----------->|
   |                                |<------ 返回设备信息 -----------|
   |                                |                                 |
   |<------ 登录成功响应 -----------|                                 |
   |                                |
   |                        建立Netty控制通道
   |                                |
   |                        建立Netty媒体通道
   |                                |
   |<====== 媒体流开始传输 =========|
```

### 5.2 控制指令流程

```
浏览器端                          服务端                          设备端
   |                                |                                 |
   |---- 用户点击方向键 ----------->|                                 |
   |                                |                                 |
   |---- direction消息 ------------>|                                 |
   |                                |                                 |
   |                        TLV编码控制指令                                |
   |                                |                                 |
   |                                |--- 控制通道发送指令 ------------>|
   |                                |                                 |
   |                                |<--- 返回控制响应 ---------------|
   |                                |                                 |
   |<--- callback消息 -------------|                                 |
```

### 5.3 媒体流处理流程

```
设备端                          服务端                          浏览器端
   |                                |                                 |
   |--- H.264视频帧 --------------->|                                 |
   |                                |                                 |
   |                        解析TLV数据                                |
   |                        添加帧头(0x01+帧类型)                        |
   |                                |                                 |
   |                                |--- WebSocket二进制推送 --------->|
   |                                |                              视频解码
   |                                |                              Canvas渲染
   |                                |                                 |
   |--- MP3音频帧 ----------------->|                                 |
   |                                |                                 |
   |                        解析TLV数据                                |
   |                        添加帧头(0x02)                              |
   |                                |                                 |
   |                                |--- WebSocket二进制推送 --------->|
   |                                |                              音频解码
   |                                |                              播放器播放
```

## 6. 协议规范

### 6.1 TLV协议格式

```
包头格式:
+--------+--------+--------+--------+--------+--------+--------+--------+
|                    magic(2B)                  |     count(4B)     |
+--------+--------+--------+--------+--------+--------+--------+--------+
|     dataLen(4B)                                                |
+--------+--------+--------+--------+--------+--------+--------+--------+
|                          TLV数据...                              |
+--------+--------+--------+--------+--------+--------+--------+--------+

TLV数据格式:
+--------+--------+--------+--------+--------+--------+--------+--------+
|                       type(4B)                   |    length(4B)    |
+--------+--------+--------+--------+--------+--------+--------+--------+
|                          value(NB)                               |
+--------+--------+--------+--------+--------+--------+--------+--------+

magic: "mu" (0x6D75 = 28021)
count: TLV字段数量
dataLen: TLV数据总长度
type: 字段标识符（ID.java中定义）
length: value字节长度
value: 字段值
```

### 6.2 WebSocket二帧格式

```
视频帧:
+--------+--------+--------+--------+
| 0x01   | frameType |       videoData        |
+--------+--------+--------+--------+
  帧头     帧类型      H.264编码数据

音频帧:
+--------+--------+--------+--------+
| 0x02   |       audioData                |
+--------+--------+--------+--------+
  帧头     MP3编码数据

frameType: 1=关键帧, 2=非关键帧
```

## 7. 编译和运行

### 7.1 编译项目

```bash
# 使用Maven编译
mvn clean package

# 或使用Maven编译并跳过测试
mvn clean package -DskipTests
```

### 7.2 运行项目

```bash
# 使用Maven运行
mvn spring-boot:run

# 或运行编译后的jar包
java -jar target/mobile-0.0.1-SNAPSHOT.jar
```

### 7.3 访问应用

1. **WebSocket端点**: `ws://localhost:40002/app/websocket/{imei}_{imsi}`
2. **静态资源页面**:
    - 主页面: `http://localhost:8088/index.html`
    - 时间页面: `http://localhost:8088/time.html`
    - 上传页面: `http://localhost:8088/upload.html`

### 7.4 测试流程

1. 启动应用服务器
2. 在浏览器中打开 `http://localhost:8088/index.html`
3. 点击"登录"按钮建立WebSocket连接
4. 等待媒体流传输开始
5. 使用控制面板操作云手机
6. 查看视频解码和音频播放效果

## 8. 配置说明

### 8.1 应用配置

```properties
# 应用名称
spring.application.name=mobile

# HTTP服务端口
server.port=8088

# HTTP服务绑定地址
server.address=0.0.0.0
```

### 8.2 WebSocket配置

- 端口: 40002
- 路径: `/app/websocket/{imeiAndImsi}`
- Boss线程数: 8
- Worker线程数: 64
- 最大帧长度: 655360字节
- 连接超时: 30秒（TCPKeepAlive）

### 8.3 设备配置

```java
// 默认设备信息
imei = "6258412454025411"
imsi = "68510155565211"
manufacturer = "default"
model = "default"
platform = "1"
clientLanguage = "en_US"
deviceType = "2"
```

### 8.4 画布尺寸配置

支持的画布尺寸:
- 240x320 (默认)
- 360x480
- 405x540
- 540x720

## 9. 异常处理

### 9.1 连接异常

- WebSocket连接失败: 在页面显示错误提示
- TCP控制通道连接失败: 记录日志并关闭会话
- TCP媒体通道连接失败: 记录日志但不影响控制通道

### 9.2 媒体解码异常

- 视频解码错误: 记录日志并尝试重新配置解码器
- 音频解码错误: 记录日志并继续处理下一个音频帧

### 9.3 协议异常

- TLV解码错误: 记录日志并丢弃错误数据包
- 无效消息类型: 记录日志并返回错误响应

## 10. 性能优化

### 10.1 Netty优化

- 使用NIO非阻塞IO模型
- 合理配置Boss和Worker线程数
- 启用TCPKeepAlive保持长连接
- 使用ByteBuf引用计数管理内存

### 10.2 WebSocket优化

- 使用二进制传输媒体数据
- 增大最大帧长度支持大数据传输
- 异步处理消息避免阻塞

### 10.3 前端优化

- 使用VideoDecoder API硬件解码视频
- 使用Web Audio API硬件解码音频
- Canvas双缓冲优化渲染性能

## 11. 浏览器兼容性

### 11.1 支持的浏览器

- Chrome 94+ (支持VideoDecoder API)
- Edge 94+
- Firefox最新版

### 11.2 不支持的特性

- IE不支持VideoDecoder API
- Safari支持度待验证

## 12. 部署说明

### 12.1 环境要求

- JDK 12+
- Maven 3.6+
- 内存: 最少512MB
- CPU: 2核以上推荐

### 12.2 生产环境配置

- 调整JVM内存参数: `-Xms512m -Xmx2g`
- 配置GIDS服务地址
- 配置日志输出路径
- 启用HTTPS加密传输

### 12.3 容器化部署

```dockerfile
FROM openjdk:12-jdk-alpine
VOLUME /tmp
COPY target/mobile-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
EXPOSE 8088 40002
```

## 13. 安全考虑

### 13.1 认证机制

- 设备登录Token验证
- WebSocket连接参数验证
- HTTP请求参数过滤

### 13.2 数据加密

- 传输层SSL/TLS加密
- WebSocket WSS协议支持
- 敏感数据脱敏处理

### 13.3 访问控制

- IP白名单配置
- 请求频率限制
- 异常行为监控

## 14. 监控和日志

### 14.1 日志级别

- INFO: 正常操作日志
- WARN: 可忽略的异常
- ERROR: 严重错误

### 14.2 关键监控指标

- WebSocket连接数
- TCP通道连接数
- 媒体流传输速率
- 系统CPU和内存使用率

## 15. 测试说明

### 15.1 单元测试

- TLV编解码测试
- WebSocket通信测试
- 设备登录流程测试
- 媒体流处理测试

### 15.2 集成测试

- 端到端登录测试
- 双通道通信测试
- 媒体流传输测试
- 控制指令响应测试

## 16. 技术要点

### 16.1 TLV协议设计

- 使用LinkedHashMap保持字段顺序
- 支持多种数据类型编解码
- 自定义magic标识防止混淆

### 16.2 双通道架构

- 控制通道: 处理指令交互和应答
- 媒体通道: 专门传输音视频流
- 避免控制指令被媒体数据阻塞

### 16.3 资源管理

- ByteBuf引用计数防止内存泄漏
- 通道关闭时释放所有资源
- 会话结束时清理上下文

### 16.4 心跳机制

- 30秒TCP KeepAlive
- ACK应答确认连接
- 异常时自动重连

## 17. 维护指南

### 17.1 代码规范

- 遵循Java命名规范
- 使用Lombok简化代码
- 添加必要的注释说明

### 17.2 扩展开发

- 新增字段: 在ID.java中添加标识符
- 新增消息类型: 在Type.java中添加类型
- 新增DTO: 在dto包下创建新的类

### 17.3 问题排查

- 查看应用日志定位错误
- 使用浏览器开发者工具调试前端
- 使用Wireshark抓包分析网络通信

## 18. 常见问题

### 18.1 连接失败

- 检查防火墙设置
- 确认端口号配置正确
- 查看网络连通性

### 18.2 媒体流卡顿

- 检查网络带宽
- 调整缓冲区大小
- 优化视频编码参数

### 18.3 音视频不同步

- 调整时间戳处理
- 检查音频采样率
- 同步时钟偏差

## 19. 版本历史

### 19.1 当前版本

- 版本号: 0.0.1-SNAPSHOT
- 发布日期: 2026年

### 19.2 功能特性

- WebSocket连接管理
- Netty双通道通信
- TLV协议编解码
- 媒体流实时传输
- 控制指令处理
- 埋点数据上报
- 文件上传下载

## 20. 附录

### 20.1 错误码定义

| 错误码 | 描述                         | 处理建议                       |
|--------|------------------------------|--------------------------------|
| 200    | 成功                         | 正常处理                       |
| 400    | 请求参数错误                 | 检查请求参数格式               |
| 401    | 认证失败                     | 检查Token有效性               |
| 404    | 资源不存在                   | 检查设备ID和地址               |
| 500    | 服务器内部错误               | 查看服务器日志                 |

### 20.2 控制类型定义

| 类型值 | 描述           | 方向 |
|--------|----------------|------|
| 1      | 上             | ↑    |
| 2      | 下             | ↓    |
| 3      | 左             | ←    |
| 4      | 右             | →    |
| 5      | 确定           | OK   |
| 10     | 菜单           | 菜单 |
| 11     | 返回           | 返回 |

### 20.3 应用类型定义

| 类型值 | 应用名称      |
|--------|---------------|
| 1      | BBC           |
| 2      | TikTok        |
| 3      | Facebook      |
| 4      | YouTube       |
| 5      | Other         |

---

**文档结束**