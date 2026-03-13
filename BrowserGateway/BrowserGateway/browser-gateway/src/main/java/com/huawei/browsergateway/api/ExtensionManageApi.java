package com.huawei.browsergateway.api;

import cn.hutool.json.JSONUtil;
import com.huawei.browsergateway.entity.CommonResult;
import com.huawei.browsergateway.entity.ResultCode;
import com.huawei.browsergateway.entity.plugin.PluginActive;
import com.huawei.browsergateway.entity.request.LoadExtensionRequest;
import com.huawei.browsergateway.entity.response.LoadExtensionResponse;
import com.huawei.browsergateway.service.ExtensionManageService;
import com.huawei.browsergateway.util.ParamValidator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.servicecomb.provider.rest.common.RestSchema;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * 插件管理API
 * 提供浏览器插件的加载和状态查询功能
 */
@RestController
@RequestMapping("/browsergw/extension")
@RestSchema(schemaId = "extension")
public class ExtensionManageApi {
    private static final Logger log = LogManager.getLogger(ExtensionManageApi.class);

    @Resource
    private ExtensionManageService extensionManageService;

    /**
     * 加载浏览器插件
     * 用于更新MUEN插件并重新加载扩展
     *
     * @param param 加载插件请求参数，包含bucket名称和插件文件路径
     * @return 加载结果
     */
    @PostMapping("/load")
    public CommonResult<LoadExtensionResponse> loadExtension(@RequestBody LoadExtensionRequest param) {
        String validateError = ParamValidator.validateLoadExtensionRequest(param);
        if (validateError != null) {
            log.warn("load extension invalid param: {}", validateError);
            return CommonResult.error(ResultCode.VALIDATE_ERROR.getCode(), validateError);
        }
        try {
            log.info("update muen plugin, reload extension, params:{}", JSONUtil.toJsonStr(param));

            boolean result = extensionManageService.loadExtension(param);
            if (result) {
                LoadExtensionResponse response = buildLoadExtensionResponse(param);
                return CommonResult.success(response);
            }

            return CommonResult.error(ResultCode.FAIL.getCode(), "reload extension failed");
        } catch (Exception e) {
            log.error("load extension failed, params:{}", JSONUtil.toJsonStr(param), e);
            return CommonResult.error(ResultCode.FAIL.getCode(), "reload extension failed: " + e.getMessage());
        }
    }

    /**
     * 构建加载插件响应对象
     *
     * @param param 加载插件请求参数
     * @return 加载插件响应对象
     */
    private LoadExtensionResponse buildLoadExtensionResponse(LoadExtensionRequest param) {
        try {
            LoadExtensionResponse response = new LoadExtensionResponse();
            response.setBucketName(param.getBucketName());
            response.setExtensionFilePath(param.getExtensionFilePath());
            return response;
        } catch (Exception e) {
            log.error("build load extension response failed, params:{}", JSONUtil.toJsonStr(param), e);
            throw e;
        }
    }

    /**
     * 获取插件信息
     * 查询当前激活的插件状态
     *
     * @return 插件状态信息
     */
    @GetMapping("/pluginInfo")
    public CommonResult<PluginActive> getPluginInfo() {
        try {
            PluginActive pluginStatus = extensionManageService.getPluginInfo();
            return CommonResult.success(pluginStatus);
        } catch (Exception e) {
            log.error("get plugin info failed", e);
            return CommonResult.error(ResultCode.FAIL);
        }
    }
}