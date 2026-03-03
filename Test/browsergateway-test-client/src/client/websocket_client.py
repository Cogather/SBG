"""
WebSocket 客户端（控制流和媒体流）
"""

import asyncio
import json
import struct
import time
from typing import Optional, Callable, Dict, Any
import websockets
from websockets.exceptions import ConnectionClosed


class WebSocketClient:
    """WebSocket 客户端"""
    
    def __init__(self):
        self.control_ws: Optional[websockets.WebSocketClientProtocol] = None
        self.media_ws: Optional[websockets.WebSocketClientProtocol] = None
        self.control_connected = False
        self.media_connected = False
        self.message_callback: Optional[Callable] = None
        self.media_callback: Optional[Callable] = None
        self.host = None
        self.control_port = None
        self.media_port = None
        self.media_connect_time: Optional[float] = None  # 媒体流连接开始时间（秒）
    
    async def connect_control(self, host: str, port: int, imei_and_imsi: str):
        """
        连接控制流 WebSocket
        
        Args:
            host: 服务器地址
            port: 服务器端口
            imei_and_imsi: IMEI 和 IMSI（格式：imei:imsi）
        """
        self.host = host
        self.control_port = port
        
        try:
            # WebSocket 路径：/control/websocket/{imeiAndImsi}
            uri = f"ws://{host}:{port}/control/websocket/{imei_and_imsi}"
            self.control_ws = await websockets.connect(uri)
            self.control_connected = True
            
            # 启动接收任务
            asyncio.create_task(self._receive_control_loop())
            
            return True, None
        except ConnectionRefusedError as e:
            error_msg = f"连接被拒绝: 无法连接到 {host}:{port}。请确认 BrowserGateway 服务器已启动并监听该端口。"
            print(f"连接控制流失败: {error_msg}")
            self.control_connected = False
            return False, error_msg
        except Exception as e:
            error_msg = f"连接控制流失败: {str(e)} (目标: {host}:{port})"
            print(f"连接控制流失败: {error_msg}")
            self.control_connected = False
            return False, error_msg
    
    async def connect_media(self, host: str, port: int, imei_and_imsi: str, init_params: Dict[str, Any] = None):
        """
        连接媒体流 WebSocket
        
        Args:
            host: 服务器地址
            port: 服务器端口
            imei_and_imsi: IMEI 和 IMSI（格式：imei:imsi）
            init_params: 初始化参数
        """
        self.host = host
        self.media_port = port
        
        try:
            # WebSocket 路径：/browser/websocket/{imeiAndImsi}
            uri = f"ws://{host}:{port}/browser/websocket/{imei_and_imsi}"
            self.media_ws = await websockets.connect(uri)
            self.media_connected = True
            self.media_connect_time = time.time()  # 记录连接开始时间
            
            # 发送初始化参数
            if init_params:
                await self.send_init_params(init_params)
            
            # 启动接收任务
            asyncio.create_task(self._receive_media_loop())
            
            return True, None
        except ConnectionRefusedError as e:
            error_msg = f"连接被拒绝: 无法连接到 {host}:{port}。请确认 BrowserGateway 服务器已启动并监听该端口。"
            print(f"连接媒体流失败: {error_msg}")
            self.media_connected = False
            return False, error_msg
        except Exception as e:
            error_msg = f"连接媒体流失败: {str(e)} (目标: {host}:{port})"
            print(f"连接媒体流失败: {error_msg}")
            self.media_connected = False
            return False, error_msg
    
    async def _receive_control_loop(self):
        """接收控制流消息循环"""
        while self.control_connected and self.control_ws:
            try:
                message = await self.control_ws.recv()
                
                # 尝试解析为 JSON
                try:
                    data = json.loads(message)
                    if self.message_callback:
                        self.message_callback('control', data)
                except json.JSONDecodeError:
                    # 二进制消息
                    if self.message_callback:
                        self.message_callback('control', message)
            
            except ConnectionClosed:
                self.control_connected = False
                break
            except Exception as e:
                if self.control_connected:
                    print(f"接收控制流消息错误: {e}")
                break
    
    async def _receive_media_loop(self):
        """接收媒体流消息循环"""
        while self.media_connected and self.media_ws:
            try:
                message = await self.media_ws.recv()
                
                if isinstance(message, bytes):
                    # 解析二进制消息：[4字节: 帧类型][4字节: 时间戳][4字节: 数据长度][N字节: 数据]
                    if len(message) >= 12:
                        frame_type = struct.unpack('>I', message[0:4])[0]
                        timestamp = struct.unpack('>I', message[4:8])[0]
                        data_length = struct.unpack('>I', message[8:12])[0]
                        frame_data = message[12:12 + data_length]
                        
                        if self.media_callback:
                            self.media_callback(frame_type, timestamp, frame_data)
                else:
                    # 文本消息（JSON）
                    try:
                        data = json.loads(message)
                        if self.media_callback:
                            self.media_callback('json', 0, data)
                    except json.JSONDecodeError:
                        pass
            
            except ConnectionClosed:
                self.media_connected = False
                break
            except Exception as e:
                if self.media_connected:
                    print(f"接收媒体流消息错误: {e}")
                break
    
    async def send_control_message(self, message_type: str, payload: Dict[str, Any]):
        """
        发送控制消息
        
        Args:
            message_type: 消息类型
            payload: 消息负载
        """
        if not self.control_connected:
            raise Exception("控制流未连接")
        
        message = {
            "type": message_type,
            "payload": payload,
            "timestamp": int(time.time() * 1000)
        }
        
        await self.control_ws.send(json.dumps(message))
    
    async def send_media_frame(self, frame_data: bytes, frame_type: int = 1, timestamp: int = None):
        """
        发送媒体帧
        
        Args:
            frame_data: 帧数据
            frame_type: 帧类型（1=I帧，2=P帧）
            timestamp: 时间戳（毫秒），如果为 None 则使用相对时间戳（从连接开始）
        """
        if not self.media_connected:
            raise Exception("媒体流未连接")
        
        if timestamp is None:
            # 使用相对时间戳（从连接开始计算的毫秒数），确保在 32 位无符号整数范围内
            if self.media_connect_time is not None:
                relative_time_ms = int((time.time() - self.media_connect_time) * 1000)
                # 确保时间戳在 32 位无符号整数范围内（0 到 4294967295）
                timestamp = relative_time_ms & 0xFFFFFFFF
            else:
                # 如果连接时间未记录，使用 0
                timestamp = 0
        
        # 确保时间戳在有效范围内
        if timestamp < 0 or timestamp > 0xFFFFFFFF:
            timestamp = timestamp & 0xFFFFFFFF
        
        # 构造二进制消息：[4字节: 帧类型][4字节: 时间戳][4字节: 数据长度][N字节: 数据]
        header = struct.pack('>III', frame_type, timestamp, len(frame_data))
        message = header + frame_data
        
        await self.media_ws.send(message)
    
    async def send_init_params(self, params: Dict[str, Any]):
        """发送初始化参数"""
        if not self.media_connected:
            raise Exception("媒体流未连接")
        
        message = {
            "type": "init",
            "params": params
        }
        
        await self.media_ws.send(json.dumps(message))
    
    async def send_screen_click(self, x: int, y: int):
        """发送屏幕点击事件"""
        await self.send_control_message("click", {"x": x, "y": y})
    
    async def send_keyboard_input(self, text: str):
        """发送键盘输入事件"""
        await self.send_control_message("keyboard", {"text": text})
    
    def set_message_callback(self, callback: Callable):
        """设置控制流消息回调"""
        self.message_callback = callback
    
    def set_media_callback(self, callback: Callable):
        """设置媒体流消息回调"""
        self.media_callback = callback
    
    async def disconnect_control(self):
        """断开控制流连接"""
        # 先设置标志，让接收循环退出
        self.control_connected = False
        if self.control_ws:
            try:
                # 等待一小段时间，让接收循环有机会退出
                await asyncio.sleep(0.1)
                await self.control_ws.close()
            except Exception as e:
                # 忽略关闭时的错误，连接可能已经关闭
                pass
            finally:
                self.control_ws = None
    
    async def disconnect_media(self):
        """断开媒体流连接"""
        # 先设置标志，让接收循环退出
        self.media_connected = False
        self.media_connect_time = None  # 清除连接时间
        if self.media_ws:
            try:
                # 等待一小段时间，让接收循环有机会退出
                await asyncio.sleep(0.1)
                await self.media_ws.close()
            except Exception as e:
                # 忽略关闭时的错误，连接可能已经关闭
                pass
            finally:
                self.media_ws = None
    
    async def disconnect_all(self):
        """断开所有连接"""
        # 并行关闭两个连接，提高效率
        await asyncio.gather(
            self.disconnect_control(),
            self.disconnect_media(),
            return_exceptions=True  # 即使一个失败也继续执行另一个
        )
