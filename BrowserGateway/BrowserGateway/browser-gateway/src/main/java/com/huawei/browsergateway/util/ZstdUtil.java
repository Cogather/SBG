package com.huawei.browsergateway.util;

import com.github.luben.zstd.ZstdInputStream;
import com.github.luben.zstd.ZstdOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

/** Zstd 压缩/解压工具类，仅支持 JSON 文件 */
public final class ZstdUtil {

    private static final Logger log = LoggerFactory.getLogger(ZstdUtil.class);

    /** 压缩级别（1-22） */
    private static final int COMPRESSION_LEVEL = 6;
    private static final int BUFFER_SIZE = 8192;

    private ZstdUtil() {}

    /**
     * 将 JSON 文件压缩为 Zstd 格式
     *
     * @param sourceJsonPath 源 JSON 文件路径
     * @param targetZstPath  目标 .zst 文件路径
     * @return 压缩成功返回 {@code true}，否则返回 {@code false}
     */
    public static boolean compressJson(String sourceJsonPath, String targetZstPath) {
        File sourceFile = new File(sourceJsonPath);
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
            pipe(in, zstdOut);
            return true;
        } catch (IOException e) {
            log.error("compress json file failed: {}", sourceJsonPath, e);
            return false;
        }
    }

    /**
     * 将 Zstd 压缩文件解压为 JSON 文件
     *
     * @param sourceZstPath  源 .zst 文件路径
     * @param targetJsonPath 目标 JSON 文件路径
     * @return 解压成功返回 {@code true}，否则返回 {@code false}
     */
    public static boolean decompressJson(String sourceZstPath, String targetJsonPath) {
        File sourceFile = new File(sourceZstPath);
        if (!sourceFile.exists() || !sourceFile.isFile()) {
            log.error("source file:{} invalid.", sourceZstPath);
            return false;
        }
        try (InputStream in = new FileInputStream(sourceFile);
             ZstdInputStream zstdIn = new ZstdInputStream(in);
             OutputStream out = new FileOutputStream(targetJsonPath)) {
            pipe(zstdIn, out);
            return true;
        } catch (IOException e) {
            log.error("decompress json file failed: {}", sourceZstPath, e);
            return false;
        }
    }

    /** 将输入流数据写入输出流 */
    private static void pipe(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        int bytesRead;
        while ((bytesRead = in.read(buffer)) != -1) {
            out.write(buffer, 0, bytesRead);
        }
    }
}
