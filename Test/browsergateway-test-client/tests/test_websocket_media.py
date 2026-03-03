"""
WebSocket 媒体流测试
"""

import pytest
import pytest_asyncio
import asyncio
import yaml
import os
from src.client.websocket_client import WebSocketClient
from src.mock.video_generator import VideoGenerator


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


@pytest.mark.asyncio
async def test_websocket_media_connect(config, ws_client):
    """测试 WebSocket 媒体流连接"""
    host = config["server"]["address"]
    port = config["server"]["websocket"]["media_port"]
    imei = config["test"]["imei"]
    imsi = config["test"]["imsi"]
    imei_and_imsi = f"{imei}:{imsi}"
    
    init_params = {
        "width": 1920,
        "height": 1080,
        "fps": 30
    }
    
    success, error_msg = await ws_client.connect_media(host, port, imei_and_imsi, init_params)
    assert success == True, f"连接失败: {error_msg}"
    assert ws_client.media_connected == True


@pytest.mark.asyncio
async def test_websocket_media_video_frame(config, ws_client, video_generator):
    """测试视频帧传输"""
    host = config["server"]["address"]
    port = config["server"]["websocket"]["media_port"]
    imei = config["test"]["imei"]
    imsi = config["test"]["imsi"]
    imei_and_imsi = f"{imei}:{imsi}"
    
    init_params = {
        "width": 1920,
        "height": 1080,
        "fps": 30
    }
    
    success, error_msg = await ws_client.connect_media(host, port, imei_and_imsi, init_params)
    assert success == True, f"连接失败: {error_msg}"
    await asyncio.sleep(0.5)
    
    # 生成并发送 I 帧
    frame_data, frame_type = video_generator.get_frame(0)
    assert frame_data is not None
    
    await ws_client.send_media_frame(frame_data, frame_type)
    await asyncio.sleep(0.2)
    
    # 生成并发送 P 帧
    frame_data, frame_type = video_generator.get_frame(1)
    await ws_client.send_media_frame(frame_data, frame_type)
    await asyncio.sleep(0.2)
    
    assert ws_client.media_connected == True
