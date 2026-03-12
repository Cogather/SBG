package com.huawei.browsergateway.entity.browser;

import com.huawei.browsergateway.entity.request.InitBrowserRequest;
import com.huawei.browsergateway.util.UserIdUtil;
import com.moon.cloud.browser.sdk.model.pojo.ChromeParams;
import lombok.Data;

/**
 * Chrome录制配置
 */
@Data
public class ChromeRecordConfig {
    /** 编解码模式：webcodecs 或 ffmpeg */
    private String codecMode;
    /** 数据处理服务地址 */
    private String dataDealAddr;
    /** IMEI与IMSI组合标识 */
    private String imeiAndImsi;
    /** 应用类型 */
    private Integer appType;

    /** 视频宽度 */
    private Integer width;
    /** 视频高度 */
    private Integer height;
    /** 视频码率 */
    private Integer bitRate;
    /** 视频帧率 */
    private Integer frameRate;

    /** 音频采样率 */
    private Integer sampleRate;
    /** 声道数量 */
    private Integer channelCount;
    /** 是否开启回声消除 */
    private Boolean echoCancellation = true;
    /** 是否开启噪声抑制 */
    private Boolean noiseSuppression = true;

    /** 控制扩展ID */
    private String controlExtensionId;
    /** 控制扩展路径 */
    private String controlExtensionPath;

    /** 限制数量 */
    private int limit;

    /**
     * 从初始化请求和Chrome参数构建录制配置
     *
     * @param request      初始化浏览器请求
     * @param chromeParams Chrome参数
     * @return ChromeRecordConfig实例
     */
    public static ChromeRecordConfig from(InitBrowserRequest request, ChromeParams chromeParams) {
        ChromeRecordConfig ret = new ChromeRecordConfig();
        ret.setDataDealAddr(request.getInnerMediaEndpoint());
        ret.setImeiAndImsi(UserIdUtil.generateUserIdByImeiAndImsi(request.getImei(), request.getImsi()));
        ret.setAppType(request.getAppType());

        ret.setWidth(chromeParams.getChromeWidth());
        ret.setHeight(chromeParams.getChromeHeight());
        ret.setBitRate(chromeParams.getBitRite());
        ret.setFrameRate(chromeParams.getFrameRate());

        ret.setSampleRate(chromeParams.getSampleRate());
        ret.setChannelCount(chromeParams.getChannels());

        ret.setControlExtensionId(chromeParams.getControlExtentionId());
        return ret;
    }
}
