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

public class ChromiumDriverProxy extends ChromiumDriver implements WebDriver.TargetLocator {

    static class CommandExecutorProxy implements CommandExecutor {
        @Override
        public Response execute(Command command) {
            return null;
        }
    }

    private static final Log log = LogFactory.get();

    private final BrowserDriver driver;
    private final DevToolsProxy devTools;
    private final WindowProxy webDriver;

    public ChromiumDriverProxy(BrowserOptions options) {
        super(new CommandExecutorProxy(), new ChromeOptions(), "goog:chromeOptions");
        driver = new BrowserDriver(options);
        devTools = new DevToolsProxy(driver);
        webDriver = new WindowProxy(driver);
    }

    public String getProxyContextId() {
        return driver.getContext().getId();
    }

    public void saveUserdata() {
        driver.saveUserdata();
    }

    @Override
    public Optional<DevTools> maybeGetDevTools() {
        return Optional.of(devTools);
    }

    @Override
    @Nonnull
    public String getWindowHandle() {
        Type.Page currentPage = driver.getContext().getCurrentPage();
        return currentPage != null ? currentPage.getId() : "";
    }

    @Override
    public Object executeScript(String script, Object... args) {
        log.info("chromium proxy execute script: {}", script);
        if (StrUtil.contains(script,"window.history.go")) {
            // fallback操作手动执行
            driver.gotoUrl("about:blank");
            driver.executeCdp("Page.resetNavigationHistory", Map.of());
            return null;
        }

        if (StrUtil.contains(script, "window.history.length")) {
            return 2L;
        }
        return driver.executeScript(script);
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


    // 关闭当前页面
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
        log.info("ignore switch to window: {}", nameOrHandle);
        return this;
    }


    @Override
    public void perform(Collection<Sequence> actions) {
        log.info("ignore perform: {}", actions);
    }


    /***************************************************selenium 原生******************************************************/

    @Override
    public WebDriver frame(int index) {
        throw new UnsupportedOperationException("chromium proxy is not support");
    }

    @Override
    public WebDriver frame(String nameOrId) {
        throw new UnsupportedOperationException("chromium proxy is not support");
    }

    @Override
    public WebDriver frame(WebElement frameElement) {
        throw new UnsupportedOperationException("chromium proxy is not support");
    }

    @Override
    public WebDriver parentFrame() {
        throw new UnsupportedOperationException("chromium proxy is not support");
    }


    @Override
    public WebDriver newWindow(WindowType typeHint) {
        throw new UnsupportedOperationException("chromium proxy is not support");
    }

    @Override
    public WebDriver defaultContent() {
        throw new UnsupportedOperationException("chromium proxy is not support");
    }

    @Override
    public WebElement activeElement() {
        throw new UnsupportedOperationException("chromium proxy is not support");
    }

    @Override
    public Alert alert() {
        throw new UnsupportedOperationException("chromium proxy is not support");
    }

    @Override
    public ScriptKey pin(String script) {
        throw new UnsupportedOperationException("chromium proxy is not support");
    }

    @Override
    public Set<ScriptKey> getPinnedScripts() {
        throw new UnsupportedOperationException("chromium proxy is not support");
    }

    @Override
    public void unpin(ScriptKey key) {
        throw new UnsupportedOperationException("chromium proxy is not support");
    }

    @Override
    public Object executeScript(ScriptKey key, Object... args) {
        throw new UnsupportedOperationException("chromium proxy is not support");
    }

    @Override
    public void setFileDetector(FileDetector detector) {
        throw new UnsupportedOperationException("chromium proxy is not support");
    }

    @Override
    public <X> void onLogEvent(EventType<X> kind) {
        throw new UnsupportedOperationException("chromium proxy is not support");
    }

    @Override
    public void register(Predicate<URI> whenThisMatches, Supplier<Credentials> useTheseCredentials) {
        throw new UnsupportedOperationException("chromium proxy is not support");
    }

    @Override
    public void launchApp(String id) {
        throw new UnsupportedOperationException("chromium proxy is not support");
    }

    @Override
    public Optional<BiDi> maybeGetBiDi() {
        throw new UnsupportedOperationException("chromium proxy is not support");
    }

    @Override
    public List<Map<String, String>> getCastSinks() {
        throw new UnsupportedOperationException("chromium proxy is not support");
    }

    @Override
    public String getCastIssueMessage() {
        throw new UnsupportedOperationException("chromium proxy is not support");
    }

    @Override
    public void selectCastSink(String deviceName) {
        throw new UnsupportedOperationException("chromium proxy is not support");
    }

    @Override
    public void startDesktopMirroring(String deviceName) {
        throw new UnsupportedOperationException("chromium proxy is not support");
    }

    @Override
    public void startTabMirroring(String deviceName) {
        throw new UnsupportedOperationException("chromium proxy is not support");
    }

    @Override
    public void stopCasting(String deviceName) {
        throw new UnsupportedOperationException("chromium proxy is not support");
    }

    @Override
    public void setPermission(String name, String value) {
        throw new UnsupportedOperationException("chromium proxy is not support");
    }

    @Override
    public ChromiumNetworkConditions getNetworkConditions() {
        throw new UnsupportedOperationException("chromium proxy is not support");
    }

    @Override
    public void setNetworkConditions(ChromiumNetworkConditions networkConditions) {
        throw new UnsupportedOperationException("chromium proxy is not support");
    }

    @Override
    public void deleteNetworkConditions() {
        throw new UnsupportedOperationException("chromium proxy is not support");
    }

    @Override
    public void register(Supplier<Credentials> alwaysUseTheseCredentials) {
        throw new UnsupportedOperationException("chromium proxy is not support");
    }

