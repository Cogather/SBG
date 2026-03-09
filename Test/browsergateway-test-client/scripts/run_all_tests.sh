#!/bin/bash

# BrowserGateway 一键启动和测试脚本
# 功能：启动 BrowserGateway 服务并执行测试

set -e  # 遇到错误立即退出

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 脚本目录
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
BROWSERGATEWAY_DIR="$PROJECT_ROOT/../../BrowserGateway/BrowserGateway/browser-gateway"

# 配置
BG_SERVER_HOST="${BG_SERVER_HOST:-127.0.0.1}"
BG_SERVER_HTTP_PORT="${BG_SERVER_HTTP_PORT:-8090}"
BG_STARTUP_TIMEOUT=60  # 服务启动超时时间（秒）
TEST_TIMEOUT=300       # 测试超时时间（秒）

# 进程 ID 存储
BG_PID=""
TEST_SERVER_PID=""

# 清理函数
cleanup() {
    echo -e "\n${YELLOW}正在清理资源...${NC}"
    
    # 停止测试服务器
    if [ ! -z "$TEST_SERVER_PID" ]; then
        echo "停止测试服务器 (PID: $TEST_SERVER_PID)"
        kill $TEST_SERVER_PID 2>/dev/null || true
        wait $TEST_SERVER_PID 2>/dev/null || true
    fi
    
    # 停止 BrowserGateway 服务
    if [ ! -z "$BG_PID" ]; then
        echo "停止 BrowserGateway 服务 (PID: $BG_PID)"
        kill $BG_PID 2>/dev/null || true
        wait $BG_PID 2>/dev/null || true
    fi
    
    # 清理可能残留的 Java 进程
    pkill -f "BrowserGatewayApplication" 2>/dev/null || true
    pkill -f "browser-gateway" 2>/dev/null || true
    
    echo -e "${GREEN}清理完成${NC}"
}

# 注册退出时的清理函数
trap cleanup EXIT INT TERM

# 打印带颜色的消息
print_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 检查命令是否存在
check_command() {
    if ! command -v $1 &> /dev/null; then
        print_error "$1 未安装，请先安装 $1"
        exit 1
    fi
}

# 检查端口是否被占用
check_port() {
    local port=$1
    if lsof -Pi :$port -sTCP:LISTEN -t >/dev/null 2>&1 || netstat -an | grep -q ":$port.*LISTEN" 2>/dev/null; then
        return 0  # 端口被占用
    else
        return 1  # 端口未被占用
    fi
}

# 等待服务启动
wait_for_service() {
    local host=$1
    local port=$2
    local timeout=$3
    local elapsed=0
    
    print_info "等待服务启动 ($host:$port)..."
    
    while [ $elapsed -lt $timeout ]; do
        if check_port $port; then
            # 额外检查 HTTP 连接（如果是 HTTP 端口）
            if [ $port -eq $BG_SERVER_HTTP_PORT ]; then
                if curl -s "http://$host:$port" > /dev/null 2>&1 || curl -s "http://$host:$port/health" > /dev/null 2>&1; then
                    print_success "服务已启动 ($host:$port)"
                    return 0
                fi
            else
                print_success "服务已启动 ($host:$port)"
                return 0
            fi
        fi
        sleep 2
        elapsed=$((elapsed + 2))
        echo -n "."
    done
    
    print_error "服务启动超时 ($host:$port)"
    return 1
}

# 启动 BrowserGateway 服务
start_browsergateway() {
    print_info "检查 BrowserGateway 服务..."
    
    # 检查服务是否已经运行
    if check_port $BG_SERVER_HTTP_PORT; then
        print_warning "BrowserGateway 服务似乎已经在运行 (端口 $BG_SERVER_HTTP_PORT)"
        read -p "是否继续使用现有服务? (y/n) " -n 1 -r
        echo
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            print_info "跳过启动 BrowserGateway 服务"
            return 0
        fi
        return 0
    fi
    
    # 检查 BrowserGateway 目录是否存在
    if [ ! -d "$BROWSERGATEWAY_DIR" ]; then
        print_warning "BrowserGateway 目录不存在: $BROWSERGATEWAY_DIR"
        print_warning "将跳过启动 BrowserGateway 服务，请手动启动服务"
        return 0
    fi
    
    print_info "启动 BrowserGateway 服务..."
    cd "$BROWSERGATEWAY_DIR"
    
    # 检查是否有 Maven
    if command -v mvn &> /dev/null; then
        print_info "使用 Maven 启动服务..."
        # 后台启动服务
        nohup mvn spring-boot:run > "$PROJECT_ROOT/browsergateway.log" 2>&1 &
        BG_PID=$!
    elif [ -f "target/browser-gateway-1.0-SNAPSHOT.jar" ]; then
        print_info "使用 JAR 文件启动服务..."
        nohup java -jar target/browser-gateway-1.0-SNAPSHOT.jar > "$PROJECT_ROOT/browsergateway.log" 2>&1 &
        BG_PID=$!
    else
        print_warning "未找到 Maven 或 JAR 文件，请手动启动 BrowserGateway 服务"
        print_info "启动命令示例:"
        echo "  cd $BROWSERGATEWAY_DIR"
        echo "  mvn spring-boot:run"
        return 0
    fi
    
    print_info "BrowserGateway 服务启动中 (PID: $BG_PID)"
    print_info "日志文件: $PROJECT_ROOT/browsergateway.log"
    
    # 等待服务启动
    if wait_for_service $BG_SERVER_HOST $BG_SERVER_HTTP_PORT $BG_STARTUP_TIMEOUT; then
        print_success "BrowserGateway 服务启动成功"
        return 0
    else
        print_error "BrowserGateway 服务启动失败，请查看日志: $PROJECT_ROOT/browsergateway.log"
        return 1
    fi
}

