package com.huawei.browsergateway.sdk;

import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import org.openqa.selenium.*;
import org.openqa.selenium.bidi.BiDi;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.chromium.ChromiumDriver;
import org.openqa.selenium.chromium.ChromiumNetworkConditions;
import org.openqa.selenium.devtools.DevTools;
import org.openqa.selenium.federatedcredentialmanagement.FederatedCredentialManagementDialog;
import org.openqa.selenium.interactions.Sequence;
import org.openqa.selenium.logging.EventType;
import org.openqa.selenium.print.PrintOptions;
import org.openqa.selenium.remote.*;
import org.openqa.selenium.virtualauthenticator.VirtualAuthenticator;
import org.openqa.selenium.virtualauthenticator.VirtualAuthenticatorOptions;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.logging.Level;

/**
 * Chromium 驱动代理实现。
 * 通过 CDP 服务提供与 Selenium 兼容的浏览器自动化接口。
 * 多数 Selenium 原生方法不支持，将抛出 UnsupportedOperationException。
 */
public class ChromiumDriverProxy extends ChromiumDriver implements WebDriver.TargetLocator {

    private static final Log log = LogFactory.get();
    private static final String UNSUPPORTED_MESSAGE = "ChromiumDriverProxy 不支持该操作";
    private static final String HISTORY_GO_SCRIPT = "window.history.go";
    private static final String HISTORY_LENGTH_SCRIPT = "window.history.length";

    /**
     * 用于 Selenium 兼容的命令执行器代理。
     */
    static class CommandExecutorProxy implements CommandExecutor {
        @Override
        public Response execute(Command command) {
            return null;
        }
    }

    private final BrowserDriver driver;
    private final DevToolsProxy devTools;
    private final WindowProxy webDriver;

    /**
     * 使用指定配置选项构造 ChromiumDriverProxy。
     *
     * @param options 浏览器配置选项
     */
    public ChromiumDriverProxy(BrowserOptions options) {
        super(new CommandExecutorProxy(), new ChromeOptions(), "goog:chromeOptions");
        this.driver = new BrowserDriver(options);
        this.devTools = new DevToolsProxy(driver);
        this.webDriver = new WindowProxy(driver);
    }

    /**
     * 获取代理的上下文 ID。
     *
     * @return 上下文 ID
     */
    public String getProxyContextId() {
        return driver.getContext().getId();
    }

    /**
     * 保存当前上下文的用户数据。
     */
    public void saveUserdata() {
        driver.saveUserdata();
    }

    /**
     * 使用标准消息抛出 UnsupportedOperationException。
     *
     * @throws UnsupportedOperationException 始终抛出
     */
    private void throwUnsupported() {
        throw new UnsupportedOperationException(UNSUPPORTED_MESSAGE);
    }

    @Override
    public Optional<DevTools> maybeGetDevTools() {
        return Optional.of(devTools);
    }

    @Override
    @Nonnull
    public String getWindowHandle() {
        return driver.getContext().getCurrent();
    }

    /**
     * 在当前页面执行 JavaScript。
     * 处理历史记录相关操作的特殊情况。
     *
     * @param script JavaScript 代码
     * @param args   脚本参数
     * @return 执行结果
     */
    @Override
    public Object executeScript(String script, Object... args) {
        log.info("执行脚本: {}", script);

        if (StrUtil.contains(script, HISTORY_GO_SCRIPT)) {
            handleHistoryGoScript();
            return null;
        }

        if (StrUtil.contains(script, HISTORY_LENGTH_SCRIPT)) {
            return 2L;
        }

        return driver.executeScript(script);
    }

    /**
     * 通过重置导航历史处理 window.history.go() 脚本。
     */
    private void handleHistoryGoScript() {
        driver.gotoUrl("about:blank");
        driver.executeCdp("Page.resetNavigationHistory", Map.of());
    }

    @Override
    public void get(String url) {
        driver.gotoUrl(url);
    }

    @Override
    public void quit() {
        driver.close();
    }

    @Override
    public Options manage() {
        return webDriver;
    }

    @Override
    public Map<String, Object> executeCdpCommand(String commandName, Map<String, Object> parameters) {
        return driver.executeCdp(commandName, parameters);
    }

