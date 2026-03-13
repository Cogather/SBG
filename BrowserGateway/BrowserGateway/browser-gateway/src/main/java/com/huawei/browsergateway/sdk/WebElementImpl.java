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
 * WebElement实现类，用于浏览器自动化
 * 提供元素交互功能，包括点击、输入和属性访问
 */
public class WebElementImpl implements WebElement {

    private static final Logger log = LogManager.getLogger(WebElementImpl.class);
    private static final String NODE_PREVIEW = "node";
    private static final String INPUT_TAG = "input";
    private static final String DATE_TYPE = "date";
    private static final String FILE_TYPE = "file";
    private static final String SEND_KEY_ACTION = "send_key";
    private static final String SET_FILE_ACTION = "set_file";

    private final String id;
    private final String preview;
    private final BrowserDriver driver;
    private Element ele;

    /**
     * 解析JSON字符串创建WebElement实例
     *
     * @param json   包含元素数据的JSON字符串
     * @param driver 浏览器驱动实例
     * @return WebElement实例
     */
    public static WebElement parse(String json, BrowserDriver driver) {
        Request.Element element = JSONUtil.toBean(json, Request.Element.class);
        return new WebElementImpl(element.getId(), element.getPreview(), driver);
    }

    /**
     * 构造WebElementImpl实例
     *
     * @param id      元素ID
     * @param preview 元素HTML预览
     * @param driver  浏览器驱动实例
     */
    public WebElementImpl(String id, String preview, BrowserDriver driver) {
        this.id = id;
        this.preview = preview;
        this.driver = driver;
        this.ele = parsePreview(preview);
    }

    /**
     * 解析HTML预览字符串为Element对象
     *
     * @param preview HTML预览字符串
     * @return 解析后的Element对象，解析失败返回null
     */
    private Element parsePreview(String preview) {
        if (NODE_PREVIEW.equals(preview)) {
            return null;
        }
        try {
            Document doc = Jsoup.parseBodyFragment(preview);
            return doc.body().child(0);
        } catch (Exception e) {
            log.error("解析Web元素预览失败: {}", preview, e);
            return null;
        }
    }

    @Override
    public void click() {
        // 未实现
    }

    @Override
    public void submit() {
        // 未实现
    }

    /**
     * 向元素发送按键输入
     * 处理日期输入和文件上传的特殊情况
     *
     * @param keysToSend 要发送的按键
     */
    @Override
    public void sendKeys(CharSequence... keysToSend) {
        if (shouldIgnoreKeys(keysToSend)) {
            return;
        }

        String inputContent = buildInputContent(keysToSend);
        if (inputContent.isEmpty()) {
            return;
        }

        String processedContent = processInputForDateType(inputContent);
        String action = determineAction(processedContent);

        logSendKeys(processedContent);
        executeAction(action, processedContent);
    }

    /**
     * 检查是否应忽略按键（Ctrl+A或Delete）
     *
     * @param keysToSend 要检查的按键
     * @return 如果应忽略返回true
     */
    private boolean shouldIgnoreKeys(CharSequence[] keysToSend) {
        if (keysToSend.length == 0) {
            return true;
        }
        CharSequence firstKey = keysToSend[0];
        return firstKey.equals(Keys.CONTROL + "a") || firstKey.equals(Keys.DELETE);
    }

    /**
     * 从按键序列构建输入内容
     *
     * @param keysToSend 要发送的按键
     * @return 拼接后的输入字符串
     */
    private String buildInputContent(CharSequence[] keysToSend) {
        StringBuilder input = new StringBuilder();
        for (CharSequence key : keysToSend) {
            input.append(key);
        }
        return input.toString().trim();
    }

    /**
     * 为日期类型输入处理输入内容
     * 将各种日期格式转换为Playwright要求的yyyy-MM-dd格式
     *
     * @param inputContent 原始输入内容
     * @return 处理后的输入内容
     */
    private String processInputForDateType(String inputContent) {
        if (isDateInput()) {
            return convertDate(inputContent);
        }
        return inputContent;
    }

