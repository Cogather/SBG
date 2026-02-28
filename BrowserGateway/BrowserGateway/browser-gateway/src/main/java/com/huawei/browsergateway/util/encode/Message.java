package com.huawei.browsergateway.util.encode;

import lombok.Data;

@Data
public class Message {
    // 基础字段
    @TlvTag(type = "int32", id = 1)
    private int type;      // 消息类型：1.登录 2.心跳等

    @TlvTag(type = "string", id = 2)
    private String factory; // 厂商

    @TlvTag(type = "string", id = 3)
    private String devType; // 机型

    @TlvTag(type = "string", id = 4)
    private String imsi;    // imsi值

    @TlvTag(type = "string", id = 5)
    private String imei;    // imei值

    @TlvTag(type = "int32", id = 6)
    private int lcdWidth;   // 设备屏幕宽度

    @TlvTag(type = "int32", id = 7)
    private int lcdHeight;  // 设备屏幕高度

    @TlvTag(type = "string", id = 8)
    private String audType; // 音频类型

    // 应答相关
    @TlvTag(type = "int32", id = 9)
    private int ackType;    // 服务端通用应答值

    @TlvTag(type = "int32", id = 10)
    private int code;       // 返回状态码

    // 事件与控制
    @TlvTag(type = "int32", id = 11)
    private int event;      // 事件消息(0 暂停 1 恢复 2 异常)

    @TlvTag(type = "int32", id = 12)
    private int ctrlType;   // 控制类型

    @TlvTag(type = "int32", id = 13)
    private int ctrlVal;    // 控制值

    @TlvTag(type = "int64", id = 14)
    private long seq;       // 时间序列

    // 音视频数据
    @TlvTag(type = "bytes", id = 15)
    private byte[] audioData; // 音频流数据

    @TlvTag(type = "bytes", id = 16)
    private byte[] videoData; // 视频流数据

    @TlvTag(type = "int32", id = 17)
    private int audSmprate; // 音频采样率

    @TlvTag(type = "int32", id = 18)
    private int audChannel; // 音频通道数

    @TlvTag(type = "int32", id = 19)
    private int appType;    // 应用类型

    // 网络地址信息
    @TlvTag(type = "string", id = 20)
    private String tcpAddr;   // 流媒体地址

    @TlvTag(type = "string", id = 21)
    private String token;     // 用户http登录token

    @TlvTag(type = "string", id = 22)
    private String sessionID; // 浏览器sessionId

    // 媒体相关
    @TlvTag(type = "int32", id = 23)
    private int frameType;  // 帧类型

    // 控制响应
    @TlvTag(type = "int32", id = 24)
    private int ctrlRspElm;   // 服务端控制响应元素

    @TlvTag(type = "int32", id = 25)
    private int ctrlRspInfo;  // 服务端控制响应信息反馈

    @TlvTag(type = "string", id = 26)
    private String content;   // 传输内容

    // 信息上传
    @TlvTag(type = "int32", id = 27)
    private int uploadType;  // 设备端信息上传类型

    @TlvTag(type = "int32", id = 28)
    private int appID;       // 应用appid

    @TlvTag(type = "int32", id = 29)
    private int platType;    // 平台类型

    @TlvTag(type = "string", id = 30)
    private String extType;  // 扩展机型

    // 视频相关
    @TlvTag(type = "string", id = 31)
    private String videoAddr;   // 视频地址

    @TlvTag(type = "int32", id = 32)
    private int videoModel;     // 视频类型

    @TlvTag(type = "int32", id = 33)
    private int playerStatus;   // 播放器状态

    // 文件上传
    @TlvTag(type = "int32", id = 34)
    private int uploadFileType;   // 文件传输类型

    @TlvTag(type = "int32", id = 35)
    private int uploadFileResult; // 文件传输反馈

    @TlvTag(type = "string", id = 36)
    private String fileAddr;      // 文件地址

    // 播放模式与图片数据
    @TlvTag(type = "int32", id = 37)
    private int playMode;   // 播放模式

    @TlvTag(type = "bytes", id = 38)
    private byte[] jpgData; // JPG流数据

    // 位置信息
    @TlvTag(type = "string", id = 39)
    private String locationData; // 位置信息(JSON)

    // Socks5代理
    @TlvTag(type = "string", id = 40)
    private String socks5Addr;   // 代理地址

    @TlvTag(type = "string", id = 41)
    private String socks5Tunnel; // 代理通道

    @TlvTag(type = "int32", id = 42)
    private int ability;         // 能力值

    // 命令与状态
    @TlvTag(type = "int32", id = 43)
    private int command;         // 回调事件类型

    @TlvTag(type = "int32", id = 44)
    private int status;          // 状态(成功/失败/超时)

    @TlvTag(type = "string", id = 45)
    private String clientLanguage; // 客户端语言

    @TlvTag(type = "int32", id = 46)
    private int deviceType;      // 设备类型细分

    // 输入相关
    @TlvTag(type = "int32", id = 47)
    private int writeType;   // 输入框类型

    @TlvTag(type = "int32", id = 48)
    private int networkType; // 网络类型

    @TlvTag(type = "string", id = 49)
    private String urlType;  // 页面地址类型

    @TlvTag(type = "string", id = 50)
    private String imei1;  // imei1

    @TlvTag(type = "string", id = 51)
    private String imei2;  // imei2

    @TlvTag(type = "string", id = 54)
    private String tlsAddr;  // 页面地址类型
}