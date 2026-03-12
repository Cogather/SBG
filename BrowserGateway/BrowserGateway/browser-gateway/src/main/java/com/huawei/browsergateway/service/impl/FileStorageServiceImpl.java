package com.huawei.browsergateway.service.impl;

import cn.hutool.json.JSONUtil;
import com.huawei.browsergateway.entity.BaseResponse;
import com.huawei.browsergateway.service.ICse;
import com.huawei.browsergateway.service.IFileStorage;
import com.huawei.browsergateway.util.HttpUtil;
import lombok.Data;
import org.apache.hc.client5.http.classic.methods.HttpDelete;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 文件存储服务实现，通过 HTTP 与远端文件服务交互，支持上传、下载、删除和存在性检查
 */
@Service
public class FileStorageServiceImpl implements IFileStorage {

    private static final Logger log = LogManager.getLogger(FileStorageServiceImpl.class);

    private static final String FILE_URL_PATTERN = "/file/v1/%s/%s";
    private static final String EXIST_URL_PATTERN = "/file/v1/%s/%s/exist";

    @Autowired
    private ICse cse;

    @Override
    public void uploadFile(String localFilePath, String remoteUrl) {
        S3Path s3Path = parseS3Url(remoteUrl);
        File file = Path.of(localFilePath).toFile();
        String url = buildFileUrl(s3Path);

        HttpEntity fileEntity = MultipartEntityBuilder.create()
                .addBinaryBody("file", file, ContentType.DEFAULT_BINARY, file.getName())
                .build();
        HttpPost httpPost = new HttpPost(url);
        httpPost.setEntity(fileEntity);

        try {
            HttpUtil.getHttpClient().execute(httpPost, response -> {
                if (response.getCode() == 200) {
                    log.info("upload {} success", remoteUrl);
                } else {
                    dealFileHttpError("upload", s3Path, response);
                }
                return null;
            });
        } catch (IOException e) {
            log.error("upload {} failed", s3Path, e);
        }
    }

    @Override
    public void downloadFile(String localFilePath, String remoteUrl) {
        S3Path s3Path = parseS3Url(remoteUrl);
        String url = buildFileUrl(s3Path);

        try {
            HttpUtil.getHttpClient().execute(new HttpGet(url), response -> {
                int status = response.getCode();
                HttpEntity entity = response.getEntity();
                if (status == 200 && entity != null) {
                    try (FileOutputStream out = new FileOutputStream(localFilePath)) {
                        entity.writeTo(out);
                    }
                } else {
                    dealFileHttpError("download", s3Path, response);
                    EntityUtils.consume(response.getEntity());
                }
                return null;
            });
        } catch (IOException e) {
            log.error("download {} failed", s3Path, e);
        }
    }

    @Override
    public void deleteFile(String remoteUrl) {
        S3Path s3Path = parseS3Url(remoteUrl);
        String url = buildFileUrl(s3Path);

        try {
            HttpUtil.getHttpClient().execute(new HttpDelete(url), response -> {
                if (response.getCode() == 200) {
                    log.info("delete {} success", remoteUrl);
                } else {
                    dealFileHttpError("delete", s3Path, response);
                }
                return null;
            });
        } catch (IOException e) {
            log.error("delete {} failed", s3Path, e);
        }
    }

    @Override
    public boolean exist(String remoteUrl) {
        S3Path s3Path = parseS3Url(remoteUrl);
        String url = buildExistUrl(s3Path);
        boolean result = false;
        try {
            result = HttpUtil.getHttpClient().execute(new HttpGet(url), response -> {
                if (response.getCode() == 200) {
                    return true;
                }
                if (response.getCode() != 404) {
                    dealFileHttpError("exist", s3Path, response);
                }
                return false;
            });
        } catch (IOException e) {
            log.error("exist {} failed", s3Path, e);
        }
        return result;
    }

    /** 构建文件操作 URL */
    private String buildFileUrl(S3Path s3Path) {
        return "http://" + cse.getReportEndpoint()
                + String.format(FILE_URL_PATTERN, s3Path.getBucket(), s3Path.getName());
    }

    /** 构建文件存在性检查 URL */
    private String buildExistUrl(S3Path s3Path) {
        return "http://" + cse.getReportEndpoint()
                + String.format(EXIST_URL_PATTERN, s3Path.getBucket(), s3Path.getName());
    }

    /**
     * 将远端路径解析为 S3Path（bucket + 扁平化文件名）。
     * 路径中第一段为 bucket，其余部分用 "_" 拼接为文件名。
     */
    private static S3Path parseS3Url(String url) {
        Path path = Paths.get(url);
        String bucketName = path.getName(0).toString();
        String name = path.subpath(1, path.getNameCount()).toString().replace("/", "_");
        return new S3Path(bucketName, name);
    }

    /** 统一处理文件操作的 HTTP 错误响应 */
    private static void dealFileHttpError(String method, S3Path file, ClassicHttpResponse response) {
        try {
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                BaseResponse badResponse = JSONUtil.toBean(entity.toString(), BaseResponse.class);
                log.error("{} failed, file:{}, code:{}, msg:{}", method, file,
                        badResponse.getCode(), badResponse.getMessage());
            } else {
                log.error("{} failed, file:{}, code:{}", method, file, response.getCode());
            }
        } catch (Exception e) {
            log.error("{} failed, file:{}, http status:{}", method, file, response.getCode());
        }
    }

    /** S3 路径值对象，包含 bucket 名称和文件名 */
    @Data
    private static class S3Path {
        private final String bucket;
        private final String name;

        @Override
        public String toString() {
            return bucket + "/" + name;
        }
    }
}
