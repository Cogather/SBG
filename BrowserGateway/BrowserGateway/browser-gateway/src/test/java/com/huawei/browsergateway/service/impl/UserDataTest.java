package com.huawei.browsergateway.service.impl;

import com.huawei.browsergateway.service.IFileStorage;
import com.huawei.browsergateway.service.IRemote;
import com.huawei.browsergateway.util.ZstdUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserDataTest {

    @Mock
    private IFileStorage fileStorageService;

    @Mock
    private IRemote remote;

    @TempDir
    Path tempDir;

    @Test
    void downloadShouldReturnLocalPathWhenRemoteFileDoesNotExist() {
        UserData userData = new UserData(fileStorageService, tempDir.toString(), "user-1", "self", remote);
        String remotePath = Paths.get("userdata", "user-1", "userdata.json.zst").toString();
        when(fileStorageService.exist(remotePath)).thenReturn(false);

        String localPath = userData.download();

        assertEquals(tempDir.resolve("user-1").resolve("userdata.json").toFile().getAbsolutePath(), localPath);
        verify(fileStorageService, never()).downloadFile(anyString(), anyString());
    }

    @Test
    void uploadShouldSkipWhenCurrentInstanceIsNotOwner() {
        UserData userData = new UserData(fileStorageService, tempDir.toString(), "user-2", "self", remote);
        when(remote.getUserBind("user-2")).thenReturn(null);

        userData.upload();

        verify(fileStorageService, never()).uploadFile(anyString(), anyString());
        verify(fileStorageService, never()).deleteFile(anyString());
    }

    @Test
    void uploadShouldSkipWhenNeedUploadThrows() {
        UserData userData = new UserData(fileStorageService, tempDir.toString(), "user-2", "self", remote);
        when(remote.getUserBind("user-2")).thenThrow(new RuntimeException("bind failed"));

        userData.upload();

        verify(fileStorageService, never()).uploadFile(anyString(), anyString());
    }

    @Test
    void uploadShouldSkipWhenLocalFileMissing() {
        UserData userData = new UserData(fileStorageService, tempDir.toString(), "user-local-missing", "self", remote);
        UserBind userBind = new UserBind();
        userBind.setBrowserInstance("self");
        when(remote.getUserBind("user-local-missing")).thenReturn(userBind);

        userData.upload();

        verify(fileStorageService, never()).uploadFile(anyString(), anyString());
    }

    @Test
    void uploadShouldCompressAndUploadWhenCurrentInstanceOwnsUser() throws Exception {
        UserData userData = new UserData(fileStorageService, tempDir.toString(), "user-3", "self", remote);
        UserBind userBind = new UserBind();
        userBind.setBrowserInstance("self");
        when(remote.getUserBind("user-3")).thenReturn(userBind);

        Path userDir = tempDir.resolve("user-3");
        Files.createDirectories(userDir);
        Path localJson = userDir.resolve("userdata.json");
        Files.writeString(localJson, "{\"cookies\":[],\"origins\":[]}", StandardCharsets.UTF_8);

        String remotePath = Paths.get("userdata", "user-3", "userdata.json.zst").toString();
        when(fileStorageService.exist(remotePath)).thenReturn(true);

        userData.upload();

        verify(fileStorageService).deleteFile(remotePath);
        ArgumentCaptor<String> zipPathCaptor = ArgumentCaptor.forClass(String.class);
        verify(fileStorageService).uploadFile(zipPathCaptor.capture(), org.mockito.ArgumentMatchers.eq(remotePath));
        assertTrue(zipPathCaptor.getValue().endsWith("userdata.json.zst"));
        assertFalse(new File(zipPathCaptor.getValue()).exists());
    }

    @Test
    void downloadShouldDownloadAndDecompressWhenRemoteFileExists() throws Exception {
        UserData userData = new UserData(fileStorageService, tempDir.toString(), "user-download", "self", remote);
        String remotePath = Paths.get("userdata", "user-download", "userdata.json.zst").toString();
        when(fileStorageService.exist(remotePath)).thenReturn(true);

        Path sourceJson = tempDir.resolve("source.json");
        String json = "{\"cookies\":[],\"origins\":[]}";
        Files.writeString(sourceJson, json, StandardCharsets.UTF_8);
        Path sourceZst = tempDir.resolve("source.json.zst");
        assertTrue(ZstdUtil.compressJson(sourceJson.toString(), sourceZst.toString()));

        doAnswer(invocation -> {
            Path targetZip = Path.of(invocation.getArgument(0, String.class));
            Files.copy(sourceZst, targetZip);
            return null;
        }).when(fileStorageService).downloadFile(anyString(), anyString());

        String localPath = userData.download();

        assertEquals(tempDir.resolve("user-download").resolve("userdata.json").toFile().getAbsolutePath(), localPath);
        assertTrue(Files.exists(Path.of(localPath)));
        assertEquals(json, Files.readString(Path.of(localPath), StandardCharsets.UTF_8));
    }

    @Test
    void deleteShouldRemoveLocalAndRemoteUserData() throws Exception {
        UserData userData = new UserData(fileStorageService, tempDir.toString(), "user-4", "self", remote);
        Path userDir = tempDir.resolve("user-4");
        Files.createDirectories(userDir);
        Path localJson = userDir.resolve("userdata.json");
        Files.writeString(localJson, "{}", StandardCharsets.UTF_8);

        userData.delete();

        assertFalse(Files.exists(localJson));
        verify(fileStorageService).deleteFile(Paths.get("userdata", "user-4", "userdata.json.zst").toString());
    }
}
