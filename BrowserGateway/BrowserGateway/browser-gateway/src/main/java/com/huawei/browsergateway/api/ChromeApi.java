package com.huawei.browsergateway.api;

import cn.hutool.core.util.StrUtil;
import com.huawei.browsergateway.common.enums.ErrorCodeEnum;
import com.huawei.browsergateway.common.response.BaseResponse;
import com.huawei.browsergateway.common.utils.UserIdUtil;
import com.huawei.browsergateway.entity.request.DeleteUserDataRequest;
import com.huawei.browsergateway.entity.request.InitBrowserRequest;
import com.huawei.browsergateway.exception.common.BusinessException;
import com.huawei.browsergateway.service.IChromeSet;
import com.huawei.browsergateway.service.IRemote;
import com.huawei.browsergateway.service.impl.UserDataManager;
import com.huawei.browsergateway.tcpserver.common.Message;
import com.huawei.browsergateway.tcpserver.common.MessageType;
import com.huawei.browsergateway.tcpserver.common.TlvCodec;

import java.nio.ByteOrder;
import java.nio.file.Paths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

/**
 * Chrome浏览器管理API
 */
@RestController
@RequestMapping(path = "/browsergw/browser", produces = MediaType.APPLICATION_JSON_VALUE)
public class ChromeApi {
    
    private static final Logger log = LoggerFactory.getLogger(ChromeApi.class);
    
    @Autowired
    private IRemote remoteService;
    
    @Autowired
    private IChromeSet chromeSet;
    
    @Autowired(required = false)
    private UserDataManager userDataManager;
    
    @org.springframework.beans.factory.annotation.Value("${browsergw.workspace:/opt/host}")
    private String workspace;

    /**
     * 删除用户数据
     * DELETE /browsergw/browser/userdata/delete
     *
     * 功能说明：删除指定用户的浏览器实例和用户数据，如用户存在浏览器实例会先关闭再删除
     *
     * @param request 删除用户数据请求参数（包含imei和imsi）
     * @return 删除结果
     */
    @DeleteMapping(value = "/userdata/delete")
    public BaseResponse<DeleteUserDataRequest> deleteUserData(@RequestBody DeleteUserDataRequest request) {
        try {
            // 1. 参数验证
            validateDeleteUserDataRequest(request);

            log.info("删除用户数据请求: imei={}, imsi={}", request.getImei(), request.getImsi());

            // 2. 生成用户ID
            String userId = UserIdUtil.generateUserId(request.getImei(), request.getImsi());

            // 3. 检查浏览器实例是否存在
            if (chromeSet.get(userId) == null) {
                log.warn("浏览器实例不存在，可能已删除: userId={}", userId);
                // 即使不存在也返回成功，幂等操作
                return BaseResponse.success(request);
            }

            // 4. 删除浏览器实例（会先关闭再删除，包括上传用户数据）
            chromeSet.delete(userId);

            // 5. 删除远程和本地用户数据
            String userDataPath = Paths.get(workspace, userId).toString();
            if (userDataManager != null) {
                try {
                    log.info("开始删除用户数据: userId={}, path={}", userId, userDataPath);
                    userDataManager.deleteUserData(userId, userDataPath);
                    log.info("用户数据删除成功: userId={}", userId);
                } catch (Exception e) {
                    log.error("删除用户数据失败: userId={}", userId, e);
                    // 删除失败不影响整体流程，继续返回成功
                }
            } else {
                log.warn("UserDataManager未注入，跳过用户数据删除: userId={}", userId);
            }

            log.info("删除用户数据成功: userId={}", userId);
            return BaseResponse.success(request);

        } catch (BusinessException e) {
            log.warn("删除用户数据业务异常: {}", e.getErrorMessage(), e);
            return BaseResponse.fail(e.getErrorCode(), e.getErrorMessage());
        } catch (Exception e) {
            log.error("删除用户数据失败", e);
            return BaseResponse.fail(ErrorCodeEnum.USER_DATA_DELETE_FAILED,
                    "删除用户数据失败: " + e.getMessage());
        }
    }

    /**
     * 预开浏览器
     * POST /browsergw/browser/preOpen
     * 
     * 功能说明：预创建浏览器实例但不启动，用于提高用户首次连接的响应速度
     * 
     * @param request 浏览器初始化请求参数
     * @return 成功响应
     */
    @PostMapping(value = "/preOpen")
    public BaseResponse<String> preOpenBrowser(@RequestBody InitBrowserRequest request) {
        try {
            // 1. 参数验证
            validatePreOpenRequest(request);
            
            log.info("预开浏览器请求: imei={}, imsi={}", request.getImei(), request.getImsi());
            
            // 2. 生成用户ID
            String userId = UserIdUtil.generateUserId(request.getImei(), request.getImsi());
            
            // 3. 检查是否已存在实例
            if (chromeSet.get(userId) != null) {
                log.warn("浏览器实例已存在，跳过预开: userId={}", userId);
                return BaseResponse.success("浏览器实例已存在");
            }
            
            // 4. 构造TLV格式参数（预开浏览器需要特殊格式）
            // 将request转换为Message对象
            Message message = new Message();
            message.setType(MessageType.LOGIN);
            message.setImei(request.getImei());
            message.setImsi(request.getImsi());
            message.setLcdWidth(request.getLcdWidth());
            message.setLcdHeight(request.getLcdHeight());
            message.setAppType(request.getAppType());
            // 预开浏览器时，audType和token设置为空字符串
            message.setAudType("");
            message.setToken("");
            
            // 使用TlvCodec编组为TLV格式（大端序）
            byte[] encodeParam = TlvCodec.marshalWithByteOrder(message, ByteOrder.BIG_ENDIAN);
            
            // 5. 调用远程服务创建浏览器实例（传入TLV编码参数）
            remoteService.createChrome(encodeParam, request, null);
            
            log.info("预开浏览器成功: userId={}", userId);
            return BaseResponse.success("success");
            
        } catch (BusinessException e) {
            log.warn("预开浏览器业务异常: {}", e.getErrorMessage(), e);
            return BaseResponse.fail(e.getErrorCode(), e.getErrorMessage());
        } catch (Exception e) {
            log.error("预开浏览器失败", e);
            return BaseResponse.fail(ErrorCodeEnum.BROWSER_CREATE_FAILED, 
                    "预开浏览器失败: " + e.getMessage());
        }
    }
    
    /**
     * 验证预开浏览器请求参数
     */
    private void validatePreOpenRequest(InitBrowserRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, "请求参数不能为空");
        }
        
        if (StrUtil.isBlank(request.getImei()) && StrUtil.isBlank(request.getImsi())) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, "IMEI和IMSI不能同时为空");
        }
        
        // 验证屏幕尺寸参数（如果提供）
        if (request.getLcdWidth() != null && request.getLcdWidth() <= 0) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, "屏幕宽度必须大于0");
        }
        
        if (request.getLcdHeight() != null && request.getLcdHeight() <= 0) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, "屏幕高度必须大于0");
        }
    }
    
    /**
     * 验证删除用户数据请求参数
     */
    private void validateDeleteUserDataRequest(DeleteUserDataRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, "请求参数不能为空");
        }
        
        if (StrUtil.isBlank(request.getImei()) && StrUtil.isBlank(request.getImsi())) {
            throw new BusinessException(ErrorCodeEnum.BAD_REQUEST, "IMEI和IMSI不能同时为空");
        }
    }
}
