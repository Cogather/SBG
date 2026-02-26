package com.huawei.browsergateway.common.utils;

import com.github.luben.zstd.Zstd;
import com.github.luben.zstd.ZstdInputStream;
import com.github.luben.zstd.ZstdOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Zstd压缩工具类
 */
public class ZstdUtil {
    
    private static final Logger log = LoggerFactory.getLogger(ZstdUtil.class);
    
    /**
     * 压缩文件或目录
     * 
     * @param sourcePath 源文件或目录路径
     * @param targetPath 目标压缩文件路径
     * @throws IOException 压缩失败时抛出
     */
    public static void compress(String sourcePath, String targetPath) throws IOException {
        Path source = Paths.get(sourcePath);
        if (!Files.exists(source)) {
            throw new IOException("源文件或目录不存在: " + sourcePath);
        }
        
        // 创建目标文件的父目录
        Path target = Paths.get(targetPath);
        Path parent = target.getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }
        
        try (FileOutputStream fos = new FileOutputStream(targetPath);
             BufferedOutputStream bos = new BufferedOutputStream(fos);
             ZstdOutputStream zos = new ZstdOutputStream(bos)) {
            
            if (Files.isDirectory(source)) {
                // 压缩目录
                compressDirectory(source, zos);
            } else {
                // 压缩单个文件
                compressFile(source, zos);
            }
            
            zos.flush();
        }
        
