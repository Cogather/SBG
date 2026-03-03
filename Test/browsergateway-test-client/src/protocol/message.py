"""
消息类型定义
"""

from enum import IntEnum


class MessageType(IntEnum):
    """TCP 消息类型（对应 BrowserGateway 的 Type.java）"""
    LOGIN = 1      # Type.LOGIN = 1
    HEARTBEATS = 2  # Type.HEARTBEATS = 2
    CONTROL = 4   # Type.CONTROL = 4
    AUDIO = 5     # Type.AUDIO = 5
    VIDEO = 6     # Type.VIDEO = 6
    ACK = 7       # Type.ACK = 7
    # 兼容旧代码
    LOGOUT = 2
    HEARTBEAT = 2
    KEY_EVENT = 4
    TOUCH_EVENT = 4
    VIDEO_FRAME = 6
    AUDIO_FRAME = 5
    RESPONSE = 7


class KeyCode(IntEnum):
    """按键代码"""
    KEY_UP = 19
    KEY_DOWN = 20
    KEY_LEFT = 21
    KEY_RIGHT = 22
    KEY_ENTER = 66
    KEY_BACK = 4
    KEY_HOME = 3
    KEY_MENU = 82


class TouchAction(IntEnum):
    """触摸动作"""
    ACTION_DOWN = 0
    ACTION_UP = 1
    ACTION_MOVE = 2


class FrameType(IntEnum):
    """视频帧类型"""
    I_FRAME = 1
    P_FRAME = 2
    AUDIO_FRAME = 3
