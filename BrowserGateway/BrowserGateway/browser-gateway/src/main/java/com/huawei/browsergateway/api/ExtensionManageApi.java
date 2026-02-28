package com.huawei.browsergateway.api;

import com.huawei.browsergateway.entity.CommonResult;
import com.huawei.browsergateway.entity.ResultCode;
import com.huawei.browsergateway.entity.plugin.PluginActive;
import com.huawei.browsergateway.entity.request.LoadExtensionRequest;
import com.huawei.browsergateway.entity.response.LoadExtensionResponse;
import com.huawei.browsergateway.service.ExtensionManageService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * 扩展管理API
 */
@RestController
@RequestMapping(path = "/browsergw/extension", produces = MediaType.APPLICATION_JSON_VALUE)
public class ExtensionManageApi {
    
    private static final Logger log = LogManager.getLogger(ExtensionManageApi.class);
    
    @Resource
    private ExtensionManageService extensionManageService;
    
    /**
     * 加载扩展
     * POST /browsergw/extension/load
     * 
     * 功能说明：加载或更新浏览器扩展插件，需要删除所有现有浏览器实例并重新加载
     * 
     * @param request 加载扩展请求参数
     * @return 加载结果
     */
    @PostMapping(value = "/load")
    public CommonResult<LoadExtensionResponse> loadExtension(@RequestBody LoadExtensionRequest request) {
        log.info("load extension:{}", request);
        
        // 参数验证
        if (request == null) {
            log.error("load extension error: request is null");
            return CommonResult.error(ResultCode.VALIDATE_ERROR);
        }
        
        // 验证必填字段
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            log.error("load extension error: name is empty");
            return CommonResult.error(ResultCode.VALIDATE_ERROR);
        }
        
        if (request.getVersion() == null || request.getVersion().trim().isEmpty()) {
            log.error("load extension error: version is empty");
            return CommonResult.error(ResultCode.VALIDATE_ERROR);
        }
        
        if (request.getBucketName() == null || request.getBucketName().trim().isEmpty()) {
            log.error("load extension error: bucketName is empty");
            return CommonResult.error(ResultCode.VALIDATE_ERROR);
        }
        
        if (request.getExtensionFilePath() == null || request.getExtensionFilePath().trim().isEmpty()) {
            log.error("load extension error: extensionFilePath is empty");
            return CommonResult.error(ResultCode.VALIDATE_ERROR);
        }
        
        // 验证扩展文件路径必须以.jar结尾
        if (!request.getExtensionFilePath().endsWith(".jar")) {
            log.error("load extension error: extensionFilePath must end with .jar");
            return CommonResult.error(ResultCode.VALIDATE_ERROR);
        }
        
        boolean result = extensionManageService.loadExtension(request);
        if (result) {
            LoadExtensionResponse response = new LoadExtensionResponse();
            response.setBucketName(request.getBucketName());
            response.setExtensionFilePath(request.getExtensionFilePath());
            return CommonResult.success(response);
        }
        return CommonResult.error(ResultCode.FAIL.getCode(), "reload extension failed");
    }
    
    /**
     * 获取插件信息
     * GET /browsergw/extension/pluginInfo
     * 
     * 功能说明：获取当前激活的浏览器扩展插件的详细信息
     * 
     * @return 插件信息
     */
    @GetMapping(value = "/pluginInfo")
    public CommonResult<PluginActive> getPluginInfo() {
        log.info("获取插件信息请求");
        PluginActive pluginActive = extensionManageService.getPluginInfo();
        return CommonResult.success(pluginActive);
    }
}
