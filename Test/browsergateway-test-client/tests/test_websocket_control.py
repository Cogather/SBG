"""
WebSocket 控制流测试
"""

import pytest
import pytest_asyncio
import asyncio
import yaml
import os
from src.client.websocket_client import WebSocketClient


@pytest.fixture
def config():
    """加载配置"""
    config_path = os.path.join(os.path.dirname(__file__), "../config/config.yaml")
    with open(config_path, 'r', encoding='utf-8') as f:
        return yaml.safe_load(f)


@pytest_asyncio.fixture
async def ws_client():
    """创建 WebSocket 客户端"""
    client = WebSocketClient()
    yield client
    # 在异步 fixture 中直接 await，避免事件循环问题
    try:
        await client.disconnect_all()
    except Exception as e:
        # 忽略清理时的错误，避免影响测试结果
        print(f"清理 WebSocket 客户端时出错: {e}")


@pytest.mark.asyncio
async def test_websocket_control_connect(config, ws_client):
    """测试 WebSocket 控制流连接"""
    host = config["server"]["address"]
    port = config["server"]["websocket"]["control_port"]
    imei = config["test"]["imei"]
    imsi = config["test"]["imsi"]
    imei_and_imsi = f"{imei}:{imsi}"
    
    success, error_msg = await ws_client.connect_control(host, port, imei_and_imsi)
    assert success == True, f"连接失败: {error_msg}"
    assert ws_client.control_connected == True


@pytest.mark.asyncio
async def test_websocket_control_message(config, ws_client):
    """测试发送控制消息"""
    host = config["server"]["address"]
    port = config["server"]["websocket"]["control_port"]
    imei = config["test"]["imei"]
    imsi = config["test"]["imsi"]
    imei_and_imsi = f"{imei}:{imsi}"
    
    success, error_msg = await ws_client.connect_control(host, port, imei_and_imsi)
    assert success == True, f"连接失败: {error_msg}"
    await asyncio.sleep(0.5)
    
    # 发送控制消息
    await ws_client.send_control_message("test", {"message": "hello"})
    await asyncio.sleep(0.2)
    
    assert ws_client.control_connected == True


@pytest.mark.asyncio
async def test_websocket_control_screen_click(config, ws_client):
    """测试屏幕点击事件"""
    host = config["server"]["address"]
    port = config["server"]["websocket"]["control_port"]
    imei = config["test"]["imei"]
    imsi = config["test"]["imsi"]
    imei_and_imsi = f"{imei}:{imsi}"
    
    success, error_msg = await ws_client.connect_control(host, port, imei_and_imsi)
    assert success == True, f"连接失败: {error_msg}"
    await asyncio.sleep(0.5)
    
    # 发送屏幕点击
    await ws_client.send_screen_click(500, 500)
    await asyncio.sleep(0.2)
    
    assert ws_client.control_connected == True


@pytest.mark.asyncio
async def test_websocket_control_keyboard_input(config, ws_client):
    """测试键盘输入事件"""
    host = config["server"]["address"]
    port = config["server"]["websocket"]["control_port"]
    imei = config["test"]["imei"]
    imsi = config["test"]["imsi"]
    imei_and_imsi = f"{imei}:{imsi}"
    
    success, error_msg = await ws_client.connect_control(host, port, imei_and_imsi)
    assert success == True, f"连接失败: {error_msg}"
    await asyncio.sleep(0.5)
    
    # 发送键盘输入
    await ws_client.send_keyboard_input("Hello World")
    await asyncio.sleep(0.2)
    
    assert ws_client.control_connected == True
