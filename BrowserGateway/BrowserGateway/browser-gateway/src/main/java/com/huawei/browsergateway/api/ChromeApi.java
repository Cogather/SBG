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
import com.huawei.browsergateway.util.UserIdUtil;
import com.huawei.browsergateway.util.encode.Message;
import com.huawei.browsergateway.util.encode.Tlv;
import com.huawei.browsergateway.util.encode.TlvCodec;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.servicecomb.provider.rest.common.RestSchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.nio.ByteOrder;

@RestController
@RequestMapping("/browsergw/browser")
@RestSchema(schemaId = "browser")
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
        log.info("delete user data request, params:{}", JSONUtil.toJsonStr(param));

        UserChrome userChromeInfo = chromeSet.get(userId);
        if (userChromeInfo != null) {
            log.info("The user has a browser instance, before delete user data, " +
                    "close the browser instance, userId:{}", userId);
            chromeSet.delete(userId);
        }

        UserData userData = new UserData(fs, config.getUserDataPath(), userId, config.getSelfAddr(), remote);
        userData.delete();
        return CommonResult.success(new DeleteUserDataResponse().setImei(param.getImei()).setImsi(param.getImsi()));
    }

    @PostMapping("/preOpen")
    public CommonResult<String> preOpenBrowser(@RequestBody InitBrowserRequest param) {
        String userId = UserIdUtil.generateUserIdByImeiAndImsi(param.getImei(), param.getImsi());
        log.info("pre open browser request, params:{}", JSONUtil.toJsonStr(param));

        param.setInnerMediaEndpoint(config.getInnerMediaEndpoint());
        try {
            //muen sdk login接口获取chrome config 需要byte[]数据且必须包含某些字段
            String json = JSONUtil.toJsonStr(param);
            Message message = JSONUtil.toBean(json, Message.class);
            message.setAudType("");
            message.setToken("");
            Tlv tlv = TlvCodec.marshal(message);
            byte[] encodeParam = tlv.marshal(ByteOrder.BIG_ENDIAN);
            remote.createChrome(encodeParam, param, null);
            return CommonResult.success("success");
        } catch (Exception e) {
            log.error("pre open browser failed, user:{}, params:{}", userId, JSONUtil.toJsonStr(param), e);
            return CommonResult.error(ResultCode.FAIL);
        }
    }

}
