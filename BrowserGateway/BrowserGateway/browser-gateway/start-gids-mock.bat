@echo off
setlocal

set VENV=D:\Code\SBG-Github\SBG\Test\mock-servers\venv\Scripts\python.exe
set MOCK=D:\Code\SBG-Github\SBG\Test\mock-servers\mock\gids_mock_server.py

echo Starting GIDS Mock Server on port 9090...
"%VENV%" "%MOCK%"

endlocal