    @Override
    public String getCurrentUrl() {
        return driver.getCurrentUrl();
    }

    @Override
    public void close() {
        driver.closeCurrentPage();
    }

    @Override
    public TargetLocator switchTo() {
        return this;
    }

    @Override
    public WebDriver window(String nameOrHandle) {
        log.info("忽略切换到窗口: {}", nameOrHandle);
        return this;
    }

    @Override
    public void perform(Collection<Sequence> actions) {
        log.info("忽略 perform: {}", actions);
    }

    /**
     * 根据定位器查找元素。
     * 仅支持 By.ByTagName 定位器。
     *
     * @param locator 元素定位器
     * @return WebElement 实例
     * @throws UnsupportedOperationException 定位器类型不支持时抛出
     */
    @Override
    public WebElement findElement(By locator) {
        if (locator instanceof By.ByTagName) {
            String tagName = extractTagName(locator);
            return driver.findElementByTagName(tagName);
        }
        throwUnsupported();
        return null;
    }

    /**
     * 从 By.ByTagName 定位器中提取标签名。
     *
     * @param locator By.ByTagName 定位器
     * @return 标签名字符串
     */
    private String extractTagName(By locator) {
        String prefix = "By.tagName: ";
        return locator.toString().substring(prefix.length());
    }

    @Override
    public Navigation navigate() {
        return new NavigationProxy(driver);
    }

    @Override
    public String toString() {
        return "ChromiumDriverProxy";
    }

    // Selenium 原生方法 - 不支持

    @Override
    public WebDriver frame(int index) {
        throwUnsupported();
        return null;
    }

    @Override
    public WebDriver frame(String nameOrId) {
        throwUnsupported();
        return null;
    }

    @Override
    public WebDriver frame(WebElement frameElement) {
        throwUnsupported();
        return null;
    }

    @Override
    public WebDriver parentFrame() {
        throwUnsupported();
        return null;
    }

    @Override
    public WebDriver newWindow(WindowType typeHint) {
        throwUnsupported();
        return null;
    }

    @Override
    public WebDriver defaultContent() {
        throwUnsupported();
        return null;
    }

    @Override
    public WebElement activeElement() {
        throwUnsupported();
        return null;
    }

    @Override
    public Alert alert() {
        throwUnsupported();
        return null;
    }

    @Override
    public ScriptKey pin(String script) {
        throwUnsupported();
        return null;
    }

    @Override
    public Set<ScriptKey> getPinnedScripts() {
        throwUnsupported();
        return null;
    }

    @Override
    public void unpin(ScriptKey key) {
        throwUnsupported();
    }

    @Override
    public Object executeScript(ScriptKey key, Object... args) {
        throwUnsupported();
        return null;
    }

    @Override
    public void setFileDetector(FileDetector detector) {
        throwUnsupported();
    }

    @Override
    public <X> void onLogEvent(EventType<X> kind) {
        throwUnsupported();
    }

    @Override
    public void register(Predicate<URI> whenThisMatches, Supplier<Credentials> useTheseCredentials) {
        throwUnsupported();
    }

    @Override
    public void launchApp(String id) {
        throwUnsupported();
    }

    @Override
    public Optional<BiDi> maybeGetBiDi() {
        throwUnsupported();
        return Optional.empty();
    }

    @Override
    public List<Map<String, String>> getCastSinks() {
        throwUnsupported();
        return null;
    }

    @Override
    public String getCastIssueMessage() {
        throwUnsupported();
        return null;
    }

    @Override
    public void selectCastSink(String deviceName) {
        throwUnsupported();
    }

    @Override
    public void startDesktopMirroring(String deviceName) {
        throwUnsupported();
    }

    @Override
    public void startTabMirroring(String deviceName) {
        throwUnsupported();
    }

    @Override
    public void stopCasting(String deviceName) {
        throwUnsupported();
    }

    @Override
    public void setPermission(String name, String value) {
        throwUnsupported();
    }

    @Override
    public ChromiumNetworkConditions getNetworkConditions() {
        throwUnsupported();
        return null;
    }

    @Override
    public void setNetworkConditions(ChromiumNetworkConditions networkConditions) {
        throwUnsupported();
    }

