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
 * Web元素实现类
 * 实现Selenium的WebElement接口，提供页面元素的操作能力
 * 支持元素的查找、属性获取、文本输入等操作
 */
public class WebElementImpl implements WebElement {

    private static final Logger log = LogManager.getLogger(WebElementImpl.class);

    /** 日期格式化器列表，支持多种日期格式 */
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

    /** 元素ID */
    private final String id;

    /** 元素预览HTML */
    private final String preview;

    /** 浏览器驱动实例 */
    private final BrowserDriver driver;

    /** 解析后的JSoup元素对象 */
    private Element element;

    /**
     * 从JSON字符串解析WebElement对象
     *
     * @param json   JSON字符串
     * @param driver 浏览器驱动实例
     * @return WebElement对象
     */
    public static WebElement parse(String json, BrowserDriver driver) {
        Request.Element element = JSONUtil.toBean(json, Request.Element.class);
        return new WebElementImpl(element.getId(), element.getPreview(), driver);
    }

    /**
     * 构造函数
     *
     * @param id      元素ID
     * @param preview 元素预览HTML
     * @param driver  浏览器驱动实例
     */
    public WebElementImpl(String id, String preview, BrowserDriver driver) {
        this.id = id;
        this.preview = preview;
        this.driver = driver;

        if ("node".equals(preview)) {
            this.element = null;
            return;
        }

        try {
            Document doc = Jsoup.parseBodyFragment(preview);
            this.element = doc.body().child(0);
        } catch (Exception e) {
            log.error("web element parse error, preview: {}", preview, e);
            this.element = null;
        }
    }

    @Override
    public void click() {
        // 空实现
    }

    @Override
    public void submit() {
        // 空实现
    }

    @Override
    public void sendKeys(CharSequence... keysToSend) {
        if (keysToSend == null || keysToSend.length == 0) {
            return;
        }

        CharSequence firstKey = keysToSend[0];
        if (firstKey.equals(Keys.CONTROL + "a") || firstKey.equals(Keys.DELETE)) {
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

        // 特殊处理：对于日期类型的input元素，需要转换日期格式
        // selenium和playwright对日期输入的处理方式不同
        if ("input".equalsIgnoreCase(this.getTagName()) && "date".equalsIgnoreCase(this.getAttribute("type"))) {
            inputContent = convertDate(inputContent);
        }

        String action = "send_key";
        String tagName = this.getTagName();
        String type = this.getAttribute("type");

        log.info("sendKeys: {}, tail: {}, tagName: {}, type: {}", inputContent,
                inputContent.length() > 0 ? inputContent.charAt(inputContent.length() - 1) : "",
                tagName, type);

        // 特殊处理：对于文件类型的input元素
        if ("input".equalsIgnoreCase(tagName) && "file".equalsIgnoreCase(type)) {
            if (FileUtil.exist(inputContent)) {
                action = "set_file";
            }
        }

        driver.executeElement(new Request.Action(id, action, inputContent));

        // 如果是文件上传操作，执行后删除临时文件
        if ("set_file".equals(action)) {
            FileUtil.del(inputContent);
        }
    }

    @Override
    public void clear() {
        // 空实现
    }

    @Override
    @Nonnull
    public String getTagName() {
        if (element == null) {
            return "";
        }
        return element.tagName();
    }

    @Override
    public @Nullable String getAttribute(String name) {
        if (element == null) {
            return "";
        }
        Attribute attribute = element.attribute(name);
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
        if (element == null) {
            return "";
        }
        return element.text();
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
     * 日期格式转换
     * 用于处理日期输入场景，selenium和playwright对日期格式的要求不同：
     * - selenium: 模拟用户键盘输入，格式要求宽松，可能因地区而异
     * - playwright: 直接设置值，只接受yyyy-MM-dd格式
     *
     * @param dateStr 输入的日期字符串（任意格式，selenium风格）
     * @return 转换后的日期字符串（yyyy-MM-dd格式，playwright风格）
     * @throws RuntimeException 如果输入不是有效的日期格式
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
                // 继续尝试下一个格式
            }
        }

        log.error("invalid input, not date format, input content: {}", dateStr);
        throw new RuntimeException("invalid input: not a valid date format");
    }
}
