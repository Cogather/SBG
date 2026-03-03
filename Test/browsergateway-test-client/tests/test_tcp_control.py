"""
TCP 控制流测试
"""

import pytest
import time
import yaml
import os
from src.client.tcp_client import TCPClient
from src.protocol.message import MessageType, KeyCode, TouchAction


@pytest.fixture
def config():
    """加载配置"""
    config_path = os.path.join(os.path.dirname(__file__), "../config/config.yaml")
    with open(config_path, 'r', encoding='utf-8') as f:
        return yaml.safe_load(f)


@pytest.fixture
def tcp_client(config):
    """创建 TCP 客户端"""
    client = TCPClient()
    yield client
    client.disconnect()


def test_tcp_connect(config, tcp_client):
    """测试 TCP 连接"""
    host = config["server"]["address"]
    port = config["server"]["tcp"]["control_port"]
    
    success, error_msg = tcp_client.connect(host, port, use_tls=False)
    assert success == True, f"连接失败: {error_msg}"
    assert tcp_client.connected == True


def test_tcp_login(config, tcp_client):
    """测试登录"""
    host = config["server"]["address"]
    port = config["server"]["tcp"]["control_port"]
    
    success, error_msg = tcp_client.connect(host, port, use_tls=False)
    assert success == True, f"连接失败: {error_msg}"
    time.sleep(0.5)  # 等待连接建立
    
    imei = config["test"]["imei"]
    imsi = config["test"]["imsi"]
    tcp_client.login(imei, imsi)
    
    time.sleep(0.5)  # 等待响应
    assert tcp_client.connected == True


def test_tcp_heartbeat(config, tcp_client):
    """测试心跳"""
    host = config["server"]["address"]
    port = config["server"]["tcp"]["control_port"]
    
    success, error_msg = tcp_client.connect(host, port, use_tls=False)
    assert success == True, f"连接失败: {error_msg}"
    time.sleep(0.5)
    
    imei = config["test"]["imei"]
    imsi = config["test"]["imsi"]
    tcp_client.login(imei, imsi)
    time.sleep(0.5)
    
    # 发送心跳
    tcp_client.send_heartbeat()
    time.sleep(0.5)
    
    assert tcp_client.connected == True


def test_tcp_key_event(config, tcp_client):
    """测试按键事件"""
    host = config["server"]["address"]
    port = config["server"]["tcp"]["control_port"]
    
    success, error_msg = tcp_client.connect(host, port, use_tls=False)
    assert success == True, f"连接失败: {error_msg}"
    time.sleep(0.5)
    
    imei = config["test"]["imei"]
    imsi = config["test"]["imsi"]
    tcp_client.login(imei, imsi)
    time.sleep(0.5)
    
    # 发送按键事件（按下）
    tcp_client.send_key_event(KeyCode.KEY_ENTER, 0)
    time.sleep(0.2)
    
    # 发送按键事件（释放）
    tcp_client.send_key_event(KeyCode.KEY_ENTER, 1)
    time.sleep(0.2)
    
    assert tcp_client.connected == True


def test_tcp_touch_event(config, tcp_client):
    """测试触摸事件"""
    host = config["server"]["address"]
    port = config["server"]["tcp"]["control_port"]
    
    success, error_msg = tcp_client.connect(host, port, use_tls=False)
    assert success == True, f"连接失败: {error_msg}"
    time.sleep(0.5)
    
    imei = config["test"]["imei"]
    imsi = config["test"]["imsi"]
    tcp_client.login(imei, imsi)
    time.sleep(0.5)
    
    # 发送触摸事件
    tcp_client.send_touch_event(500, 500, TouchAction.ACTION_DOWN)
    time.sleep(0.1)
    tcp_client.send_touch_event(500, 500, TouchAction.ACTION_UP)
    time.sleep(0.2)
    
    assert tcp_client.connected == True


def test_tcp_logout(config, tcp_client):
    """测试登出"""
    host = config["server"]["address"]
    port = config["server"]["tcp"]["control_port"]
    
    success, error_msg = tcp_client.connect(host, port, use_tls=False)
    assert success == True, f"连接失败: {error_msg}"
    time.sleep(0.5)
    
    imei = config["test"]["imei"]
    imsi = config["test"]["imsi"]
    tcp_client.login(imei, imsi)
    time.sleep(0.5)
    
    # 登出
    tcp_client.logout()
    time.sleep(0.5)
    
    assert tcp_client.connected == False
