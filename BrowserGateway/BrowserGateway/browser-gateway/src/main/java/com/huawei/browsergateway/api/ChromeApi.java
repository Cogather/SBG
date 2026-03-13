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
import com.huawei.browsergateway.util.ParamValidator;
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

/**
 * 浏览器管理API
 * 提供浏览器实例的创建、删除和用户数据管理功能
 */
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

    /**
     * 删除用户数据
     * 如果用户有活跃的浏览器实例，会先关闭实例再删除数据
     *
     * @param param 删除请求参数，包含IMEI和IMSI
     * @return 删除结果
     */
    @DeleteMapping("/userdata/delete")
    public CommonResult<DeleteUserDataResponse> deleteUserData(@RequestBody DeleteUserDataRequest param) {
        String validateError = ParamValidator.validateDeleteUserDataRequest(param);
        if (validateError != null) {
            log.warn("delete user data invalid param: {}", validateError);
            return CommonResult.error(ResultCode.VALIDATE_ERROR.getCode(), validateError);
        }
        String userId = null;
        try {
            userId = UserIdUtil.generateUserIdByImeiAndImsi(param.getImei(), param.getImsi());
            log.info("delete user data request, params:{}", JSONUtil.toJsonStr(param));

            closeExistingBrowserInstance(userId);
            deleteUserDataFiles(userId);

            DeleteUserDataResponse response = new DeleteUserDataResponse()
                    .setImei(param.getImei())
                    .setImsi(param.getImsi());
            return CommonResult.success(response);
        } catch (Exception e) {
            log.error("delete user data failed, userId:{}, params:{}", userId, JSONUtil.toJsonStr(param), e);
            return CommonResult.error(ResultCode.FAIL);
        }
    }

    /**
     * 关闭用户已存在的浏览器实例
     *
     * @param userId 用户ID
     */
    private void closeExistingBrowserInstance(String userId) {
        try {
            UserChrome userChromeInfo = chromeSet.get(userId);
            if (userChromeInfo != null) {
                log.info("Closing existing browser instance before deleting user data, userId:{}", userId);
                chromeSet.delete(userId);
            }
        } catch (Exception e) {
            log.error("Failed to close existing browser instance, userId:{}", userId, e);
            throw e;
        }
    }

    /**
     * 删除用户数据文件
     *
     * @param userId 用户ID
     */
    private void deleteUserDataFiles(String userId) {
        try {
            UserData userData = new UserData(fs, config.getUserDataPath(), userId, config.getSelfAddr(), remote);
            userData.delete();
        } catch (Exception e) {
            log.error("delete user data files failed, userId:{}", userId, e);
            throw e;
        }
    }

    /**
     * 预打开浏览器
     * 为MUEN SDK准备浏览器配置并创建Chrome实例
     *
     * @param param 初始化浏览器请求参数
     * @return 操作结果
     */
    @PostMapping("/preOpen")
    public CommonResult<String> preOpenBrowser(@RequestBody InitBrowserRequest param) {
        String validateError = ParamValidator.validateInitBrowserRequest(param);
        if (validateError != null) {
            log.warn("pre open browser invalid param: {}", validateError);
            return CommonResult.error(ResultCode.VALIDATE_ERROR.getCode(), validateError);
        }
        String userId = UserIdUtil.generateUserIdByImeiAndImsi(param.getImei(), param.getImsi());
        log.info("pre open browser request, params:{}", JSONUtil.toJsonStr(param));

        param.setInnerMediaEndpoint(config.getInnerMediaEndpoint());

        try {
            byte[] encodedParam = encodeRequestForMuenSdk(param);
            remote.createChrome(encodedParam, param, null);
            return CommonResult.success("success");
        } catch (Exception e) {
            log.error("pre open browser failed, user:{}, params:{}", userId, JSONUtil.toJsonStr(param), e);
            return CommonResult.error(ResultCode.FAIL);
        }
    }

    /**
     * 将请求参数编码为MUEN SDK所需的字节数组格式
     * MUEN SDK的login接口要求byte[]数据且必须包含特定字段
     *
     * @param param 初始化浏览器请求参数
     * @return 编码后的字节数组
     */
    private byte[] encodeRequestForMuenSdk(InitBrowserRequest param) throws Exception {
        try {
            String json = JSONUtil.toJsonStr(param);
            Message message = JSONUtil.toBean(json, Message.class);
            message.setAudType("");
            message.setToken("");
            Tlv tlv = TlvCodec.marshal(message);
            return tlv.marshal(ByteOrder.BIG_ENDIAN);
        } catch (Exception e) {
            log.error("encode request for muen sdk failed, params:{}", JSONUtil.toJsonStr(param), e);
            throw e;
        }
    }
}
