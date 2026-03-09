@echo off
REM BrowserGateway 一键启动和测试脚本 (Windows 版本)
REM 功能：启动 BrowserGateway 服务并执行测试

setlocal enabledelayedexpansion

REM 脚本目录
set "SCRIPT_DIR=%~dp0"
set "PROJECT_ROOT=%SCRIPT_DIR%.."
set "BROWSERGATEWAY_DIR=%PROJECT_ROOT%\..\..\BrowserGateway\BrowserGateway\browser-gateway"

REM 配置
set "BG_SERVER_HOST=127.0.0.1"
set "BG_SERVER_HTTP_PORT=8090"
set "BG_STARTUP_TIMEOUT=60"
set "TEST_TIMEOUT=300"

REM 进程 ID 存储
set "BG_PID="
set "TEST_SERVER_PID="

echo ==========================================
echo   BrowserGateway 一键测试脚本 (Windows)
echo ==========================================
echo.

REM 检查参数
set "START_UI=false"
set "SKIP_BG=false"

:parse_args
if "%~1"=="" goto end_parse
if /i "%~1"=="--ui" set "START_UI=true"
if /i "%~1"=="--web-ui" set "START_UI=true"
if /i "%~1"=="--skip-bg" set "SKIP_BG=true"
if /i "%~1"=="--skip-browsergateway" set "SKIP_BG=true"
if /i "%~1"=="--help" goto show_help
if /i "%~1"=="-h" goto show_help
shift
goto parse_args

:end_parse

REM 检查 Python
where python >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Python 未安装，请先安装 Python
    exit /b 1
)

REM 检查虚拟环境
if not exist "%PROJECT_ROOT%\venv" (
    echo [INFO] 创建虚拟环境...
    python -m venv "%PROJECT_ROOT%\venv"
)

REM 激活虚拟环境
call "%PROJECT_ROOT%\venv\Scripts\activate.bat"

REM 检查依赖
echo [INFO] 检查 Python 依赖...
pip show fastapi >nul 2>&1
if errorlevel 1 (
    echo [INFO] 安装 Python 依赖...
    pip install -r "%PROJECT_ROOT%\requirements.txt" -q
)

REM 启动 BrowserGateway 服务
if "%SKIP_BG%"=="false" (
    echo [INFO] 检查 BrowserGateway 服务...
    
    REM 检查端口是否被占用
    netstat -an | findstr ":%BG_SERVER_HTTP_PORT%" | findstr "LISTENING" >nul
    if not errorlevel 1 (
        echo [WARNING] BrowserGateway 服务似乎已经在运行 (端口 %BG_SERVER_HTTP_PORT%^)
        set /p CONTINUE="是否继续使用现有服务? (y/n) "
        if /i not "!CONTINUE!"=="y" (
            echo [INFO] 跳过启动 BrowserGateway 服务
            goto skip_bg_start
        )
        goto skip_bg_start
    )
    
    REM 检查 BrowserGateway 目录
    if not exist "%BROWSERGATEWAY_DIR%" (
        echo [WARNING] BrowserGateway 目录不存在: %BROWSERGATEWAY_DIR%
        echo [WARNING] 将跳过启动 BrowserGateway 服务，请手动启动服务
        goto skip_bg_start
    )
    
    echo [INFO] 启动 BrowserGateway 服务...
    cd /d "%BROWSERGATEWAY_DIR%"
    
    REM 检查 Maven
    where mvn >nul 2>&1
    if not errorlevel 1 (
        echo [INFO] 使用 Maven 启动服务...
        start /B mvn spring-boot:run > "%PROJECT_ROOT%\browsergateway.log" 2>&1
    ) else (
        REM 检查 JAR 文件
        if exist "target\browser-gateway-1.0-SNAPSHOT.jar" (
            echo [INFO] 使用 JAR 文件启动服务...
            start /B java -jar target\browser-gateway-1.0-SNAPSHOT.jar > "%PROJECT_ROOT%\browsergateway.log" 2>&1
        ) else (
            echo [WARNING] 未找到 Maven 或 JAR 文件，请手动启动 BrowserGateway 服务
            echo [INFO] 启动命令示例:
            echo   cd %BROWSERGATEWAY_DIR%
            echo   mvn spring-boot:run
            goto skip_bg_start
        )
    )
    
    echo [INFO] BrowserGateway 服务启动中...
    echo [INFO] 日志文件: %PROJECT_ROOT%\browsergateway.log
    
    REM 等待服务启动
    echo [INFO] 等待服务启动 (%BG_SERVER_HOST%:%BG_SERVER_HTTP_PORT%)...
    set /a ELAPSED=0
    :wait_loop
    timeout /t 2 /nobreak >nul
    netstat -an | findstr ":%BG_SERVER_HTTP_PORT%" | findstr "LISTENING" >nul
    if not errorlevel 1 (
        echo [SUCCESS] 服务已启动 (%BG_SERVER_HOST%:%BG_SERVER_HTTP_PORT%^)
        goto skip_bg_start
    )
    set /a ELAPSED+=2
    if !ELAPSED! geq %BG_STARTUP_TIMEOUT% (
        echo [ERROR] 服务启动超时 (%BG_SERVER_HOST%:%BG_SERVER_HTTP_PORT%^)
        echo [ERROR] 请查看日志: %PROJECT_ROOT%\browsergateway.log
        exit /b 1
    )
    echo|set /p="."
    goto wait_loop
)

:skip_bg_start

REM 启动 Web 界面或运行测试
if "%START_UI%"=="true" (
    echo.
    echo [INFO] 启动 Web 测试界面...
    cd /d "%PROJECT_ROOT%"
    call "%PROJECT_ROOT%\venv\Scripts\activate.bat"
    
    start /B python scripts\run_test.py > "%PROJECT_ROOT%\test_server.log" 2>&1
    
    echo [INFO] Web 测试界面启动中...
    timeout /t 3 /nobreak >nul
    
    netstat -an | findstr ":8000" | findstr "LISTENING" >nul
    if not errorlevel 1 (
        echo [SUCCESS] Web 测试界面已启动
        echo [INFO] 访问地址: http://localhost:8000
        echo [INFO] 日志文件: %PROJECT_ROOT%\test_server.log
        echo.
        echo [INFO] 按 Ctrl+C 停止所有服务
        pause
    ) else (
        echo [WARNING] Web 测试界面启动可能失败，请查看日志
    )
) else (
    echo.
    echo [INFO] 开始运行测试...
    cd /d "%PROJECT_ROOT%"
    call "%PROJECT_ROOT%\venv\Scripts\activate.bat"
    
    echo [INFO] 执行 pytest 测试...
    pytest tests\ -v --tb=short --timeout=%TEST_TIMEOUT%
    
    if errorlevel 1 (
        echo.
        echo [ERROR] 测试完成！部分测试失败
        exit /b 1
    ) else (
        echo.
        echo [SUCCESS] 测试完成！所有测试通过
        exit /b 0
    )
)

goto end

:show_help
echo 用法: %~nx0 [选项]
echo.
echo 选项:
echo   --ui, --web-ui              启动 Web 测试界面
echo   --skip-bg, --skip-browsergateway  跳过启动 BrowserGateway 服务
echo   --help, -h                  显示帮助信息
echo.
echo 环境变量:
echo   BG_SERVER_HOST              BrowserGateway 服务器地址 (默认: 127.0.0.1)
echo   BG_SERVER_HTTP_PORT         BrowserGateway HTTP 端口 (默认: 8090)
exit /b 0

:end
endlocal
