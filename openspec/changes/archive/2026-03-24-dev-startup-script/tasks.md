## 1. 调研与准备

- [x] 1.1 确认 gids_mock_server 的启动命令（入口文件及参数）
- [x] 1.2 确认 browser-proxy 的启动命令（uvicorn 入口模块及端口）
- [x] 1.3 确认 mobile 的 Maven 启动命令及工作目录
- [x] 1.4 确认 browser-gateway 使用 local profile 的 Maven 启动命令
- [x] 1.5 确认各 Python 服务 `.venv` 虚拟环境路径（`.venv/Scripts/activate.bat`）

## 2. 编写启动脚本

- [x] 2.1 在项目根目录创建 `start-dev.bat`
- [x] 2.2 添加启动 gids_mock_server 的命令（独立 cmd 窗口 + 虚拟环境激活）
- [x] 2.3 添加启动 browser-proxy 的命令（独立 cmd 窗口 + 虚拟环境激活）
- [x] 2.4 添加启动 mobile 的命令（独立 cmd 窗口 + mvn spring-boot:run）
- [x] 2.5 添加启动 browser-gateway 的命令（独立 cmd 窗口 + mvn spring-boot:run -Dspring-boot.run.profiles=local）
- [x] 2.6 在脚本顶部添加注释，说明前置条件（Java、Maven、Python 虚拟环境需已就绪，application-local.yaml 需已存在）

## 3. 验证

- [x] 3.1 运行 `start-dev.bat`，确认四个服务窗口均正常打开
- [x] 3.2 确认 gids_mock_server 在端口 9090 监听
- [x] 3.3 确认 browser-proxy 正常启动
- [x] 3.4 确认 mobile 在端口 8088 和 40002 监听
- [x] 3.5 确认 browser-gateway 以 local profile 启动，日志显示加载了 application-local.yaml
