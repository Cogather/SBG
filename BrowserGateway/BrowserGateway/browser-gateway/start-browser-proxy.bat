@echo off
setlocal

set VENV=D:\Code\SBG-Github\SBG\BrowserGateway\BrowserGateway\browser-proxy\.venv\Scripts\python.exe
set PROXY_DIR=D:\Code\SBG-Github\SBG\BrowserGateway\BrowserGateway\browser-proxy

echo Starting browser-proxy on port 8000...

cd /d "%PROXY_DIR%"
"%VENV%" -m browser_proxy.main --port 8000

endlocal
