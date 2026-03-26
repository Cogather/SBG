@echo off
setlocal

REM ============================================================
REM  SBG 开发环境一键启动脚本
REM
REM  前置条件:
REM    1. Java 和 Maven 已安装并在系统 PATH 中
REM    2. Python 虚拟环境已创建:
REM       - Test\mock-servers\venv\
REM         (创建: cd Test\mock-servers && python -m venv venv)
REM       - BrowserGateway\BrowserGateway\browser-proxy\.venv\
REM         (创建: cd BrowserGateway\BrowserGateway\browser-proxy && python -m venv .venv)
REM    3. browser-gateway local 配置文件已存在:
REM       BrowserGateway\BrowserGateway\browser-gateway\src\main\resources\application-local.yaml
REM
REM  启动顺序: gids-mock-server -> browser-proxy -> mobile -> browser-gateway
REM  各服务端口:
REM    gids-mock-server : 9090
REM    browser-proxy    : 8000
REM    mobile           : 8088 (HTTP), 40002 (WebSocket)
REM    browser-gateway  : 见 application-local.yaml
REM ============================================================

set ROOT=%~dp0

echo.
echo =============================================
echo  SBG Dev Environment Startup
echo =============================================
echo.

REM 1. gids_mock_server (port 9090)
echo [1/4] Starting gids-mock-server on port 9090...
start "gids-mock-server" cmd /k "cd /d %ROOT%Test\browsergateway-test-client & call .venv\Scripts\activate.bat & python src\mock\gids_mock_server.py"

REM 2. browser-proxy (port 8000)
echo [2/4] Starting browser-proxy on port 8000...
start "browser-proxy" cmd /k "cd /d %ROOT%BrowserGateway\BrowserGateway\browser-proxy & call .venv\Scripts\activate.bat & python -m browser_proxy.main --port 8000"

REM 3. mobile (port 8088 / WS 40002)
echo [3/4] Starting mobile service (port 8088, WS 40002)...
start "mobile" cmd /k "cd /d %ROOT%mobile & mvn spring-boot:run"

REM 4. browser-gateway (local profile)
echo [4/4] Building and starting browser-gateway with local profile...
start "browser-gateway" cmd /k "cd /d %ROOT%BrowserGateway\BrowserGateway\browser-gateway & mvn package -DskipTests -q & call start-local.bat"

echo.
echo All 4 services launched in separate windows.
echo Close each window to stop the corresponding service.
echo.
echo Waiting 60s for services to start, then verifying ports...
timeout /t 60 /nobreak >nul

echo.
echo =============================================
echo  Port Verification
echo =============================================
python -c "import socket; ports=[('gids-mock',9090),('browser-proxy',8000),('mobile-http',8088),('mobile-ws',40002),('bgw-tcp-ctrl',30001),('bgw-tcp-media',30002),('bgw-http',8090)]; [print(('OK' if socket.socket().connect_ex(('127.0.0.1',p))==0 else 'MISSING')+' '+str(p)+' '+n) for n,p in ports]"
echo.

endlocal
