"""
按键控制 & 触屏控制测试

mobile WebSocket JSON 协议：
  按键: {"type":"direction", "ct":0, "cv":<值>}
    cv 值：UP=12, DOWN=13, LEFT=14, RIGHT=15, ENTER=20, BACK=18, MENU=17
  触屏: {"type":"direction", "ct":1, "cv":<action>}  action: 0=DOWN,1=UP,2=MOVE
  数字键: {"type":"direction", "ct":0, "cv":<0-9>}

注意：控制消息发出后 mobile 不返回 WS 响应，
验证方式为：发送成功且连接不关闭（无异常）。
"""

import asyncio
import pytest


async def _send_direction(client, ct: int, cv: int):
    """Send direction message and verify no exception/close."""
    try:
        await client.send_direction(ct=ct, cv=cv)
    except Exception as e:
        pytest.fail(f"send_direction(ct={ct}, cv={cv}) raised: {e}")


@pytest.mark.parametrize("name,cv", [
    ("UP",    12),
    ("DOWN",  13),
    ("LEFT",  14),
    ("RIGHT", 15),
    ("ENTER", 20),
    ("BACK",  18),
])
async def test_key_event(mobile_client, name, cv):
    """方向/功能按键：发送后连接正常，无异常。"""
    await _send_direction(mobile_client, ct=0, cv=cv)


async def test_menu_key(mobile_client):
    """菜单键 (cv=17) 发送正常。"""
    await _send_direction(mobile_client, ct=0, cv=17)


@pytest.mark.parametrize("digit", list(range(10)))
async def test_numeric_keys(mobile_client, digit):
    """数字键 0-9 发送正常。"""
    await _send_direction(mobile_client, ct=0, cv=digit)


async def test_touch_tap(mobile_client):
    """单点触摸：ACTION_DOWN → ACTION_UP，发送正常。"""
    await _send_direction(mobile_client, ct=1, cv=0)  # ACTION_DOWN
    await asyncio.sleep(0.1)
    await _send_direction(mobile_client, ct=1, cv=1)  # ACTION_UP


async def test_touch_swipe(mobile_client):
    """滑动手势：ACTION_DOWN → 3×ACTION_MOVE → ACTION_UP，发送正常。"""
    await _send_direction(mobile_client, ct=1, cv=0)  # DOWN
    for _ in range(3):
        await asyncio.sleep(0.05)
        await _send_direction(mobile_client, ct=1, cv=2)  # MOVE
    await asyncio.sleep(0.05)
    await _send_direction(mobile_client, ct=1, cv=1)  # UP
