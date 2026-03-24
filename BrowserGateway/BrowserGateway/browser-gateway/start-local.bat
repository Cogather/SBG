@echo off
setlocal

set JAVA_HOME=D:\Program Files\Java\jdk-21.0.10
set PATH=%JAVA_HOME%\bin;%PATH%

set BGW_DIR=%~dp0
set JAR=%BGW_DIR%target\browser-gateway-1.0-SNAPSHOT.jar
set LIB=%BGW_DIR%target\lib
set RESOURCES=%BGW_DIR%src\main\resources

echo Starting BrowserGateway with local profile...
echo JAVA: %JAVA_HOME%\bin\java
echo JAR:  %JAR%

"%JAVA_HOME%\bin\java" ^
  -Dspring.profiles.active=local ^
  -Dfile.encoding=UTF-8 ^
  -Dlogging.file.name=D:/workspace/logs/browsergw.log ^
  -cp "%JAR%;%LIB%\*" ^
  com.huawei.browsergateway.BrowserGatewayApplication

endlocal
