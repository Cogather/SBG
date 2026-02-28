package com.huawei.browsergateway.util;

import com.github.luben.zstd.ZstdInputStream;
import com.github.luben.zstd.ZstdOutputStream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;

public class ZstdUtil {

    private static final Logger log = LogManager.getLogger(ZstdUtil.class);
    // 默认压缩级别 1-22
    private static final int COMPRESSION_LEVEL = 6;

    private static final int BUFFER_SIZE = 8192;

    /**
     * 压缩JSON文件
     * @param sourceJsonPath 源JSON文件路径
     * @param targetZstPath 压缩后的.zst文件路径
     * @return 压缩是否成功
     */
    public static boolean compressJson(String sourceJsonPath, String targetZstPath) {
        File sourceFile = new File(sourceJsonPath);

        // 验证源文件
        if (!sourceFile.exists() || !sourceFile.isFile()) {
            log.error("source file:{} invalid.", sourceJsonPath);
            return false;
        }

        if (!sourceJsonPath.toLowerCase().endsWith(".json")) {
            log.error("source file:{} is not a json file.", sourceJsonPath);
            return false;
        }

        try (InputStream in = new FileInputStream(sourceFile);
             OutputStream out = new FileOutputStream(targetZstPath);
             ZstdOutputStream zstdOut = new ZstdOutputStream(out, COMPRESSION_LEVEL)) {

            // 缓冲区复制数据
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                zstdOut.write(buffer, 0, bytesRead);
            }
            return true;
        } catch (IOException e) {
            System.err.println("JSON文件压缩失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 解压Zstd压缩的JSON文件
     * @param sourceZstPath 源.zst压缩文件路径
     * @param targetJsonPath 解压后的JSON文件路径
     * @return 解压是否成功
     */
    public static boolean decompressJson(String sourceZstPath, String targetJsonPath) {
        File sourceFile = new File(sourceZstPath);

        // 验证源文件
        if (!sourceFile.exists() || !sourceFile.isFile()) {
            log.error("source file:{} invalid.", sourceZstPath);
            return false;
        }

        try (InputStream in = new FileInputStream(sourceFile);
             ZstdInputStream zstdIn = new ZstdInputStream(in);
             OutputStream out = new FileOutputStream(targetJsonPath)) {

            // 缓冲区复制数据
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;
            while ((bytesRead = zstdIn.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
            return true;
        } catch (IOException e) {
            System.err.println("JSON文件解压失败: " + e.getMessage());
            return false;
        }
    }
}