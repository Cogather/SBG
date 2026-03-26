"""
登录 / 登出测试

mobile WebSocket JSON 协议：
  发送: {"type":"login", "cs":"240x320", "dv":0, "at":5, "ga":"http://127.0.0.1:9090"}
  成功: 无回复（异步处理，5s 内无 error 消息即为成功）
  失败: {"code": <非0>, "msg": "..."}
"""

import asyncio
import pytest
from conftest import IMEI, IMSI


async def do_login(client, imei=IMEI, imsi=IMSI, app_type=5,
                   width=240, height=320):
    """Connect and send login. Returns error dict if received, else None (success)."""
    await client.connect(imei, imsi)
    await client.login(width=width, height=height, app_type=app_type)
    # Give mobile time to process; if error occurs it sends text JSON
    try:
        msg = await client.recv_text(timeout=5)
        return msg  # got a response (likely error)
    except TimeoutError:
        return None  # no error = success


async def test_login_success(mobile_client):
    """
    session 级 client 已登录，验证连接可用（能发送方向消息无异常）。
    """
    try:
        await mobile_client.send_direction(ct=0, cv=12)
    except Exception as e:
        pytest.fail(f"Session client not usable after login: {e}")


async def test_logout(fresh_mobile_client):
    """
    登录成功后正常断开，再次用同一 IMEI/IMSI 可重新登录。
    """
    result = await do_login(fresh_mobile_client)
    if result is not None:
        assert result.get("code", 0) == 0, f"First login failed: {result}"

    await fresh_mobile_client.disconnect()
    await asyncio.sleep(1)

    # Reconnect and login again
    result2 = await do_login(fresh_mobile_client)
    if result2 is not None:
        assert result2.get("code", 0) == 0, f"Re-login failed: {result2}"
