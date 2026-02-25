package com.huawei.browsergateway.api;

import cn.hutool.core.util.StrUtil;
import com.huawei.browsergateway.common.enums.ErrorCodeEnum;
import com.huawei.browsergateway.common.response.BaseResponse;
import com.huawei.browsergateway.entity.plugin.PluginActive;
import com.huawei.browsergateway.entity.request.LoadExtensionRequest;
import com.huawei.browsergateway.entity.response.LoadExtensionResponse;
import com.huawei.browsergateway.entity.response.PluginInfoResponse;
import com.huawei.browsergateway.exception.common.BusinessException;
import com.huawei.browsergateway.service.IChromeSet;
import com.huawei.browsergateway.service.IPluginManage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

/**
 * 扩展管理API
 */
@RestController
@RequestMapping(path = "/browsergw/extension", produces = MediaType.APPLICATION_JSON_VALUE)
public class ExtensionManageApi {
    
    private static final Logger log = LoggerFactory.getLogger(ExtensionManageApi.class);
    
    @Autowired
    private IPluginManage pluginManage;
    
    @Autowired
    private IChromeSet chromeSet;
    
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
    public BaseResponse<LoadExtensionResponse> loadExtension(@RequestBody LoadExtensionRequest request) {
        log.info("加载扩展请求: name={}, version={}, bucketName={}, extensionFilePath={}", 
                request.getName(), request.getVersion(), request.getBucketName(), request.getExtensionFilePath());
        
        try {
            // 1. 参数验证
            validateLoadExtensionRequest(request);
            
            // 2. 关闭所有现有浏览器实例（加载新扩展前必须关闭）
            log.info("关闭所有现有浏览器实例，准备加载新扩展");
            chromeSet.deleteAll();
            
            // 3. 调用插件管理服务加载扩展
            boolean success = pluginManage.loadExtension(request);
            
            if (!success) {
                log.error("扩展加载失败: name={}, version={}", request.getName(), request.getVersion());
                return BaseResponse.fail(ErrorCodeEnum.EXTENSION_LOAD_FAILED, 
                        "扩展加载失败，请检查扩展文件是否正确");
            }
            
            // 4. 构建响应
            LoadExtensionResponse response = new LoadExtensionResponse();
            response.setBucketName(request.getBucketName());
            response.setExtensionFilePath(request.getExtensionFilePath());
            
            log.info("扩展加载成功: name={}, version={}", request.getName(), request.getVersion());
            return BaseResponse.success(response);
            
        } catch (BusinessException e) {
            log.warn("加载扩展业务异常: {}", e.getErrorMessage(), e);
            return BaseResponse.fail(e.getErrorCode(), e.getErrorMessage());
        } catch (Exception e) {
            log.error("加载扩展失败", e);
            return BaseResponse.fail(ErrorCodeEnum.EXTENSION_LOAD_FAILED, 
                    "加载扩展失败: " + e.getMessage());
        }
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
    public BaseResponse<PluginInfoResponse> getPluginInfo() {
        log.info("获取插件信息请求");
        
        try {
            // 1. 调用插件管理服务获取插件信息
            PluginActive pluginActive = pluginManage.getPluginInfo();
            
            if (pluginActive == null) {
                log.warn("插件信息为空，可能未加载插件");
                return BaseResponse.fail(ErrorCodeEnum.PLUGIN_NOT_FOUND, "插件未加载");
            }
            
            // 2. 转换为响应对象
            PluginInfoResponse response = new PluginInfoResponse();
            response.setName(pluginActive.getName());
            response.setVersion(pluginActive.getVersion());
            response.setType(pluginActive.getType());
            response.setStatus(pluginActive.getStatus());
            response.setBucketName(pluginActive.getBucketName());
            response.setPackageName(pluginActive.getPackageName());
            response.setLoadTime(pluginActive.getLoadTime());
            
            log.info("获取插件信息成功: name={}, version={}, status={}", 
                    response.getName(), response.getVersion(), response.getStatus());
            return BaseResponse.success(response);
            
        } catch (Exception e) {
            log.error("获取插件信息失败", e);
            return BaseResponse.fail(ErrorCodeEnum.PLUGIN_NOT_FOUND, 
                    "获取插件信息失败: " + e.getMessage());
        }
    }
    
    /**
     * 验证加载扩展请求参数
     */
    private void validateLoadExtensionRequest(LoadExtensionRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, "请求参数不能为空");
        }
        
        if (StrUtil.isBlank(request.getName())) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, "插件名称(name)不能为空");
        }
        
        if (StrUtil.isBlank(request.getVersion())) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, "插件版本(version)不能为空");
        }
        
        if (StrUtil.isBlank(request.getBucketName())) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, "存储桶名称(bucketName)不能为空");
        }
        
        if (StrUtil.isBlank(request.getExtensionFilePath())) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, "扩展文件路径(extensionFilePath)不能为空");
        }
        
        // 验证文件路径格式（应该是.jar文件）
        if (!request.getExtensionFilePath().endsWith(".jar")) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, 
                    "扩展文件路径必须以.jar结尾");
        }
    }
}
