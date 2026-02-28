package com.huawei.browsergateway.api;

import cn.hutool.json.JSONUtil;

import com.huawei.browsergateway.config.Config;
import com.huawei.browsergateway.entity.CommonResult;
import com.huawei.browsergateway.entity.ResultCode;
import com.huawei.browsergateway.entity.request.DeleteUserDataRequest;
import com.huawei.browsergateway.entity.request.InitBrowserRequest;
import com.huawei.browsergateway.entity.response.DeleteUserDataResponse;
import com.huawei.browsergateway.service.IChromeSet;
import com.huawei.browsergateway.service.IFileStorage;
import com.huawei.browsergateway.service.IRemote;
import com.huawei.browsergateway.service.impl.UserChrome;
import com.huawei.browsergateway.service.impl.UserData;


import java.nio.ByteOrder;

import com.huawei.browsergateway.util.UserIdUtil;
import com.huawei.browsergateway.util.encode.Message;
import com.huawei.browsergateway.util.encode.Tlv;
import com.huawei.browsergateway.util.encode.TlvCodec;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * Chrome浏览器管理API
 */
@RestController
@RequestMapping(path = "/browsergw/browser", produces = MediaType.APPLICATION_JSON_VALUE)
public class ChromeApi {

    private static final Logger log = LogManager.getLogger(ChromeApi.class);
    @Resource
    private IChromeSet chromeSet;
    @Autowired
    private IFileStorage fs;
    @Autowired
    private Config config;
    @Autowired
    private IRemote remote;

    @DeleteMapping("/userdata/delete")
    public CommonResult<DeleteUserDataResponse> deleteUserData(@RequestBody DeleteUserDataRequest param) {
        String userId = UserIdUtil.generateUserIdByImeiAndImsi(param.getImei(), param.getImsi());
        log.info("deleteUserData userId:{}", userId);
        UserChrome userChromeInfo = chromeSet.get(userId);
        if (userChromeInfo != null) {
            log.info("deleteUserData userChromeInfo:{}", userChromeInfo);
            chromeSet.delete(userId);
        }
        UserData userdata = new UserData(fs, config.getUserDataPath(), userId, config.getSelfAddr(), remote);
        userdata.delete();
        return CommonResult.success(new DeleteUserDataResponse().setImei(param.getImei()).setImsi(param.getImsi()));
    }

    @PostMapping("/preOpen")
    public CommonResult<String> preOpenBrowser(@RequestBody InitBrowserRequest param) {
        if (param == null) {
            log.error("pre open browser error: param is null");
            return CommonResult.error(ResultCode.FAIL);
        }
        
        // 验证IMEI和IMSI不能同时为空
        String imei = param.getImei() == null ? "" : param.getImei().trim();
        String imsi = param.getImsi() == null ? "" : param.getImsi().trim();
        if (imei.isEmpty() && imsi.isEmpty()) {
            log.error("pre open browser error: both imei and imsi are empty");
            return CommonResult.error(ResultCode.FAIL);
        }
        
        String userId = UserIdUtil.generateUserIdByImeiAndImsi(param.getImei(), param.getImsi());
        log.info("pre open browser request userId:{}", userId);

        param.setInnerMediaEndpoint(config.getInnerMediaEndpoint());
        try {
            String json = JSONUtil.toJsonStr(param);
            Message message = JSONUtil.toBean(json, Message.class);
            message.setAudType("");
            message.setToken("");
            Tlv tlv = TlvCodec.marshal(message);
            byte[] encodeParam = tlv.marshal(ByteOrder.BIG_ENDIAN);
            remote.createChrome(encodeParam, param, null);
            return CommonResult.success("success");
        } catch (Exception e) {
            log.error("pre open browser error userId:{}", param.getImei(), e);
            return CommonResult.error(ResultCode.FAIL);
        }
    }
}
