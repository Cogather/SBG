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


@Service
public class FileStorageServiceImpl implements IFileStorage {
    private static final Logger log = LogManager.getLogger(FileStorageServiceImpl.class);
    @Autowired
    private ICse cse;

    final private static String url = "/file/v1/%s/%s";
    final private static String existUrl = "/file/v1/%s/%s/exist";

    @Override
    public void uploadFile(String localFilePath, String remoteUrl) {
        S3Path s3Path = parseS3Url(remoteUrl);
        File file = Path.of(localFilePath).toFile();
        String endpoint = cse.getReportEndpoint();
        String url = "http://" + endpoint + String.format(FileStorageServiceImpl.url, s3Path.getBucket(), s3Path.getName());

        HttpEntity fileEntity = MultipartEntityBuilder.create().addBinaryBody("file", file, ContentType.DEFAULT_BINARY, file.getName()).build();
        HttpPost httpPost = new HttpPost(url);
        httpPost.setEntity(fileEntity);
        try {
            HttpUtil.getHttpClient().execute(httpPost, response -> {
                if (response.getCode() == 200) {
                    log.info("upload {} success", remoteUrl);
                    return null;
                }
                dealFileHttpError("upload", s3Path, response);
                return null;
            });
        } catch (IOException e) {
            log.error("upload {} failed", s3Path, e);
        }
    }

    @Override
    public void downloadFile(String localFilePath, String remoteUrl) {
        S3Path s3Path = parseS3Url(remoteUrl);
        String endpoint = cse.getReportEndpoint();
        String url = "http://" + endpoint + String.format(FileStorageServiceImpl.url, s3Path.getBucket(), s3Path.getName());

        HttpGet httpGet = new HttpGet(url);
        try {
            HttpUtil.getHttpClient().execute(httpGet, response -> {
                int status = response.getCode();
                HttpEntity entity = response.getEntity();
                if (status == 200 && entity != null) {
                    // 2. 直接获取输入流，写入目标文件
                    try (FileOutputStream out = new FileOutputStream(localFilePath)) {
                        entity.writeTo(out);
                    }
                } else {
                    dealFileHttpError("download", s3Path, response);
                    EntityUtils.consume(response.getEntity());
                }
                return null; // ResponseHandler 返回 Void
            });
        } catch (IOException e) {
            log.error("download {} failed", s3Path, e);
        }
    }

    @Override
    public void deleteFile(String remoteUrl) {
        S3Path s3Path = parseS3Url(remoteUrl);
        String endpoint = cse.getReportEndpoint();
        String url = "http://" + endpoint + String.format(FileStorageServiceImpl.url, s3Path.getBucket(), s3Path.getName());
        HttpDelete httpDelete = new HttpDelete(url);
        try {
            HttpUtil.getHttpClient().execute(httpDelete, response -> {
                if (response.getCode() == 200)  {
                    log.info("delete {} success", remoteUrl);
                    return null;
                }
                dealFileHttpError("delete", s3Path, response);
                return null;
            });
        } catch (IOException e) {
            log.error("delete {} failed", s3Path, e);
        }
    }

    @Override
    public boolean exist(String remoteUrl) {
        S3Path s3Path = parseS3Url(remoteUrl);
        String endpoint = cse.getReportEndpoint();
        String url = "http://" + endpoint + String.format(FileStorageServiceImpl.existUrl, s3Path.getBucket(), s3Path.getName());

        HttpGet httpGet = new HttpGet(url);
        boolean result = false;
        try {
            result = HttpUtil.getHttpClient().execute(httpGet, response -> {
                if (response.getCode() == 200) {
                    return true;
                }
                if (response.getCode() != 404) {
                    dealFileHttpError("exist", s3Path, response);
                }
                return false;
            });
        } catch (IOException e) {
            log.error("delete {} failed", s3Path, e);
        }
        return result;
    }

    private static S3Path parseS3Url(String url) {
        Path path = Paths.get(url);
        String bucketName = path.getName(0).toString();
        //将除第一个标识bucket之后的路径转换为一个文件名
        String name = path.subpath(1, path.getNameCount()).toString().replace("/", "_");
        return new S3Path(bucketName, name);
    }


    @Data
    private static class S3Path {
        private final String bucket;
        private final String name;

        @Override
        public String toString() {
            return bucket + "/" + name;
        }
    }

    private static void dealFileHttpError(String method, S3Path file, ClassicHttpResponse response) {
        try {
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                BaseResponse badResponse = JSONUtil.toBean(entity.toString(), BaseResponse.class);
                log.error("{} failed, file:{}, code:{}, msg:{}", method, file
                        , badResponse.getCode(), badResponse.getMessage());
            } else {
                log.error("{} failed, file:{}, code:{}", method, file, response.getCode());
            }
        } catch (Exception e) {
            log.error("{} failed, file:{}, http status:{}", method, file, response.getCode());
        }
    }

}
