"""
TCP 媒体流测试
"""

import pytest
import time
import yaml
import os
from src.client.tcp_client import TCPClient
from src.mock.video_generator import VideoGenerator
from src.protocol.message import FrameType


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


@pytest.fixture
def video_generator(config):
    """创建视频生成器"""
    video_config = config["test"]["video"]
    generator = VideoGenerator(
        width=video_config["width"],
        height=video_config["height"],
        fps=video_config["fps"],
        codec=video_config["codec"],
        gop_size=video_config["gop_size"]
    )
    yield generator
    generator.stop_streaming()


def test_tcp_media_connect(config, tcp_client):
    """测试 TCP 媒体流连接"""
    host = config["server"]["address"]
    port = config["server"]["tcp"]["media_port"]
    
    success, error_msg = tcp_client.connect(host, port, use_tls=False)
    assert success == True, f"连接失败: {error_msg}"
    assert tcp_client.connected == True


def test_tcp_media_login(config, tcp_client):
    """测试媒体流登录"""
    host = config["server"]["address"]
    port = config["server"]["tcp"]["media_port"]
    
    success, error_msg = tcp_client.connect(host, port, use_tls=False)
    assert success == True, f"连接失败: {error_msg}"
    time.sleep(0.5)
    
    imei = config["test"]["imei"]
    imsi = config["test"]["imsi"]
    tcp_client.login(imei, imsi)
    
    time.sleep(0.5)
    assert tcp_client.connected == True


def test_tcp_media_video_frame(config, tcp_client, video_generator):
    """测试视频帧传输"""
    host = config["server"]["address"]
    port = config["server"]["tcp"]["media_port"]
    
    success, error_msg = tcp_client.connect(host, port, use_tls=False)
    assert success == True, f"连接失败: {error_msg}"
    time.sleep(0.5)
    
    imei = config["test"]["imei"]
    imsi = config["test"]["imsi"]
    tcp_client.login(imei, imsi)
    time.sleep(0.5)
    
    # 生成并发送 I 帧
    frame_data, frame_type = video_generator.get_frame(0)
    assert frame_data is not None
    assert frame_type == FrameType.I_FRAME
    
    tcp_client.send_video_frame(frame_data, frame_type)
    time.sleep(0.2)
    
    # 生成并发送 P 帧
    frame_data, frame_type = video_generator.get_frame(1)
    assert frame_data is not None
    assert frame_type == FrameType.P_FRAME
    
    tcp_client.send_video_frame(frame_data, frame_type)
    time.sleep(0.2)
    
    assert tcp_client.connected == True


def test_tcp_media_audio_frame(config, tcp_client):
    """测试音频帧传输"""
    host = config["server"]["address"]
    port = config["server"]["tcp"]["media_port"]
    
    success, error_msg = tcp_client.connect(host, port, use_tls=False)
    assert success == True, f"连接失败: {error_msg}"
    time.sleep(0.5)
    
    imei = config["test"]["imei"]
    imsi = config["test"]["imsi"]
    tcp_client.login(imei, imsi)
    time.sleep(0.5)
    
    # 发送模拟音频帧（实际应该是 PCM 或其他格式）
    audio_data = b'\x00' * 1024  # 模拟音频数据
    tcp_client.send_audio_frame(audio_data)
    time.sleep(0.2)
    
    assert tcp_client.connected == True