        log.debug("压缩完成: {} -> {}", sourcePath, targetPath);
    }
    
    /**
     * 解压文件
     * 
     * @param sourcePath 源压缩文件路径
     * @param targetPath 目标解压目录路径
     * @throws IOException 解压失败时抛出
     */
    public static void decompress(String sourcePath, String targetPath) throws IOException {
        Path source = Paths.get(sourcePath);
        if (!Files.exists(source) || !Files.isRegularFile(source)) {
            throw new IOException("源压缩文件不存在: " + sourcePath);
        }
        
        // 创建目标目录
        Path target = Paths.get(targetPath);
        if (!Files.exists(target)) {
            Files.createDirectories(target);
        }
        
        try (FileInputStream fis = new FileInputStream(sourcePath);
             BufferedInputStream bis = new BufferedInputStream(fis);
             ZstdInputStream zis = new ZstdInputStream(bis);
             FileOutputStream fos = new FileOutputStream(new File(target.toFile(), "decompressed.tar"));
             BufferedOutputStream bos = new BufferedOutputStream(fos)) {
            
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = zis.read(buffer)) != -1) {
                bos.write(buffer, 0, bytesRead);
            }
            
            bos.flush();
        }
        
        log.debug("解压完成: {} -> {}", sourcePath, targetPath);
    }
    
    /**
     * 压缩目录
     */
    private static void compressDirectory(Path dir, ZstdOutputStream zos) throws IOException {
        // 这里简化实现，实际应该使用tar格式打包目录
        // 为了简化，我们只压缩目录下的所有文件
        Files.walk(dir)
            .filter(Files::isRegularFile)
            .forEach(file -> {
                try {
                    compressFile(file, zos);
                } catch (IOException e) {
                    throw new RuntimeException("压缩文件失败: " + file, e);
                }
            });
    }
    
    /**
     * 压缩单个文件
     */
    private static void compressFile(Path file, ZstdOutputStream zos) throws IOException {
        try (FileInputStream fis = new FileInputStream(file.toFile());
             BufferedInputStream bis = new BufferedInputStream(fis)) {
            
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = bis.read(buffer)) != -1) {
                zos.write(buffer, 0, bytesRead);
            }
        }
    }
    
    /**
     * 压缩字节数组
     * 
     * @param data 原始数据
     * @return 压缩后的数据
     */
    public static byte[] compress(byte[] data) {
        return Zstd.compress(data);
    }
    
    /**
     * 解压字节数组
     * 
     * @param compressedData 压缩后的数据
     * @param originalSize 原始数据大小
     * @return 解压后的数据
     */
    public static byte[] decompress(byte[] compressedData, int originalSize) {
        return Zstd.decompress(compressedData, originalSize);
    }
    
    /**
     * 压缩JSON文件
     * 用于压缩用户数据JSON文件
     * 
     * @param sourceJsonPath 源JSON文件路径
     * @param targetCompressedPath 目标压缩文件路径（通常以.zst结尾）
     * @throws IOException 压缩失败时抛出
     */
    public static void compressJson(String sourceJsonPath, String targetCompressedPath) throws IOException {
        Path source = Paths.get(sourceJsonPath);
        if (!Files.exists(source) || !Files.isRegularFile(source)) {
            throw new IOException("源JSON文件不存在: " + sourceJsonPath);
        }
        
        // 创建目标文件的父目录
        Path target = Paths.get(targetCompressedPath);
        Path parent = target.getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }
        
        try (FileInputStream fis = new FileInputStream(sourceJsonPath);
             BufferedInputStream bis = new BufferedInputStream(fis);
             FileOutputStream fos = new FileOutputStream(targetCompressedPath);
             BufferedOutputStream bos = new BufferedOutputStream(fos);
             ZstdOutputStream zos = new ZstdOutputStream(bos)) {
            
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = bis.read(buffer)) != -1) {
                zos.write(buffer, 0, bytesRead);
            }
            
            zos.flush();
        }
        
        log.debug("JSON文件压缩完成: {} -> {}", sourceJsonPath, targetCompressedPath);
    }
    
    /**
     * 解压JSON文件
     * 用于解压用户数据压缩文件
     * 
     * @param sourceCompressedPath 源压缩文件路径（通常以.zst结尾）
     * @param targetJsonPath 目标JSON文件路径
     * @throws IOException 解压失败时抛出
     */
    public static void decompressJson(String sourceCompressedPath, String targetJsonPath) throws IOException {
        Path source = Paths.get(sourceCompressedPath);
        if (!Files.exists(source) || !Files.isRegularFile(source)) {
            throw new IOException("源压缩文件不存在: " + sourceCompressedPath);
        }
        
        // 创建目标文件的父目录
        Path target = Paths.get(targetJsonPath);
        Path parent = target.getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }
        
        try (FileInputStream fis = new FileInputStream(sourceCompressedPath);
             BufferedInputStream bis = new BufferedInputStream(fis);
             ZstdInputStream zis = new ZstdInputStream(bis);
             FileOutputStream fos = new FileOutputStream(targetJsonPath);
             BufferedOutputStream bos = new BufferedOutputStream(fos)) {
            
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = zis.read(buffer)) != -1) {
                bos.write(buffer, 0, bytesRead);
            }
            
            bos.flush();
        }
        
        log.debug("JSON文件解压完成: {} -> {}", sourceCompressedPath, targetJsonPath);
    }
    
    /**
     * 流式压缩
     * 支持大文件的流式处理，避免内存占用过高
     * 
     * @param inputStream 输入流
     * @return 压缩后的输入流
     * @throws IOException 压缩失败时抛出
     */
    public static InputStream compressStream(InputStream inputStream) throws IOException {
        if (inputStream == null) {
            throw new IllegalArgumentException("输入流不能为null");
        }
        
        try {
            // 使用PipedInputStream和PipedOutputStream实现流式压缩
            PipedInputStream pipedInputStream = new PipedInputStream();
            PipedOutputStream pipedOutputStream = new PipedOutputStream(pipedInputStream);
            
            // 在后台线程中执行压缩
            Thread compressThread = new Thread(() -> {
                try (ZstdOutputStream zos = new ZstdOutputStream(pipedOutputStream)) {
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        zos.write(buffer, 0, bytesRead);
                    }
                    zos.flush();
                } catch (IOException e) {
                    log.error("流式压缩失败", e);
                    try {
                        pipedOutputStream.close();
                    } catch (IOException closeException) {
                        log.error("关闭输出流失败", closeException);
                    }
                } finally {
                    try {
                        pipedOutputStream.close();
                    } catch (IOException e) {
                        log.error("关闭输出流失败", e);
                    }
                }
            }, "ZstdCompressThread");
            
            compressThread.setDaemon(true);
            compressThread.start();
            
            return pipedInputStream;
            
        } catch (Exception e) {
            log.error("创建流式压缩流失败", e);
            throw new IOException("创建流式压缩流失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 流式解压
     * 支持大文件的流式处理，避免内存占用过高
     * 
     * @param inputStream 压缩的输入流
     * @return 解压后的输入流
     * @throws IOException 解压失败时抛出
     */
    public static InputStream decompressStream(InputStream inputStream) throws IOException {
        if (inputStream == null) {
            throw new IllegalArgumentException("输入流不能为null");
        }
        
        try {
            // 直接使用ZstdInputStream进行流式解压
            return new ZstdInputStream(inputStream);
        } catch (Exception e) {
            log.error("创建流式解压流失败", e);
            throw new IOException("创建流式解压流失败: " + e.getMessage(), e);
        }
    }
}
