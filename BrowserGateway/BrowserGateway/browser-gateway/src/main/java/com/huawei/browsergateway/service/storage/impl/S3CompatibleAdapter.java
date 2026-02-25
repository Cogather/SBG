package com.huawei.browsergateway.service.storage.impl;

import com.huawei.browsergateway.exception.storage.FileStorageException;
import com.huawei.browsergateway.service.storage.FileInfo;
import com.huawei.browsergateway.service.storage.StorageAdapter;
import com.huawei.browsergateway.service.storage.StorageConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.File;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;

/**
 * S3兼容存储适配器
 */
public class S3CompatibleAdapter implements StorageAdapter {
    
    private static final Logger log = LoggerFactory.getLogger(S3CompatibleAdapter.class);
    
    private S3Client s3Client;
    private S3Presigner s3Presigner;
    private String bucketName;
    private String region;
    private String endpoint;
    
    @Override
    public void init(StorageConfig config) throws FileStorageException {
        try {
            this.bucketName = config.getBucketName();
            this.region = config.getRegion();
            this.endpoint = config.getEndpoint();
            
            // 构建AWS凭证
            AwsBasicCredentials credentials = AwsBasicCredentials.create(
                config.getAccessKey(),
                config.getSecretKey()
            );
            
            // 配置S3客户端
            S3ClientBuilder builder = S3Client.builder()
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .region(Region.of(region));
            
            // 如果指定了endpoint，使用自定义endpoint（用于MinIO等）
            if (endpoint != null && !endpoint.trim().isEmpty()) {
                builder.endpointOverride(URI.create(endpoint));
                // MinIO需要禁用路径样式
                builder.serviceConfiguration(S3Configuration.builder()
                    .pathStyleAccessEnabled(true)
                    .build());
            }
            
            this.s3Client = builder.build();
            
            // 创建Presigner用于生成预签名URL
            S3Presigner.Builder presignerBuilder = S3Presigner.builder()
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .region(Region.of(region));
            
            if (endpoint != null && !endpoint.trim().isEmpty()) {
                presignerBuilder.endpointOverride(URI.create(endpoint));
            }
            
            this.s3Presigner = presignerBuilder.build();
            
            // 测试连接（检查bucket是否存在）
            try {
                HeadBucketRequest headBucketRequest = HeadBucketRequest.builder()
                    .bucket(bucketName)
                    .build();
                s3Client.headBucket(headBucketRequest);
                log.info("S3存储适配器初始化成功: bucket={}, endpoint={}", bucketName, endpoint);
            } catch (NoSuchBucketException e) {
                // Bucket不存在，尝试创建
                try {
                    CreateBucketRequest createBucketRequest = CreateBucketRequest.builder()
                        .bucket(bucketName)
                        .build();
                    s3Client.createBucket(createBucketRequest);
                    log.info("创建S3存储桶: {}", bucketName);
                } catch (Exception createEx) {
                    throw new FileStorageException("S3存储桶不存在且创建失败: " + bucketName, createEx);
                }
            }
            
        } catch (FileStorageException e) {
            throw e;
        } catch (Exception e) {
            throw new FileStorageException("初始化S3存储适配器失败", e);
        }
    }
    
    @Override
    public String uploadFile(String localPath, String remotePath) throws FileStorageException {
        try {
            File localFile = new File(localPath);
            if (!localFile.exists()) {
                throw new FileStorageException.UploadException(localPath, remotePath, "本地文件不存在");
            }
            
            // 构建ObjectMetadata
            PutObjectRequest.Builder requestBuilder = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(remotePath)
                .contentLength(localFile.length());
            
            // 设置ContentType
            String contentType = getContentType(localPath);
            if (contentType != null) {
                requestBuilder.contentType(contentType);
            }
            
            PutObjectRequest putRequest = requestBuilder.build();
            
            // 执行上传
            RequestBody requestBody = RequestBody.fromFile(localFile);
            s3Client.putObject(putRequest, requestBody);
            
            log.debug("文件上传成功: {} -> s3://{}/{}", localPath, bucketName, remotePath);
            return remotePath;
            
        } catch (FileStorageException e) {
            throw e;
        } catch (Exception e) {
            throw new FileStorageException.UploadException(localPath, remotePath, e);
        }
    }
    