    @Override
    public BiDi getBiDi() {
        throw new UnsupportedOperationException("chromium proxy is not support");
    }

    @Override
    public SessionId getSessionId() {
        throw new UnsupportedOperationException("chromium proxy is not support");
    }

    @Override
    protected void setSessionId(String opaqueKey) {
        throw new UnsupportedOperationException("chromium proxy is not support");
    }

    @Override
    protected void startSession(Capabilities capabilities) {

    }

    @Override
    public ErrorHandler getErrorHandler() {
        throw new UnsupportedOperationException("chromium proxy is not support");
    }

    @Override
    public void setErrorHandler(ErrorHandler handler) {
        throw new UnsupportedOperationException("chromium proxy is not support");
    }

    @Override
    public CommandExecutor getCommandExecutor() {
        throw new UnsupportedOperationException("chromium proxy is not support");
    }

    @Override
    protected void setCommandExecutor(CommandExecutor executor) {
        throw new UnsupportedOperationException("chromium proxy is not support");
    }

    @Override
    public String getTitle() {
        throw new UnsupportedOperationException("chromium proxy is not support");
    }

    @Override
    public <X> X getScreenshotAs(OutputType<X> outputType) throws WebDriverException {
        throw new UnsupportedOperationException("chromium proxy is not support");
    }

    @Override
    public Pdf print(PrintOptions printOptions) throws WebDriverException {
        throw new UnsupportedOperationException("chromium proxy is not support");
    }

    @Override
    public WebElement findElement(By locator) {
        if (locator instanceof By.ByTagName) {
            //By.ByTagName类中，字段tagName私有无法直接获取，从toString结果中提取。
            String prefix = "By.tagName: ";
            String  tagName = locator.toString().substring(prefix.length());
            return driver.findElementByTagName(tagName);
        }
        throw new UnsupportedOperationException("chromium proxy is not support");
    }

    @Override
    public List<WebElement> findElements(By locator) {
        throw new UnsupportedOperationException("chromium proxy is not support");
    }

    @Override
    public List<WebElement> findElements(SearchContext context, BiFunction<String, Object, CommandPayload> findCommand, By locator) {
        throw new UnsupportedOperationException("chromium proxy is not support");
    }

    @Override
    protected void setFoundBy(SearchContext context, WebElement element, String by, String using) {
        throw new UnsupportedOperationException("chromium proxy is not support");
    }

    @Override
    public String getPageSource() {
        throw new UnsupportedOperationException("chromium proxy is not support");
    }

    @Override
    @Nonnull
    public Set<String> getWindowHandles() {
        throw new UnsupportedOperationException("chromium proxy is not support");
    }

    @Override
    public Object executeAsyncScript(String script, Object... args) {
        throw new UnsupportedOperationException("chromium proxy is not support");
    }


    @Override
    public Navigation navigate() {
        return new NavigationProxy(driver);
    }

    @Override
    public Script script() {
        throw new UnsupportedOperationException("chromium proxy is not support");
    }

    @Override
    public Network network() {
        throw new UnsupportedOperationException("chromium proxy is not support");
    }

    @Override
    protected JsonToWebElementConverter getElementConverter() {
        throw new UnsupportedOperationException("chromium proxy is not support");
    }

    @Override
    protected void setElementConverter(JsonToWebElementConverter converter) {
        throw new UnsupportedOperationException("chromium proxy is not support");
    }

    @Override
    public void setLogLevel(Level level) {
        throw new UnsupportedOperationException("chromium proxy is not support");
    }

    @Override
    protected Response execute(CommandPayload payload) {
        throw new UnsupportedOperationException("chromium proxy is not support");
    }

    @Override
    protected Response execute(String driverCommand, Map<String, ?> parameters) {
        throw new UnsupportedOperationException("chromium proxy is not support");
    }

    @Override
    protected Response execute(String command) {
        throw new UnsupportedOperationException("chromium proxy is not support");
    }

    @Override
    public void resetInputState() {
        throw new UnsupportedOperationException("chromium proxy is not support");
    }

    @Override
    public VirtualAuthenticator addVirtualAuthenticator(VirtualAuthenticatorOptions options) {
        throw new UnsupportedOperationException("chromium proxy is not support");
    }

    @Override
    public void removeVirtualAuthenticator(VirtualAuthenticator authenticator) {
        throw new UnsupportedOperationException("chromium proxy is not support");
    }

    @Override
    public List<String> getDownloadableFiles() {
        throw new UnsupportedOperationException("chromium proxy is not support");
    }

    @Override
    public void downloadFile(String fileName, Path targetLocation) throws IOException {
        throw new UnsupportedOperationException("chromium proxy is not support");
    }

    @Override
    public void deleteDownloadableFiles() {
        throw new UnsupportedOperationException("chromium proxy is not support");
    }

    @Override
    public void setDelayEnabled(boolean enabled) {
        throw new UnsupportedOperationException("chromium proxy is not support");
    }

    @Override
    public void resetCooldown() {
        throw new UnsupportedOperationException("chromium proxy is not support");
    }

    @Override
    public FederatedCredentialManagementDialog getFederatedCredentialManagementDialog() {
        throw new UnsupportedOperationException("chromium proxy is not support");
    }

    @Override
    protected void log(SessionId sessionId, String commandName, Object toLog, When when) {
        throw new UnsupportedOperationException("chromium proxy is not support");
    }

    @Override
    public FileDetector getFileDetector() {
        throw new UnsupportedOperationException("chromium proxy is not support");
    }

    @Override
    public String toString() {
        return "";
    }

    @Override
    public void requireDownloadsEnabled(Capabilities capabilities) {
        throw new UnsupportedOperationException("chromium proxy is not support");
    }
}