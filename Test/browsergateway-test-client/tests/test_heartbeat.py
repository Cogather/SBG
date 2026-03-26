"""
心跳保活测试

mobile WebSocket 协议中没有独立的心跳消息类型。
心跳保活验证：登录后等待一段时间，连接仍正常（不自动断开），
之后仍可发送控制消息且连接未关闭。
"""

import asyncio
import pytest


async def test_connection_stays_alive(mobile_client):
    """
    session 级 client 登录后等待 15s，验证连接仍保持（WebSocket 未关闭）。
    """
    await asyncio.sleep(15)

    try:
        await mobile_client.send_direction(ct=0, cv=12)  # UP
    except Exception as e:
        pytest.fail(f"Connection closed after 15s idle: {e}")
