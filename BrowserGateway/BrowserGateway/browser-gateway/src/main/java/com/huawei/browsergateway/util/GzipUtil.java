package com.huawei.browsergateway.util;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.zip.GZIPInputStream;

/** tar.gz 解压工具类 */
public final class GzipUtil {

    private static final int BUFFER_SIZE = 8192;
    /** tar 头部固定长度 */
    private static final int TAR_HEADER_SIZE = 512;

    private GzipUtil() {}

    /**
     * 将 tar.gz 文件解压到指定目录
     *
     * @param inputFile 源 tar.gz 文件路径
     * @param outputDir 目标解压目录
     * @throws IOException 解压失败或检测到路径穿越攻击时抛出
     */
    public static void unGzip(String inputFile, String outputDir) throws IOException {
        Path outputPath = Paths.get(outputDir);
        Files.createDirectories(outputPath);

        try (InputStream fi = Files.newInputStream(Paths.get(inputFile));
             InputStream gzi = new GZIPInputStream(fi);
             BufferedInputStream bi = new BufferedInputStream(gzi)) {

            byte[] buffer = new byte[BUFFER_SIZE];
            TarEntry entry;
            while ((entry = readNextEntry(bi)) != null) {
                Path target = Paths.get(outputPath.toString(), entry.name).normalize();

                // 防止路径穿越攻击
                if (!target.startsWith(outputPath)) {
                    throw new IOException("非法路径: " + entry.name);
                }

                if (entry.isDirectory) {
                    Files.createDirectories(target);
                } else {
                    Files.createDirectories(target.getParent());
                    try (OutputStream os = Files.newOutputStream(target, StandardOpenOption.CREATE_NEW)) {
                        int remaining = (int) entry.size;
                        while (remaining > 0) {
                            int read = bi.read(buffer, 0, Math.min(buffer.length, remaining));
                            if (read == -1) break;
                            os.write(buffer, 0, read);
                            remaining -= read;
                        }
                    }
                }
                // 跳过填充字节（对齐到 512 字节边界）
                bi.skipNBytes((TAR_HEADER_SIZE - (entry.size % TAR_HEADER_SIZE)) % TAR_HEADER_SIZE);
            }
        }
    }

    /** 从输入流读取下一个 tar 条目头部，文件结束返回 null */
    private static TarEntry readNextEntry(InputStream in) throws IOException {
        byte[] header = new byte[TAR_HEADER_SIZE];
        int read = in.readNBytes(header, 0, TAR_HEADER_SIZE);
        if (read == 0) return null;
        if (read < TAR_HEADER_SIZE) throw new EOFException("无效的 tar 头部");
        if (isZeroBlock(header)) return null;

        // 文件名：前 100 字节
        String name = new String(header, 0, 100, StandardCharsets.UTF_8).trim();
        // 文件大小：八进制字符串（偏移 124，长度 12）
        String sizeStr = new String(header, 124, 12, StandardCharsets.UTF_8).trim();
        long size = Long.parseLong(sizeStr, 8);
        // 类型标志：偏移 156，'5' 为目录
        boolean isDir = header[156] == '5' || name.endsWith("/");

        return new TarEntry(name, size, isDir);
    }

    /** 判断是否为全零块（tar 结束标志） */
    private static boolean isZeroBlock(byte[] block) {
        for (byte b : block) {
            if (b != 0) return false;
        }
        return true;
    }

    /** tar 条目元数据 */
    static class TarEntry {
        final String name;
        final long size;
        final boolean isDirectory;

        TarEntry(String name, long size, boolean isDirectory) {
            this.name = name;
            this.size = size;
            this.isDirectory = isDirectory;
        }
    }
}
