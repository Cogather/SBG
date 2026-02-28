package com.huawei.browsergateway.sdk;

import cn.hutool.core.io.FileUtil;
import cn.hutool.json.JSONUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jspecify.annotations.Nullable;
import org.openqa.selenium.*;

import javax.annotation.Nonnull;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
/**
 * Web元素实现
 */
public class WebElementImpl implements WebElement {

    private static final Logger log = LogManager.getLogger(WebElementImpl.class);
    private final String id;
    private final String preview;
    private final BrowserDriver driver;
    private Element ele;

    public static WebElement parse(String json, BrowserDriver driver) {
        Request.Element element = JSONUtil.toBean(json, Request.Element.class);
        return new WebElementImpl(element.getId(), element.getPreview(), driver);
    }

    public WebElementImpl(String id, String preview, BrowserDriver driver) {
        this.id = id;
        this.preview = preview;
        this.driver = driver;
        if ("node".equals(preview)) {
            ele = null;
            return;
        }
        try {
            Document doc = Jsoup.parseBodyFragment(preview);
            ele = doc.body().child(0);
        } catch (Exception e) {
            log.error("web element parse error, preview: {}", preview);
            ele = null;
        }
    }

    @Override
    public void click() {

    }

    @Override
    public void submit() {

    }

    @Override
    public void sendKeys(CharSequence... keysToSend) {
        CharSequence charSequence = keysToSend[0];
        if (charSequence.equals(Keys.CONTROL + "a") || charSequence.equals(Keys.DELETE)) {
            return;
        }

        StringBuilder input = new StringBuilder();
        for (CharSequence key : keysToSend) {
            input.append(key);
        }
        String inputContent = input.toString().trim();
        if (inputContent.isEmpty()) {
            return;
        }

        //special: for <input type = 'date'>, playwright is different from selenium
        if ("input".equalsIgnoreCase(this.getTagName()) && "date".equalsIgnoreCase(this.getAttribute("type"))) {
            inputContent = convertDate(inputContent);
        }
        String action = "send_key";
        String tagName = this.getTagName();
        String type = this.getAttribute("type");
        log.info("sendKeys: {}, tail:{}, tagName: {}, type: {}", inputContent
                , inputContent.charAt(inputContent.length() - 1), tagName, type);
        if ("input".equalsIgnoreCase(tagName) && "file".equalsIgnoreCase(type)) {
            if (FileUtil.exist(inputContent)) {
                action = "set_file";
            }
        }

        driver.executeElement(new Request.Action(id, action, inputContent));
        if ("set_file".equals(action)) {
            FileUtil.del(inputContent);
        }
    }

    @Override
    public void clear() {

    }

    @Override
    @Nonnull
    public String getTagName() {
        if (ele == null) {
            return "";
        }
        return ele.tagName();
    }

    @Override
    public @Nullable String getAttribute(String name) {
        if (ele == null) {
            return "";
        }
        Attribute attribute = ele.attribute(name);
        return attribute == null ? "" : attribute.getValue();
    }

    @Override
    public boolean isSelected() {
        return false;
    }

    @Override
    public boolean isEnabled() {
        return false;
    }

    @Override
    @Nonnull
    public String getText() {
        return ele.text();
    }

    @Override
    public List<WebElement> findElements(By by) {
        return List.of();
    }

    @Override
    public WebElement findElement(By by) {
        return null;
    }

    @Override
    public boolean isDisplayed() {
        return false;
    }

    @Override
    public Point getLocation() {
        return null;
    }

    @Override
    public Dimension getSize() {
        Type.Size size = driver.getSize(id);
        return new Dimension((int) size.getWidth(), (int) size.getHeight());
    }

    @Override
    public Rectangle getRect() {
        return null;
    }

    @Override
    public String getCssValue(String propertyName) {
        return "";
    }

    @Override
    public <X> X getScreenshotAs(OutputType<X> target) throws WebDriverException {
        return null;
    }

    /**
     * for date input scenario , selenium webElement.sendKeys() VS playwright elementHandle.fill()
     * selenium => simulates user keyboard input, the format requirements are loose and may vary according to regions.
     * playwright => directly sets the value, Only yyyy-MM-dd is accepted.
     * @param dateStr input date str, any format, selenium
     * @return yyyy-MM-dd, playwright
     */
    private static String convertDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }

        for (SimpleDateFormat formatter : DATE_FORMATTERS) {
            try {
                formatter.setLenient(false);
                Date date = formatter.parse(dateStr.trim());
                SimpleDateFormat targetFormatter = new SimpleDateFormat("yyyy-MM-dd");
                return targetFormatter.format(date);
            } catch (ParseException ignored) {

            }
        }

        log.error("invalid input, not date format, input content:{}", dateStr);
        throw new RuntimeException("invalid input");
    }
    private static final List<SimpleDateFormat> DATE_FORMATTERS = new ArrayList<>();
    static {
        DATE_FORMATTERS.add(new SimpleDateFormat("yyyy-MM-dd"));
        DATE_FORMATTERS.add(new SimpleDateFormat("MM-dd-yyyy"));
        DATE_FORMATTERS.add(new SimpleDateFormat("dd-MM-yyyy"));
        DATE_FORMATTERS.add(new SimpleDateFormat("yyyy/MM/dd"));
        DATE_FORMATTERS.add(new SimpleDateFormat("dd/MM/yyyy"));
        DATE_FORMATTERS.add(new SimpleDateFormat("MM/dd/yyyy"));
        DATE_FORMATTERS.add(new SimpleDateFormat("yyyy.MM.dd"));
        DATE_FORMATTERS.add(new SimpleDateFormat("dd.MM.yyyy"));
        DATE_FORMATTERS.add(new SimpleDateFormat("MM.dd.yyyy"));

        DATE_FORMATTERS.add(new SimpleDateFormat("dd-MMM-yyyy", Locale.ENGLISH));
        DATE_FORMATTERS.add(new SimpleDateFormat("MMM-dd-yyyy", Locale.ENGLISH));
    }
}
