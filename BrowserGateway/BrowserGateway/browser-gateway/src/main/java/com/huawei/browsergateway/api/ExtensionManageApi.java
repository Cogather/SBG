package com.huawei.browsergateway.api;

import cn.hutool.json.JSONUtil;
import com.huawei.browsergateway.entity.CommonResult;
import com.huawei.browsergateway.entity.ResultCode;
import com.huawei.browsergateway.entity.plugin.PluginActive;
import com.huawei.browsergateway.entity.request.LoadExtensionRequest;
import com.huawei.browsergateway.entity.response.LoadExtensionResponse;
import com.huawei.browsergateway.service.ExtensionManageService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.servicecomb.provider.rest.common.RestSchema;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@RestController
@RequestMapping("/browsergw/extension")
@RestSchema(schemaId = "extension")
public class ExtensionManageApi {
    private static final Logger log = LogManager.getLogger(ExtensionManageApi.class);

    @Resource
    private ExtensionManageService extensionManageService;

    @PostMapping("/load")
    public CommonResult<LoadExtensionResponse> loadExtension(@RequestBody LoadExtensionRequest param) {
        log.info("update muen plugin, reload extension, params:{}", JSONUtil.toJsonStr(param));
        boolean result = extensionManageService.loadExtension(param);
        if (result) {
            LoadExtensionResponse response = new LoadExtensionResponse();
            response.setBucketName(param.getBucketName());
            response.setExtensionFilePath(param.getExtensionFilePath());
            return CommonResult.success(response);
        }
        return CommonResult.error(ResultCode.FAIL.getCode(), "reload extension failed");
    }

    @GetMapping("/pluginInfo")
    public CommonResult<PluginActive> getPluginInfo() {
        PluginActive pluginStatus = extensionManageService.getPluginInfo();
        return CommonResult.success(pluginStatus);
    }
}