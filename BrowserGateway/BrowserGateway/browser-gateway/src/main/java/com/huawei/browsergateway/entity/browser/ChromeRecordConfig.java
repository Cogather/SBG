package com.huawei.browsergateway.entity.browser;
import com.huawei.browsergateway.entity.request.InitBrowserRequest;
import com.huawei.browsergateway.util.UserIdUtil;
import com.moon.cloud.browser.sdk.model.pojo.ChromeParams;
import lombok.Data;

@Data
public class ChromeRecordConfig {
    private String codecMode; // webcodecs 或 ffmpeg
    private String dataDealAddr;
    private String imeiAndImsi;
    private Integer appType;

    // 视频相关参数
    private Integer width;
    private Integer height;
    private Integer bitRate; // 码率
    private Integer frameRate; // 帧率

    // 音频相关参数
    private Integer sampleRate; // 音频采样率
    private Integer channelCount; // 声道数量
    private Boolean echoCancellation = true;
    private Boolean noiseSuppression = true;

    private String controlExtensionId;
    private String controlExtensionPath;

    private int limit;


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