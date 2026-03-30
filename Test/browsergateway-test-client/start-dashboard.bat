@echo off
setlocal
REM ============================================================
REM  评测看板 + 端到端自动化测试（需先启动依赖，如仓库根目录 start-dev.bat）
REM
REM  行为:
REM    1. 新窗口: serve_dashboard.py（固定以本目录为网站根，避免 404）
REM    2. serve_dashboard.py --open-browser（端口被占用时会自动换端口并打开正确 URL）
REM    3. 当前窗口: python run_tests.py（端口检查 + pytest，产物写入 reports/）
REM
REM  额外 pytest 参数可跟在脚本后，例如:
REM    start-dashboard.bat -k login
REM ============================================================

set ROOT=%~dp0
cd /d "%ROOT%"

if not exist ".venv\Scripts\activate.bat" (
  echo ERROR: 未找到 .venv，请先在本目录执行: python -m venv .venv ^&^& pip install -r requirements.txt
  exit /b 1
)

echo.
echo [1/2] 启动看板 HTTP 服务 ^(新窗口；首选 8765，被系统保留时会自动换端口^)...
start "e2e-dashboard-http" cmd /k echo 关闭本窗口即停止 HTTP。 ^& echo. ^& "%ROOT%.venv\Scripts\python.exe" "%ROOT%serve_dashboard.py" --open-browser

timeout /t 2 /nobreak >nul

echo [2/2] 运行端到端测试 ^(run_tests.py^)...
call .venv\Scripts\activate.bat
python run_tests.py %*
set RC=%ERRORLEVEL%

echo.
echo 更新探针数据 ^(reports\probe-result.json^)...
python probe_services.py
echo.
if %RC% equ 0 (
  echo 测试完成。若看板在测试前就打开了，请 F5 刷新以加载最新 reports。
) else (
  echo 测试结束，退出码 %RC%。仍可刷新看板查看已生成的 junit / 报告。
)
echo.
pause
exit /b %RC%