    /**
     * 检查此元素是否为日期输入框
     *
     * @return 如果是input[type=date]返回true
     */
    private boolean isDateInput() {
        return INPUT_TAG.equalsIgnoreCase(getTagName())
                && DATE_TYPE.equalsIgnoreCase(getAttribute("type"));
    }

    /**
     * 根据元素类型和输入内容确定操作类型
     *
     * @param inputContent 输入内容
     * @return 操作类型（send_key或set_file）
     */
    private String determineAction(String inputContent) {
        if (isFileInput() && FileUtil.exist(inputContent)) {
            return SET_FILE_ACTION;
        }
        return SEND_KEY_ACTION;
    }

    /**
     * 检查此元素是否为文件输入框
     *
     * @return 如果是input[type=file]返回true
     */
    private boolean isFileInput() {
        return INPUT_TAG.equalsIgnoreCase(getTagName())
                && FILE_TYPE.equalsIgnoreCase(getAttribute("type"));
    }

    /**
     * 记录sendKeys操作详情
     *
     * @param inputContent 输入内容
     */
    private void logSendKeys(String inputContent) {
        log.info("sendKeys: {}, tagName: {}, type: {}",
                inputContent, getTagName(), getAttribute("type"));
    }

    /**
     * 在元素上执行操作
     *
     * @param action  操作类型
     * @param content 操作内容
     */
    private void executeAction(String action, String content) {
        driver.executeElement(new Request.Action(id, action, content));
        if (SET_FILE_ACTION.equals(action)) {
            FileUtil.del(content);
        }
    }

    @Override
    public void clear() {
        // 未实现
    }

    @Override
    @Nonnull
    public String getTagName() {
        return ele == null ? "" : ele.tagName();
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
        return ele == null ? "" : ele.text();
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
     * 将日期字符串从各种格式转换为yyyy-MM-dd格式
     * 这是必要的，因为Selenium模拟键盘输入，格式要求宽松，
     * 而Playwright直接设置值，只接受yyyy-MM-dd格式
     *
     * @param dateStr 任意支持格式的输入日期字符串
     * @return yyyy-MM-dd格式的日期字符串
     * @throws RuntimeException 如果日期格式无效
     */
    private static String convertDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }

        for (SimpleDateFormat formatter : DATE_FORMATTERS) {
            try {
                formatter.setLenient(false);
                Date date = formatter.parse(dateStr.trim());
                return new SimpleDateFormat("yyyy-MM-dd").format(date);
            } catch (ParseException ignored) {
                // 尝试下一个格式化器
            }
        }

        log.error("无效的日期格式，输入: {}", dateStr);
        throw new RuntimeException("无效的日期格式: " + dateStr);
    }

    /**
     * 支持的日期格式模式
     * 涵盖不同地区使用的常见日期格式
     */
    private static final List<SimpleDateFormat> DATE_FORMATTERS = new ArrayList<>();

    static {
        // ISO格式
        DATE_FORMATTERS.add(new SimpleDateFormat("yyyy-MM-dd"));

        // 美国格式
        DATE_FORMATTERS.add(new SimpleDateFormat("MM-dd-yyyy"));
        DATE_FORMATTERS.add(new SimpleDateFormat("MM/dd/yyyy"));
        DATE_FORMATTERS.add(new SimpleDateFormat("MM.dd.yyyy"));

        // 欧洲格式
        DATE_FORMATTERS.add(new SimpleDateFormat("dd-MM-yyyy"));
        DATE_FORMATTERS.add(new SimpleDateFormat("dd/MM/yyyy"));
        DATE_FORMATTERS.add(new SimpleDateFormat("dd.MM.yyyy"));

        // 其他格式
        DATE_FORMATTERS.add(new SimpleDateFormat("yyyy/MM/dd"));
        DATE_FORMATTERS.add(new SimpleDateFormat("yyyy.MM.dd"));

        // 月份名称格式
        DATE_FORMATTERS.add(new SimpleDateFormat("dd-MMM-yyyy", Locale.ENGLISH));
        DATE_FORMATTERS.add(new SimpleDateFormat("MMM-dd-yyyy", Locale.ENGLISH));
    }
}