# 检查 Python 环境
check_python_env() {
    print_info "检查 Python 环境..."
    
    check_command python3
    
    # 检查虚拟环境
    if [ ! -d "$PROJECT_ROOT/venv" ]; then
        print_warning "虚拟环境不存在，正在创建..."
        python3 -m venv "$PROJECT_ROOT/venv"
    fi
    
    # 激活虚拟环境
    source "$PROJECT_ROOT/venv/bin/activate"
    
    # 检查依赖
    print_info "检查 Python 依赖..."
    if ! pip show fastapi > /dev/null 2>&1; then
        print_info "安装 Python 依赖..."
        pip install -r "$PROJECT_ROOT/requirements.txt" -q
    fi
    
    print_success "Python 环境检查完成"
}

# 运行测试
run_tests() {
    print_info "开始运行测试..."
    
    cd "$PROJECT_ROOT"
    source "$PROJECT_ROOT/venv/bin/activate"
    
    # 运行测试
    print_info "执行 pytest 测试..."
    pytest tests/ -v --tb=short --timeout=$TEST_TIMEOUT
    
    local test_result=$?
    
    if [ $test_result -eq 0 ]; then
        print_success "所有测试通过！"
    else
        print_error "部分测试失败"
    fi
    
    return $test_result
}

# 启动 Web 测试界面（可选）
start_web_ui() {
    local start_ui=$1
    
    if [ "$start_ui" != "true" ]; then
        return 0
    fi
    
    print_info "启动 Web 测试界面..."
    cd "$PROJECT_ROOT"
    source "$PROJECT_ROOT/venv/bin/activate"
    
    # 后台启动 Web 服务器
    python scripts/run_test.py > "$PROJECT_ROOT/test_server.log" 2>&1 &
    TEST_SERVER_PID=$!
    
    print_info "Web 测试界面启动中 (PID: $TEST_SERVER_PID)"
    sleep 3
    
    if check_port 8000; then
        print_success "Web 测试界面已启动"
        print_info "访问地址: http://localhost:8000"
        print_info "日志文件: $PROJECT_ROOT/test_server.log"
    else
        print_warning "Web 测试界面启动可能失败，请查看日志"
    fi
}

# 主函数
main() {
    echo "=========================================="
    echo "  BrowserGateway 一键测试脚本"
    echo "=========================================="
    echo ""
    
    # 解析参数
    START_UI=false
    SKIP_BG=false
    
    while [[ $# -gt 0 ]]; do
        case $1 in
            --ui|--web-ui)
                START_UI=true
                shift
                ;;
            --skip-bg|--skip-browsergateway)
                SKIP_BG=true
                shift
                ;;
            --help|-h)
                echo "用法: $0 [选项]"
                echo ""
                echo "选项:"
                echo "  --ui, --web-ui              启动 Web 测试界面"
                echo "  --skip-bg, --skip-browsergateway  跳过启动 BrowserGateway 服务"
                echo "  --help, -h                  显示帮助信息"
                echo ""
                echo "环境变量:"
                echo "  BG_SERVER_HOST              BrowserGateway 服务器地址 (默认: 127.0.0.1)"
                echo "  BG_SERVER_HTTP_PORT         BrowserGateway HTTP 端口 (默认: 8090)"
                exit 0
                ;;
            *)
                print_error "未知参数: $1"
                echo "使用 --help 查看帮助"
                exit 1
                ;;
        esac
    done
    
    # 执行步骤
    print_info "开始执行测试流程..."
    echo ""
    
    # 1. 检查环境
    check_command curl
    check_python_env
    echo ""
    
    # 2. 启动 BrowserGateway 服务
    if [ "$SKIP_BG" != "true" ]; then
        start_browsergateway
        echo ""
    else
        print_info "跳过启动 BrowserGateway 服务"
        # 检查服务是否运行
        if ! check_port $BG_SERVER_HTTP_PORT; then
            print_warning "BrowserGateway 服务未运行，测试可能会失败"
        fi
        echo ""
    fi
    
    # 3. 启动 Web 界面（可选）
    if [ "$START_UI" = "true" ]; then
        start_web_ui true
        echo ""
        print_info "Web 界面已启动，可以在浏览器中访问 http://localhost:8000"
        print_info "按 Ctrl+C 停止所有服务"
        echo ""
        # 如果启动了 UI，保持运行
        while true; do
            sleep 10
        done
    else
        # 4. 运行测试
        run_tests
        TEST_RESULT=$?
        echo ""
        
        # 5. 显示结果
        if [ $TEST_RESULT -eq 0 ]; then
            print_success "测试完成！所有测试通过"
            exit 0
        else
            print_error "测试完成！部分测试失败"
            exit 1
        fi
    fi
}

# 运行主函数
main "$@"
