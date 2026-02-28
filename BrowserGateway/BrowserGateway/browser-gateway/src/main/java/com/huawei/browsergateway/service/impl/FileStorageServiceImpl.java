package com.huawei.browsergateway.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.json.JSONUtil;
import com.huawei.browsergateway.entity.BaseResponse;
import com.huawei.browsergateway.service.ICse;
import com.huawei.browsergateway.service.IFileStorage;

import com.huawei.browsergateway.util.HttpUtil;
import com.huawei.browsergateway.util.UserdataSlimmer;
import com.huawei.browsergateway.util.ZstdUtil;
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
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 文件存储服务实现类
 */
@Service
public class FileStorageServiceImpl implements IFileStorage {
    private static final Logger log = LogManager.getLogger(FileStorageServiceImpl.class);
    @Autowired
    private ICse   cse;

    final private static String url = "/file/v1/%s/%s";
    final private static String existUrl = "/file/v1/%s/%s/exist";
    final private static String sizeUrl = "/file/v1/%s/%s/size";
    final private static String fileUrl = "/file/v1/%s/%s/url";

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

    @Override
    public Map<String, String> batchUpload(Map<String, String> fileMap) {
        Map<String, String> result = new HashMap<>();
        for (Map.Entry<String, String> entry : fileMap.entrySet()) {
            String localPath = entry.getKey();
            String remotePath = entry.getValue();
            try {
                uploadFile(localPath, remotePath);
                // 注意：当前 uploadFile 方法没有返回 URL，这里先返回远程路径
                // 如果需要返回访问 URL，需要修改 uploadFile 方法或调用 getFileUrl
                result.put(localPath, remotePath);
            } catch (Exception e) {
                log.error("batch upload failed for localPath: {}, remotePath: {}", localPath, remotePath, e);
                result.put(localPath, null);
            }
        }
        return result;
    }

    @Override
    public Map<String, Boolean> batchDelete(Set<String> paths) {
        Map<String, Boolean> result = new HashMap<>();
        for (String path : paths) {
            try {
                deleteFile(path);
                result.put(path, true);
            } catch (Exception e) {
                log.error("batch delete failed for path: {}", path, e);
                result.put(path, false);
            }
        }
        return result;
    }

    @Override
    public long getFileSize(String path) {
        S3Path s3Path = parseS3Url(path);
        String endpoint = cse.getReportEndpoint();
        String url = "http://" + endpoint + String.format(FileStorageServiceImpl.sizeUrl, s3Path.getBucket(), s3Path.getName());

        HttpGet httpGet = new HttpGet(url);
        try {
            Long result = HttpUtil.getHttpClient().execute(httpGet, response -> {
                if (response.getCode() == 200) {
                    HttpEntity entity = response.getEntity();
                    if (entity != null) {
                        try {
                            String content = EntityUtils.toString(entity);
                            return Long.parseLong(content.trim());
                        } catch (Exception e) {
                            log.error("parse file size failed for path: {}", path, e);
                            return -1L;
                        }
                    }
                } else {
                    dealFileHttpError("getFileSize", s3Path, response);
                }
                return -1L;
            });
            return result != null ? result : -1L;
        } catch (IOException e) {
            log.error("get file size failed for path: {}", path, e);
            return -1L;
        }
    }

    @Override
    public String getFileUrl(String path, int expireSeconds) {
        S3Path s3Path = parseS3Url(path);
        String endpoint = cse.getReportEndpoint();
        String url = "http://" + endpoint + String.format(FileStorageServiceImpl.fileUrl, s3Path.getBucket(), s3Path.getName()) + "?expire=" + expireSeconds;

        HttpGet httpGet = new HttpGet(url);
        try {
            String result = HttpUtil.getHttpClient().execute(httpGet, response -> {
                if (response.getCode() == 200) {
                    HttpEntity entity = response.getEntity();
                    if (entity != null) {
                        try {
                            return EntityUtils.toString(entity).trim();
                        } catch (Exception e) {
                            log.error("parse file url failed for path: {}", path, e);
                            return null;
                        }
                    }
                } else {
                    dealFileHttpError("getFileUrl", s3Path, response);
                }
                return null;
            });
            return result;
        } catch (IOException e) {
            log.error("get file url failed for path: {}", path, e);
            return null;
        }
    }

    @Override
    public String downloadUserData(String userDataPath, String userId, String serverAddr) {
        log.info("Downloading Chrome user data for user {}", userId);
        File localUserData = FileUtil.file(userDataPath, userId, "userdata.json");
        String remoteURL = Paths.get("userdata", userId, "userdata.json.zst").toString();

        if (!exist(remoteURL)) {
            log.info("userdata not exist in remote storage, use local path, userId:{}", userId);
            return localUserData.getAbsolutePath();
        }

        if (!localUserData.getParentFile().exists()) {
            FileUtil.mkdir(localUserData.getParentFile());
            FileUtil.touch(localUserData);
        }
        File localZip = new File(localUserData.getParent(), "userdata.json.zst");
        try {
            downloadFile(localZip.getAbsolutePath(), remoteURL);
            ZstdUtil.decompressJson(localZip.getAbsolutePath(), localUserData.getAbsolutePath());

            if (localZip.exists()) {
                FileUtil.del(localZip);
            }
        } catch (Exception e) {
            log.error("download user data error! userId:{}", userId, e);
        }

        return localUserData.getAbsolutePath();
    }

    @Override
    public boolean uploadUserData(String userDataPath, String userId, String serverAddr) {
        log.info("Uploading Chrome user data for user {}", userId);
        try {
            File localUserData = FileUtil.file(userDataPath, userId, "userdata.json");
            if (!localUserData.exists()) {
                log.warn("browser userdata path is not exist: {}", localUserData);
                return false;
            }

            UserdataSlimmer.slimInplace(localUserData);
            //压缩
            File localZip = new File(localUserData.getParent(), "userdata.json.zst");
            try {
                ZstdUtil.compressJson(localUserData.getAbsolutePath(), localZip.getAbsolutePath());
            } catch (Exception e) {
                log.error("Compression failed: {}", localUserData.getAbsolutePath(), e);
                return false;
            }

            String remoteURL = Paths.get("userdata", userId, "userdata.json.zst").toString();
            long startUpload = System.currentTimeMillis();
            if (exist(remoteURL)) {
                deleteFile(remoteURL);
            }
            uploadFile(localZip.getAbsolutePath(), remoteURL);
            log.info("upload userdata to remote storage end, cost {} ms", System.currentTimeMillis() - startUpload);

            if (localZip.exists()) {
                FileUtil.del(localZip);
            }
            return true;
        } catch (Exception e) {
            log.error("upload user data error! userId:{}", userId, e);
            return false;
        }
    }

    private static void dealFileHttpError(String method, S3Path file, ClassicHttpResponse response) {
        try {
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                String entityStr = EntityUtils.toString(entity);
                BaseResponse badResponse = JSONUtil.toBean(entityStr, BaseResponse.class);
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