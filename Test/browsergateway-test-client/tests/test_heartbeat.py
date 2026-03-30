"""
心跳保活测试

mobile WebSocket 协议中没有独立的心跳消息类型。
心跳保活验证：登录后等待一段时间，连接仍正常（不自动断开），
之后仍可发送控制消息且连接未关闭。

环境变量：
  E2E_IDLE_SECONDS   静默秒数，默认 15（PLAN-HB-LONG-01 可在 nightly 设为 600）
"""

import asyncio
import os

import pytest


async def test_connection_stays_alive(mobile_client):
    """
    session 级 client 登录后静默 E2E_IDLE_SECONDS（默认 15s），验证连接仍保持。
    """
    idle = int(os.environ.get("E2E_IDLE_SECONDS", "15"))
    await asyncio.sleep(idle)

    try:
        await mobile_client.send_direction(ct=0, cv=12)  # UP
    except Exception as e:
        pytest.fail(f"Connection closed after {idle}s idle: {e}")


@pytest.mark.slow
async def test_connection_stays_alive_long(mobile_client):
    """
    PLAN-HB-LONG-01：长静默保活。默认跳过，避免拖慢 PR。

    运行示例：set E2E_RUN_LONG_IDLE=1 && set E2E_IDLE_SECONDS=600 && pytest ...
    """
    if os.environ.get("E2E_RUN_LONG_IDLE", "").lower() not in ("1", "true", "yes"):
        pytest.skip("Set E2E_RUN_LONG_IDLE=1 to run long idle test")
    idle = int(os.environ.get("E2E_IDLE_SECONDS", "600"))
    if idle < 60:
        pytest.skip("E2E_IDLE_SECONDS should be >= 60 for long idle test")
    await asyncio.sleep(idle)
    try:
        await mobile_client.send_direction(ct=0, cv=12)
    except Exception as e:
        pytest.fail(f"Connection closed after {idle}s idle: {e}")
