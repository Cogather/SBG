package com.huawei.browsergateway.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/** Unit tests for ZstdUtil */
class ZstdUtilTest {

    @TempDir
    Path tempDir;

    @Test
    void testCompressAndDecompress_roundTrip() throws IOException {
        Path jsonFile = tempDir.resolve("test.json");
        String content = "{\"key\":\"value\",\"num\":123}";
        Files.writeString(jsonFile, content);

        Path zstFile = tempDir.resolve("test.json.zst");
        Path outJson = tempDir.resolve("out.json");

        assertTrue(ZstdUtil.compressJson(jsonFile.toString(), zstFile.toString()), "compress should succeed");
        assertTrue(Files.exists(zstFile), "compressed file should exist");

        assertTrue(ZstdUtil.decompressJson(zstFile.toString(), outJson.toString()), "decompress should succeed");
        assertEquals(content, Files.readString(outJson), "decompressed content should match original");
    }

    @Test
    void testCompressJson_sourceNotFound() {
        assertFalse(ZstdUtil.compressJson("/nonexistent/test.json", "/tmp/out.zst"));
    }

    @Test
    void testCompressJson_nonJsonFile() throws IOException {
        Path txtFile = tempDir.resolve("test.txt");
        Files.writeString(txtFile, "hello");
        assertFalse(ZstdUtil.compressJson(txtFile.toString(), tempDir.resolve("out.zst").toString()));
    }

    @Test
    void testDecompressJson_sourceNotFound() {
        assertFalse(ZstdUtil.decompressJson("/nonexistent/test.zst", "/tmp/out.json"));
    }
}
