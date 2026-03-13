package com.huawei.browsergateway.sdk;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebElementImplTest {

    @Mock
    private BrowserDriver driver;

    // --- parse ---

    @Test
    void testParse_validJson_returnsElement() {
        String json = "{\"id\":\"elem-1\",\"preview\":\"<div class=\\\"test\\\">hello</div>\"}";
        WebElement element = WebElementImpl.parse(json, driver);
        assertNotNull(element);
        assertEquals("div", element.getTagName());
    }

    // --- getTagName ---

    @Test
    void testGetTagName_withValidPreview() {
        WebElementImpl element = new WebElementImpl("id1", "<input type=\"text\">", driver);
        assertEquals("input", element.getTagName());
    }

    @Test
    void testGetTagName_withNodePreview_returnsEmpty() {
        WebElementImpl element = new WebElementImpl("id1", "node", driver);
        assertEquals("", element.getTagName());
    }

    // --- getAttribute ---

    @Test
    void testGetAttribute_existingAttribute() {
        WebElementImpl element = new WebElementImpl("id1", "<input type=\"text\" name=\"user\">", driver);
        assertEquals("text", element.getAttribute("type"));
        assertEquals("user", element.getAttribute("name"));
    }

    @Test
    void testGetAttribute_missingAttribute_returnsEmpty() {
        WebElementImpl element = new WebElementImpl("id1", "<input type=\"text\">", driver);
        assertEquals("", element.getAttribute("nonexistent"));
    }

    @Test
    void testGetAttribute_nullEle_returnsEmpty() {
        WebElementImpl element = new WebElementImpl("id1", "node", driver);
        assertEquals("", element.getAttribute("type"));
    }

    // --- getText ---

    @Test
    void testGetText_returnsTextContent() {
        WebElementImpl element = new WebElementImpl("id1", "<span>hello world</span>", driver);
        assertEquals("hello world", element.getText());
    }

    // --- sendKeys ---

    @Test
    void testSendKeys_regularText_callsExecuteElement() {
        WebElementImpl element = new WebElementImpl("id1", "<input type=\"text\">", driver);
        element.sendKeys("hello");

        ArgumentCaptor<Request.Action> captor = ArgumentCaptor.forClass(Request.Action.class);
        verify(driver).executeElement(captor.capture());
        assertEquals("send_key", captor.getValue().getAction());
        assertEquals("hello", captor.getValue().getValue());
        assertEquals("id1", captor.getValue().getElementId());
    }

    @Test
    void testSendKeys_ctrlA_ignored() {
        WebElementImpl element = new WebElementImpl("id1", "<input type=\"text\">", driver);
        element.sendKeys(Keys.CONTROL + "a");
        verify(driver, never()).executeElement(any());
    }

    @Test
    void testSendKeys_delete_ignored() {
        WebElementImpl element = new WebElementImpl("id1", "<input type=\"text\">", driver);
        element.sendKeys(Keys.DELETE);
        verify(driver, never()).executeElement(any());
    }

    @Test
    void testSendKeys_blankString_ignored() {
        WebElementImpl element = new WebElementImpl("id1", "<input type=\"text\">", driver);
        element.sendKeys("   ");
        verify(driver, never()).executeElement(any());
    }

    // --- sendKeys date conversion ---

    @Test
    void testSendKeys_dateInput_mmddyyyy_convertsToIso() {
        WebElementImpl element = new WebElementImpl("id1", "<input type=\"date\">", driver);
        element.sendKeys("01/15/2023");

        ArgumentCaptor<Request.Action> captor = ArgumentCaptor.forClass(Request.Action.class);
        verify(driver).executeElement(captor.capture());
        assertEquals("2023-01-15", captor.getValue().getValue());
    }

    @Test
    void testSendKeys_dateInput_alreadyIsoFormat_unchanged() {
        WebElementImpl element = new WebElementImpl("id1", "<input type=\"date\">", driver);
        element.sendKeys("2023-06-20");

        ArgumentCaptor<Request.Action> captor = ArgumentCaptor.forClass(Request.Action.class);
        verify(driver).executeElement(captor.capture());
        assertEquals("2023-06-20", captor.getValue().getValue());
    }

    @Test
    void testSendKeys_dateInput_invalidFormat_throwsException() {
        WebElementImpl element = new WebElementImpl("id1", "<input type=\"date\">", driver);
        assertThrows(RuntimeException.class, () -> element.sendKeys("not-a-date"));
    }

    // --- sendKeys file input ---

    @Test
    void testSendKeys_fileInput_fileNotExist_usesSendKey() {
        WebElementImpl element = new WebElementImpl("id1", "<input type=\"file\">", driver);
        element.sendKeys("/nonexistent/path/file.txt");

        ArgumentCaptor<Request.Action> captor = ArgumentCaptor.forClass(Request.Action.class);
        verify(driver).executeElement(captor.capture());
        assertEquals("send_key", captor.getValue().getAction());
    }

    // --- getSize ---

    @Test
    void testGetSize_returnsDimension() {
        Type.Size size = new Type.Size();
        size.setWidth(100.0);
        size.setHeight(200.0);
        when(driver.getSize("id1")).thenReturn(size);

        WebElementImpl element = new WebElementImpl("id1", "<div>", driver);
        Dimension dim = element.getSize();
        assertEquals(100, dim.getWidth());
        assertEquals(200, dim.getHeight());
    }
}
