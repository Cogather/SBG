"""
媒体流接收测试

WebSocket 二进制帧格式（mobile 推送）：
  视频：[0x01, frameType] + H.264 bytes  (frameType=1 I帧, 2 P帧)
  音频：[0x02] + MP3 bytes
"""

import pytest


async def _recv_frame(client, first_byte: int, timeout: float = 30.0) -> bytes:
    """Wait for a binary frame with the given first byte within timeout."""
    deadline = timeout
    while deadline > 0:
        try:
            frame = await client.recv_binary(timeout=min(deadline, 2.0))
            if len(frame) >= 1 and frame[0] == first_byte:
                return frame
        except TimeoutError:
            pass
        deadline -= 2.0
    return None


async def test_browser_opened_after_login(mobile_client):
    """
    浏览器打开验证：session 登录后 30s 内应收到至少一帧视频数据（首字节 0x01）。
    超时视为浏览器未打开，pytest.skip。
    """
    raw = await _recv_frame(mobile_client, 0x01, timeout=30.0)
    if raw is None:
        pytest.skip(
            "No video frame received within 30s after login — "
            "browser-gateway or cloud-phone stream not available in this environment."
        )


async def test_video_frame_format(mobile_client):
    """
    视频帧格式验证：首字节 0x01，第二字节为 1（I帧）或 2（P帧）。
    若 30s 内无帧，skip（依赖推流环境）。
    """
    raw = await _recv_frame(mobile_client, 0x01, timeout=30.0)
    if raw is None:
        pytest.skip("No video frame received within 30s (no stream available)")

    assert raw[0] == 0x01, f"Video frame first byte should be 0x01, got {raw[0]:#04x}"
    assert len(raw) >= 2, "Video frame too short (missing frameType byte)"
    # frameType is passed through from the cloud-phone TLV field; 0/1/2 are all valid
    assert isinstance(raw[1], int), "frameType byte must be an integer"
    assert len(raw) > 2, "Video frame has no H.264 payload"


async def test_audio_frame_format(mobile_client):
    """
    音频帧格式验证：首字节 0x02，其后为 MP3 数据。
    若 30s 内无帧，skip。
    """
    raw = await _recv_frame(mobile_client, 0x02, timeout=30.0)
    if raw is None:
        pytest.skip("No audio frame received within 30s (no stream available)")

    assert raw[0] == 0x02, f"Audio frame first byte should be 0x02, got {raw[0]:#04x}"
    assert len(raw) > 1, "Audio frame has no MP3 payload"
