"""
统一终端模拟器 - 整合用户终端和沐恩终端功能

提供统一接口用于：
- 登录流程（TCP 控制流）
- 视频流发送（WebSocket 浏览器客户端）
- 视频流接收（TCP 媒体流）
- 控制事件发送（按键、触摸）
- 心跳保持
"""

import asyncio
import logging
import time
from typing import Optional, Callable, Dict, Any
from ..client.tcp_client import TCPClient
from ..client.websocket_browser_client import WebSocketBrowserClient
from ..mock.video_generator import VideoGenerator
from ..protocol.tlv import TLVEncoder
from ..protocol.message import MessageType

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


class UnifiedTerminal:
    """统一终端模拟器"""

    def __init__(self, config: Dict[str, Any]):
        """
        初始化统一终端

        Args:
            config: 配置字典，包含：
                - control_host: 控制流主机
                - control_port: 控制流端口
                - media_host: 媒体流主机
                - media_port: 媒体流端口
                - browser_ws_url: 浏览器 WebSocket URL
        """
        self.config = config

        # TCP 客户端
        self.control_client = TCPClient()
        self.media_client = TCPClient()

        # 连接参数
        self.control_host = config.get('control_host', '127.0.0.1')
        self.control_port = config.get('control_port', 30001)
        self.media_host = config.get('media_host', '127.0.0.1')
        self.media_port = config.get('media_port', 30011)

        # WebSocket 浏览器客户端
        self.browser_client: Optional[WebSocketBrowserClient] = None

        # 视频生成器
        self.video_generator = VideoGenerator()

        # 状态
        self.is_logged_in = False
        self.session_id: Optional[str] = None
        self.video_streaming = False
        self.video_receiving = False

        # 统计
        self.stats = {
            'frames_sent': 0,
            'frames_received': 0,
            'control_events_sent': 0,
            'heartbeats_sent': 0,
            'start_time': None,
            'end_time': None
        }

    async def login(self, imei: str, imsi: str, token: str) -> bool:
        """
        登录流程

        Args:
            imei: 设备 IMEI
            imsi: 设备 IMSI
            token: 认证 token

        Returns:
            登录是否成功
        """
        try:
            logger.info(f"Starting login: IMEI={imei}, IMSI={imsi}")

            # 设置接收回调（在连接之前设置）
            self.login_response = None

            def on_message(msg_type, msg_data):
                data_len = len(msg_data) if isinstance(msg_data, (bytes, bytearray)) else len(msg_data)
                logger.info(f"Received message: type={msg_type}, fields={data_len}")
                self.login_response = (msg_type, msg_data)

            self.control_client.set_message_callback(on_message)

            # 连接控制流
            success, error = self.control_client.connect(
                self.control_host,
                self.control_port,
                use_tls=False
            )
            if not success:
                logger.error(f"Failed to connect to control server: {error}")
                return False

            logger.info("Connected to control server")

            # 等待一下让接收线程启动
            await asyncio.sleep(0.5)

            # 发送 LOGIN 消息
            self.session_id = f"{imei}_{imsi}"
            self.control_client.login(
                imei=imei,
                imsi=imsi,
                token=token,
                lcd_width=1920,
                lcd_height=1080
            )

            logger.info("LOGIN message sent, waiting for ACK...")

            # 等待 ACK 响应（最多 15 秒）
            for i in range(150):
                await asyncio.sleep(0.1)
                if self.login_response:
                    logger.info(f"Got response after {i * 0.1:.1f} seconds")
                    break
                if i % 10 == 0:
                    logger.debug(f"Still waiting... {i * 0.1:.1f}s elapsed")

            if not self.login_response:
                logger.error("No ACK received (timeout)")
                return False

            msg_type, msg_data = self.login_response
            logger.info(f"Received response: type={msg_type}")

            if msg_type != MessageType.ACK:
                logger.error(f"Expected ACK (type={MessageType.ACK}), got {msg_type}")
                return False

            # 简单解析 ACK 数据（假设成功）
            logger.info("Login successful!")
            self.is_logged_in = True
            self.stats['start_time'] = time.time()
            return True

        except Exception as e:
            logger.error(f"Login error: {e}", exc_info=True)
            return False

    async def start_video_streaming(self, width: int = 1920, height: int = 1080,
                                   fps: int = 30, codec: str = "H264") -> bool:
        """
        启动视频流（模拟浏览器发送视频）

        Args:
            width: 视频宽度
            height: 视频高度
            fps: 帧率
            codec: 编码格式

        Returns:
            是否成功启动
        """
        try:
            if not self.is_logged_in:
                logger.error("Not logged in, cannot start video streaming")
                return False

            logger.info(f"Starting video streaming: {width}x{height} @ {fps}fps, codec={codec}")

            # 创建 WebSocket 浏览器客户端
            ws_url = self.config.get('browser_ws_url', 'ws://127.0.0.1:30002')
            self.browser_client = WebSocketBrowserClient(ws_url)

            if not await self.browser_client.connect():
                logger.error("Failed to connect browser WebSocket")
                return False

            # 发送初始化参数
            init_params = {
                'width': width,
                'height': height,
                'fps': fps,
                'codec': codec,
                'sessionId': self.session_id
            }

            if not await self.browser_client.send_init_params(init_params):
                logger.error("Failed to send init params")
                return False

            logger.info("Video streaming initialized")
            self.video_streaming = True

            # 启动视频发送任务
            asyncio.create_task(self._video_streaming_task(fps))

            return True

        except Exception as e:
            logger.error(f"Start video streaming error: {e}", exc_info=True)
            return False

    async def _video_streaming_task(self, fps: int):
        """视频流发送任务"""
        frame_interval = 1.0 / fps
        frame_count = 0

        try:
            while self.video_streaming and self.browser_client:
                start_time = time.time()

                # 生成视频帧
                is_keyframe = (frame_count % 30 == 0)  # 每 30 帧一个关键帧
                frame_data = self.video_generator.generate_frame(
                    frame_number=frame_count,
                    is_keyframe=is_keyframe
                )

                # 发送视频帧
                if await self.browser_client.send_video_frame(frame_data, is_keyframe):
                    self.stats['frames_sent'] += 1
                    frame_count += 1
                else:
                    logger.warning(f"Failed to send frame {frame_count}")

                # 控制帧率
                elapsed = time.time() - start_time
                sleep_time = max(0, frame_interval - elapsed)
                if sleep_time > 0:
                    await asyncio.sleep(sleep_time)

        except Exception as e:
            logger.error(f"Video streaming task error: {e}", exc_info=True)
        finally:
            logger.info(f"Video streaming stopped, sent {frame_count} frames")

    async def receive_video(self, callback: Optional[Callable[[bytes, bool], None]] = None) -> bool:
        """
        接收视频流（模拟用户终端接收）

        Args:
            callback: 接收到帧时的回调函数 callback(frame_data, is_keyframe)

        Returns:
            是否成功启动接收
        """
        try:
            if not self.is_logged_in:
                logger.error("Not logged in, cannot receive video")
                return False

            logger.info("Starting video receiving...")

            # 连接媒体流
            if not await self.media_client.connect():
                logger.error("Failed to connect to media server")
                return False

            self.video_receiving = True

            # 启动视频接收任务
            asyncio.create_task(self._video_receiving_task(callback))

            return True

        except Exception as e:
            logger.error(f"Receive video error: {e}", exc_info=True)
            return False

    async def _video_receiving_task(self, callback: Optional[Callable[[bytes, bool], None]]):
        """视频流接收任务"""
        try:
            while self.video_receiving and self.media_client:
                # 接收视频帧
                frame_data = await self.media_client.receive(timeout=5.0)
                if not frame_data:
                    continue

                self.stats['frames_received'] += 1

                # 解析帧类型（简化版，实际需要解析 TLV）
                is_keyframe = (self.stats['frames_received'] % 30 == 1)

                # 回调
                if callback:
                    callback(frame_data, is_keyframe)

        except Exception as e:
            logger.error(f"Video receiving task error: {e}", exc_info=True)
        finally:
            logger.info(f"Video receiving stopped, received {self.stats['frames_received']} frames")

    async def send_key_event(self, key_code: int, action: int = 0) -> bool:
        """
        发送按键事件

        Args:
            key_code: 按键码
            action: 动作 (0=按下, 1=释放)

        Returns:
            是否发送成功
        """
        try:
            if not self.is_logged_in:
                logger.error("Not logged in")
                return False

            logger.info(f"Sending key event: code={key_code}, action={action}")

            # 构造按键事件
            key_data = {
                'keyCode': key_code,
                'action': action,
                'timestamp': int(time.time() * 1000)
            }

            encoder = TLVEncoder()
            key_msg = encoder.encode_message(MessageType.KEY_EVENT, key_data)

            if not await self.control_client.send(key_msg):
                logger.error("Failed to send key event")
                return False

            # 等待 ACK
            response = await self.control_client.receive(timeout=5.0)
            if response:
                decoded = TLVEncoder.decode(response)
                if decoded:
                    msg_type, ack_value, _ = decoded
                    if msg_type == MessageType.ACK:
                        # 简单解析 code
                        if len(ack_value) >= 4:
                            import struct
                            code = struct.unpack('>I', ack_value[0:4])[0]
                            if code == 0:
                                self.stats['control_events_sent'] += 1
                                logger.info("Key event ACK received")
                                return True

            return False

        except Exception as e:
            logger.error(f"Send key event error: {e}", exc_info=True)
            return False

    async def send_touch_event(self, x: int, y: int, action: int = 0) -> bool:
        """
        发送触摸事件

        Args:
            x: X 坐标
            y: Y 坐标
            action: 动作 (0=按下, 1=移动, 2=释放)

        Returns:
            是否发送成功
        """
        try:
            if not self.is_logged_in:
                logger.error("Not logged in")
                return False

            logger.info(f"Sending touch event: ({x}, {y}), action={action}")

            # 构造触摸事件
            touch_data = {
                'x': x,
                'y': y,
                'action': action,
                'timestamp': int(time.time() * 1000)
            }

            encoder = TLVEncoder()
            touch_msg = encoder.encode_message(MessageType.TOUCH_EVENT, touch_data)

            if not await self.control_client.send(touch_msg):
                logger.error("Failed to send touch event")
                return False

            # 等待 ACK
            response = await self.control_client.receive(timeout=5.0)
            if response:
                decoded = TLVEncoder.decode(response)
                if decoded:
                    msg_type, ack_value, _ = decoded
                    if msg_type == MessageType.ACK:
                        # 简单解析 code
                        if len(ack_value) >= 4:
                            import struct
                            code = struct.unpack('>I', ack_value[0:4])[0]
                            if code == 0:
                                self.stats['control_events_sent'] += 1
                                logger.info("Touch event ACK received")
                                return True

            return False

        except Exception as e:
            logger.error(f"Send touch event error: {e}", exc_info=True)
            return False

    async def send_heartbeat(self) -> bool:
        """
        发送心跳

        Returns:
            是否发送成功
        """
        try:
            if not self.is_logged_in:
                return False

            logger.debug("Sending heartbeat")

            heartbeat_data = {
                'timestamp': int(time.time() * 1000),
                'sessionId': self.session_id
            }

            encoder = TLVEncoder()
            heartbeat_msg = encoder.encode_message(MessageType.HEARTBEAT, heartbeat_data)

            if await self.control_client.send(heartbeat_msg):
                self.stats['heartbeats_sent'] += 1
                return True

            return False

        except Exception as e:
            logger.error(f"Send heartbeat error: {e}", exc_info=True)
            return False

    async def disconnect(self):
        """断开所有连接"""
        logger.info("Disconnecting all connections...")

        self.video_streaming = False
        self.video_receiving = False
        self.stats['end_time'] = time.time()

        # 断开浏览器客户端
        if self.browser_client:
            await self.browser_client.disconnect()
            self.browser_client = None

        # 断开 TCP 客户端
        self.control_client.disconnect()
        self.media_client.disconnect()

        self.is_logged_in = False
        logger.info("All connections closed")

    def get_stats(self) -> Dict[str, Any]:
        """获取统计信息"""
        stats = self.stats.copy()

        if stats['start_time'] and stats['end_time']:
            duration = stats['end_time'] - stats['start_time']
            stats['duration_seconds'] = duration

            if duration > 0:
                stats['avg_fps_sent'] = stats['frames_sent'] / duration
                stats['avg_fps_received'] = stats['frames_received'] / duration

        return stats

    def print_stats(self):
        """打印统计信息"""
        stats = self.get_stats()

        logger.info("=" * 60)
        logger.info("Terminal Statistics")
        logger.info("=" * 60)
        logger.info(f"Session ID: {self.session_id}")
        logger.info(f"Duration: {stats.get('duration_seconds', 0):.2f} seconds")
        logger.info(f"Frames sent: {stats['frames_sent']}")
        logger.info(f"Frames received: {stats['frames_received']}")
        logger.info(f"Control events sent: {stats['control_events_sent']}")
        logger.info(f"Heartbeats sent: {stats['heartbeats_sent']}")

        if 'avg_fps_sent' in stats:
            logger.info(f"Average FPS (sent): {stats['avg_fps_sent']:.2f}")
        if 'avg_fps_received' in stats:
            logger.info(f"Average FPS (received): {stats['avg_fps_received']:.2f}")

        logger.info("=" * 60)
