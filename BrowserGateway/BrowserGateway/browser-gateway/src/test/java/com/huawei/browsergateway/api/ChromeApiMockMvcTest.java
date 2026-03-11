package com.huawei.browsergateway.api;

import com.huawei.browsergateway.config.Config;
import com.huawei.browsergateway.service.IChromeSet;
import com.huawei.browsergateway.service.IFileStorage;
import com.huawei.browsergateway.service.IRemote;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ChromeApiMockMvcTest {

    @Mock
    private IChromeSet chromeSet;

    @Mock
    private IFileStorage fs;

    @Mock
    private Config config;

    @Mock
    private IRemote remote;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ChromeApi chromeApi = new ChromeApi();
        ReflectionTestUtils.setField(chromeApi, "chromeSet", chromeSet);
        ReflectionTestUtils.setField(chromeApi, "fs", fs);
        ReflectionTestUtils.setField(chromeApi, "config", config);
        ReflectionTestUtils.setField(chromeApi, "remote", remote);

        when(config.getInnerMediaEndpoint()).thenReturn("127.0.0.1:30002");
        when(config.getUserDataPath()).thenReturn("target/mockmvc-userdata");
        when(config.getSelfAddr()).thenReturn("127.0.0.1:8080");

        mockMvc = MockMvcBuilders.standaloneSetup(chromeApi).build();
    }

    @Test
    void preOpenShouldReturnStructuredFailResponseForInvalidInput() throws Exception {
        MvcResult result = mockMvc.perform(post("/browsergw/browser/preOpen")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "imei": " ",
                                  "imsi": ""
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        assertTrue(response.contains("\"code\":500"));
        assertTrue(response.contains("\"message\":\"system error!\""));
    }

    @Test
    void preOpenShouldInvokeRemoteCreateChrome() throws Exception {
        MvcResult result = mockMvc.perform(post("/browsergw/browser/preOpen")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "imei": "imei",
                                  "imsi": "imsi",
                                  "factory": "factory"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        assertTrue(response.contains("\"code\":200"));
        assertTrue(response.contains("\"message\":\"success\""));
        assertTrue(response.contains("\"data\":\"success\""));

        verify(remote).createChrome(any(byte[].class), any(), isNull());
    }

    @Test
    void deleteUserDataShouldReturnSuccessResponse() throws Exception {
        MvcResult result = mockMvc.perform(delete("/browsergw/browser/userdata/delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "imei": "imei",
                                  "imsi": "imsi"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        assertTrue(response.contains("\"code\":200"));
        assertTrue(response.contains("\"message\":\"success\""));
        assertTrue(response.contains("\"imei\":\"imei\""));
        assertTrue(response.contains("\"imsi\":\"imsi\""));

        verify(fs).deleteFile(anyString());
    }
}
