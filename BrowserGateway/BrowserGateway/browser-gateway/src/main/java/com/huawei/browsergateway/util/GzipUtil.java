package com.huawei.browsergateway.util;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.zip.GZIPInputStream;

public class GzipUtil {
    public static void unGzip(String inputFile, String outputDir) throws IOException {
        Path outputPath = Paths.get(outputDir);
        Files.createDirectories(outputPath);

        try (InputStream fi = Files.newInputStream(Paths.get(inputFile));
             InputStream gzi = new GZIPInputStream(fi);
             BufferedInputStream bi = new BufferedInputStream(gzi)) {

            byte[] buffer = new byte[1024];
            TarEntry entry;
            while ((entry = readNextEntry(bi)) != null) {
                Path target = Paths.get(outputPath.toString(), entry.name).normalize();

                // 安全校验：防止路径穿越攻击
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
                // 跳过填充字节（对齐到512字节边界）
                long skipBytes = (512 - (entry.size % 512)) % 512;
                bi.skipNBytes(skipBytes);
            }
        }
    }

    private static TarEntry readNextEntry(InputStream in) throws IOException {
        byte[] header = new byte[512];
        int read = in.readNBytes(header, 0, 512);
        if (read == 0) return null; // 文件结束
        if (read < 512) throw new EOFException("无效的tar头部");

        // 检查空块（结束标志）
        if (isZeroBlock(header)) return null;

        // 解析文件名（前100字节）
        String name = new String(header, 0, 100, StandardCharsets.UTF_8).trim();

        // 解析文件大小（八进制字符串转十进制）
        String sizeStr = new String(header, 124, 12, StandardCharsets.UTF_8).trim();
        long size = Long.parseLong(sizeStr, 8);

        // 检查类型：目录（'5'）或普通文件（'0'或'\0'）
        char type = (char) header[156];
        boolean isDir = type == '5' || name.endsWith("/");

        return new TarEntry(name, size, isDir);
    }

    private static boolean isZeroBlock(byte[] block) {
        for (byte b : block) {
            if (b != 0) return false;
        }
        return true;
    }

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