    @Override
    public File downloadFile(String localPath, String remotePath) throws FileStorageException {
        try {
            // 创建目标文件和目录
            File targetFile = new File(localPath);
            File targetDir = targetFile.getParentFile();
            if (targetDir != null && !targetDir.exists()) {
                boolean created = targetDir.mkdirs();
                if (!created) {
                    throw new FileStorageException("创建目标目录失败: " + targetDir.getAbsolutePath());
                }
            }
            
            // 构建下载请求
            GetObjectRequest getRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(remotePath)
                .build();
            
            // 执行下载
            s3Client.getObject(getRequest, Paths.get(localPath));
            
            // 验证文件存在
            if (!targetFile.exists()) {
                throw new FileStorageException.DownloadException(localPath, remotePath, "下载后文件不存在");
            }
            
            log.debug("文件下载成功: s3://{}/{} -> {}", bucketName, remotePath, localPath);
            return targetFile;
            
        } catch (FileStorageException e) {
            throw e;
        } catch (Exception e) {
            throw new FileStorageException.DownloadException(localPath, remotePath, e);
        }
    }
    
    @Override
    public boolean deleteFile(String path) throws FileStorageException {
        try {
            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(path)
                .build();
            
            s3Client.deleteObject(deleteRequest);
            log.debug("文件删除成功: s3://{}/{}", bucketName, path);
            return true;
            
        } catch (Exception e) {
            throw new FileStorageException.DeleteException(path, e);
        }
    }
    
    @Override
    public boolean exist(String path) throws FileStorageException {
        try {
            HeadObjectRequest headRequest = HeadObjectRequest.builder()
                .bucket(bucketName)
                .key(path)
                .build();
            
            s3Client.headObject(headRequest);
            return true;
            
        } catch (NoSuchKeyException e) {
            return false;
        } catch (Exception e) {
            throw new FileStorageException("检查文件存在性失败: " + path, e);
        }
    }
    
    @Override
    public FileInfo getFileInfo(String path) throws FileStorageException {
        try {
            HeadObjectRequest headRequest = HeadObjectRequest.builder()
                .bucket(bucketName)
                .key(path)
                .build();
            
            HeadObjectResponse response = s3Client.headObject(headRequest);
            
            FileInfo fileInfo = new FileInfo();
            fileInfo.setPath(path);
            fileInfo.setName(Paths.get(path).getFileName().toString());
            fileInfo.setSize(response.contentLength());
            fileInfo.setLastModified(response.lastModified().toEpochMilli());
            
            // 设置扩展名
            String fileName = fileInfo.getName();
            int lastDotIndex = fileName.lastIndexOf('.');
            if (lastDotIndex > 0) {
                fileInfo.setExtension(fileName.substring(lastDotIndex + 1).toLowerCase());
            }
            
            return fileInfo;
            
        } catch (NoSuchKeyException e) {
            return null;
        } catch (Exception e) {
            throw new FileStorageException("获取文件信息失败: " + path, e);
        }
    }
    
    @Override
    public String getFileUrl(String path, int expireSeconds) throws FileStorageException {
        try {
            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofSeconds(expireSeconds))
                .getObjectRequest(GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(path)
                    .build())
                .build();
            
            PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);
            return presignedRequest.url().toString();
            
        } catch (Exception e) {
            throw new FileStorageException("生成文件访问URL失败: " + path, e);
        }
    }
    
    /**
     * 获取ContentType
     */
    private String getContentType(String filePath) {
        try {
            return Files.probeContentType(Paths.get(filePath));
        } catch (Exception e) {
            log.debug("无法获取ContentType: {}", filePath);
            return null;
        }
    }
    
    /**
     * 关闭资源
     */
    public void close() {
        if (s3Client != null) {
            s3Client.close();
        }
        if (s3Presigner != null) {
            s3Presigner.close();
        }
    }
}
