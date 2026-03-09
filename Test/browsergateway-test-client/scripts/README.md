# 测试脚本说明

本目录包含用于一键启动和测试 BrowserGateway 的脚本。

## 脚本文件

### 1. `run_all_tests.sh` (Linux/Mac/Git Bash)

适用于 Linux、Mac 或 Windows 上的 Git Bash 环境。

**功能：**
- 自动检查环境（Python、依赖等）
- 启动 BrowserGateway 服务（如果未运行）
- 运行自动化测试用例
- 可选启动 Web 测试界面
- 自动清理资源

**使用方法：**

```bash
# 运行所有测试
./scripts/run_all_tests.sh

# 启动 Web 测试界面（不运行自动化测试）
./scripts/run_all_tests.sh --ui

# 跳过启动 BrowserGateway 服务（假设服务已运行）
./scripts/run_all_tests.sh --skip-bg

# 显示帮助
./scripts/run_all_tests.sh --help
```

**环境变量：**

```bash
export BG_SERVER_HOST=192.168.1.100
export BG_SERVER_HTTP_PORT=8090
./scripts/run_all_tests.sh
```

### 2. `run_all_tests.bat` (Windows)

适用于 Windows 命令行（CMD）环境。

**功能：**
- 与 `.sh` 脚本功能相同，但适用于 Windows

**使用方法：**

```cmd
REM 运行所有测试
scripts\run_all_tests.bat

REM 启动 Web 测试界面
scripts\run_all_tests.bat --ui

REM 跳过启动 BrowserGateway 服务
scripts\run_all_tests.bat --skip-bg

REM 显示帮助
scripts\run_all_tests.bat --help
```

### 3. `run_test.py`

启动 Web 测试界面的 Python 脚本。

**使用方法：**

```bash
python scripts/run_test.py
```

## 使用场景

### 场景 1：完整自动化测试

```bash
# Linux/Mac/Git Bash
./scripts/run_all_tests.sh

# Windows
scripts\run_all_tests.bat
```

脚本会：
1. 检查环境
2. 启动 BrowserGateway 服务（如果未运行）
3. 等待服务启动
4. 运行所有测试用例
5. 显示测试结果
6. 清理资源

### 场景 2：使用已运行的服务进行测试

如果 BrowserGateway 服务已经在运行：

```bash
# Linux/Mac/Git Bash
./scripts/run_all_tests.sh --skip-bg

# Windows
scripts\run_all_tests.bat --skip-bg
```

### 场景 3：启动 Web 测试界面

启动 Web 界面进行可视化测试：

```bash
# Linux/Mac/Git Bash
./scripts/run_all_tests.sh --ui

# Windows
scripts\run_all_tests.bat --ui
```

然后访问 `http://localhost:8000` 进行测试。

### 场景 4：仅运行 Web 界面（不启动服务）

```bash
# 直接运行 Python 脚本
python scripts/run_test.py
```

## 注意事项

1. **BrowserGateway 服务启动**
   - 脚本会尝试自动启动 BrowserGateway 服务
   - 需要 Maven 或已编译的 JAR 文件
   - 如果自动启动失败，请手动启动服务

2. **端口占用**
   - 脚本会检查端口是否被占用
   - 如果端口已被占用，会询问是否继续使用现有服务

3. **日志文件**
   - BrowserGateway 服务日志：`browsergateway.log`
   - 测试服务器日志：`test_server.log`

4. **资源清理**
   - 脚本退出时会自动清理资源
   - 按 Ctrl+C 可以安全退出

5. **Windows 用户**
   - 如果使用 Git Bash，可以使用 `.sh` 脚本
   - 如果使用 CMD，请使用 `.bat` 脚本
   - PowerShell 用户可以使用 Git Bash 或 CMD

## 故障排除

### 问题 1：服务启动失败

**解决方案：**
- 检查 BrowserGateway 目录路径是否正确
- 确认 Maven 已安装或 JAR 文件存在
- 查看 `browsergateway.log` 日志文件

### 问题 2：测试失败

**解决方案：**
- 确认 BrowserGateway 服务已启动
- 检查配置文件中的服务器地址和端口
- 查看测试输出中的详细错误信息

### 问题 3：端口被占用

**解决方案：**
- 使用 `--skip-bg` 参数跳过启动服务
- 或手动停止占用端口的进程

### 问题 4：Python 依赖问题

**解决方案：**
- 脚本会自动安装依赖
- 如果失败，手动运行：`pip install -r requirements.txt`

## 示例输出

```
==========================================
  BrowserGateway 一键测试脚本
==========================================

[INFO] 检查 Python 环境...
[SUCCESS] Python 环境检查完成

[INFO] 检查 BrowserGateway 服务...
[INFO] 启动 BrowserGateway 服务...
[INFO] BrowserGateway 服务启动中 (PID: 12345)
[SUCCESS] BrowserGateway 服务启动成功

[INFO] 开始运行测试...
[INFO] 执行 pytest 测试...
======================== test session starts =========================
tests/test_tcp_control.py::test_tcp_connect PASSED
tests/test_tcp_control.py::test_tcp_login PASSED
...
======================== 10 passed in 5.23s =========================

[SUCCESS] 测试完成！所有测试通过
```
