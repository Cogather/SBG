@echo off
setlocal

set VENV=D:\Code\SBG-Github\SBG\Test\browsergateway-test-client\venv\Scripts\python.exe
set MOCK=D:\Code\SBG-Github\SBG\Test\browsergateway-test-client\src\mock\gids_mock_server.py

echo Starting GIDS Mock Server on port 9090...
"%VENV%" "%MOCK%"

endlocal