    @Override
    public void deleteNetworkConditions() {
        throwUnsupported();
    }

    @Override
    public void register(Supplier<Credentials> alwaysUseTheseCredentials) {
        throwUnsupported();
    }

    @Override
    public BiDi getBiDi() {
        throwUnsupported();
        return null;
    }

    @Override
    public SessionId getSessionId() {
        throwUnsupported();
        return null;
    }

    @Override
    protected void setSessionId(String opaqueKey) {
        throwUnsupported();
    }

    @Override
    protected void startSession(Capabilities capabilities) {
        // 空实现，用于兼容
    }

    @Override
    public ErrorHandler getErrorHandler() {
        throwUnsupported();
        return null;
    }

    @Override
    public void setErrorHandler(ErrorHandler handler) {
        throwUnsupported();
    }

    @Override
    public CommandExecutor getCommandExecutor() {
        throwUnsupported();
        return null;
    }

    @Override
    protected void setCommandExecutor(CommandExecutor executor) {
        throwUnsupported();
    }

    @Override
    public String getTitle() {
        throwUnsupported();
        return null;
    }

    @Override
    public <X> X getScreenshotAs(OutputType<X> outputType) throws WebDriverException {
        throwUnsupported();
        return null;
    }

    @Override
    public Pdf print(PrintOptions printOptions) throws WebDriverException {
        throwUnsupported();
        return null;
    }

    @Override
    public List<WebElement> findElements(By locator) {
        throwUnsupported();
        return null;
    }

    @Override
    public List<WebElement> findElements(SearchContext context, BiFunction<String, Object, CommandPayload> findCommand, By locator) {
        throwUnsupported();
        return null;
    }

    @Override
    protected void setFoundBy(SearchContext context, WebElement element, String by, String using) {
        throwUnsupported();
    }

    @Override
    public String getPageSource() {
        throwUnsupported();
        return null;
    }

    @Override
    @Nonnull
    public Set<String> getWindowHandles() {
        throwUnsupported();
        return null;
    }

    @Override
    public Object executeAsyncScript(String script, Object... args) {
        throwUnsupported();
        return null;
    }

    @Override
    public Script script() {
        throwUnsupported();
        return null;
    }

    @Override
    public Network network() {
        throwUnsupported();
        return null;
    }

    @Override
    protected JsonToWebElementConverter getElementConverter() {
        throwUnsupported();
        return null;
    }

    @Override
    protected void setElementConverter(JsonToWebElementConverter converter) {
        throwUnsupported();
    }

    @Override
    public void setLogLevel(Level level) {
        throwUnsupported();
    }

    @Override
    protected Response execute(CommandPayload payload) {
        throwUnsupported();
        return null;
    }

    @Override
    protected Response execute(String driverCommand, Map<String, ?> parameters) {
        throwUnsupported();
        return null;
    }

    @Override
    protected Response execute(String command) {
        throwUnsupported();
        return null;
    }

    @Override
    public void resetInputState() {
        throwUnsupported();
    }

    @Override
    public VirtualAuthenticator addVirtualAuthenticator(VirtualAuthenticatorOptions options) {
        throwUnsupported();
        return null;
    }

    @Override
    public void removeVirtualAuthenticator(VirtualAuthenticator authenticator) {
        throwUnsupported();
    }

    @Override
    public List<String> getDownloadableFiles() {
        throwUnsupported();
        return null;
    }

    @Override
    public void downloadFile(String fileName, Path targetLocation) throws IOException {
        throwUnsupported();
    }

    @Override
    public void deleteDownloadableFiles() {
        throwUnsupported();
    }

    @Override
    public void setDelayEnabled(boolean enabled) {
        throwUnsupported();
    }

    @Override
    public void resetCooldown() {
        throwUnsupported();
    }

    @Override
    public FederatedCredentialManagementDialog getFederatedCredentialManagementDialog() {
        throwUnsupported();
        return null;
    }

    @Override
    protected void log(SessionId sessionId, String commandName, Object toLog, When when) {
        throwUnsupported();
    }

    @Override
    public FileDetector getFileDetector() {
        throwUnsupported();
        return null;
    }

    @Override
    public void requireDownloadsEnabled(Capabilities capabilities) {
        throwUnsupported();
    }
}
