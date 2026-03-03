"""
TCP 客户端（控制流和媒体流）
支持 TLS 加密连接和 TLV 协议
"""

import socket
import ssl
import struct
import threading
import time
from typing import Optional, Callable
from ..protocol.tlv import TLVEncoder, TLVBuffer
from ..protocol.message import MessageType, KeyCode, TouchAction, FrameType


class TCPClient:
    """TCP 客户端"""
    
    def __init__(self):
        self.socket: Optional[socket.socket] = None
        self.ssl_socket: Optional[ssl.SSLSocket] = None
        self.connected = False
        self.host = None
        self.port = None
        self.use_tls = True
        self.buffer = TLVBuffer()
        self.message_callback: Optional[Callable] = None
        self.receive_thread: Optional[threading.Thread] = None
        self.running = False
        self.last_error = None
    
    def connect(self, host: str, port: int, use_tls: bool = True):
        """
        建立 TCP 连接
        
        Args:
            host: 服务器地址
            port: 服务器端口
            use_tls: 是否使用 TLS 加密
        
        Returns:
            tuple: (是否成功, 错误信息)
        """
        self.host = host
        self.port = port
        self.use_tls = use_tls
        self.last_error = None
        
        try:
            # 创建 socket
            self.socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            self.socket.settimeout(10)
            
            if use_tls:
                # 创建 SSL 上下文（不验证证书，用于测试）
                context = ssl.create_default_context()
                context.check_hostname = False
                context.verify_mode = ssl.CERT_NONE
                
                # 建立连接
                self.socket.connect((host, port))
                self.ssl_socket = context.wrap_socket(self.socket, server_hostname=host)
                self.connected = True
                
                # 启动接收线程
                self.running = True
                self.receive_thread = threading.Thread(target=self._receive_loop, daemon=True)
                self.receive_thread.start()
            else:
                self.socket.connect((host, port))
                self.connected = True
                
                # 启动接收线程
                self.running = True
                self.receive_thread = threading.Thread(target=self._receive_loop, daemon=True)
                self.receive_thread.start()
            
            return True, None
        except ConnectionRefusedError as e:
            error_msg = f"连接被拒绝: 无法连接到 {host}:{port}。请确认 BrowserGateway 服务器已启动并监听该端口。"
            self.last_error = error_msg
            print(f"连接失败: {error_msg}")
            self.connected = False
            return False, error_msg
        except socket.timeout as e:
            error_msg = f"连接超时: 无法在 10 秒内连接到 {host}:{port}。请检查服务器地址和端口配置。"
            self.last_error = error_msg
            print(f"连接失败: {error_msg}")
            self.connected = False
            return False, error_msg
        except Exception as e:
            error_msg = f"连接失败: {str(e)} (目标: {host}:{port})"
            self.last_error = error_msg
            print(f"连接失败: {error_msg}")
            self.connected = False
            return False, error_msg
    
    def disconnect(self):
        """断开连接"""
        self.running = False
        self.connected = False
        
        try:
            if self.ssl_socket:
                self.ssl_socket.close()
            elif self.socket:
                self.socket.close()
        except:
            pass
        
        self.socket = None
        self.ssl_socket = None
    
    def _send(self, data: bytes):
        """发送数据"""
        if not self.connected:
            raise Exception("未连接")
        
        try:
            if self.ssl_socket:
                self.ssl_socket.sendall(data)
            else:
                self.socket.sendall(data)
            print(f"[DEBUG] TCP 客户端成功发送 {len(data)} 字节数据")
        except Exception as e:
            print(f"发送数据失败: {e}")
            self.connected = False
            raise
    
    def _receive_loop(self):
        """接收数据循环"""
        while self.running and self.connected:
            try:
                if self.ssl_socket:
                    data = self.ssl_socket.recv(4096)
                else:
                    data = self.socket.recv(4096)
                
                if not data:
                    break
                
                self.buffer.append(data)
                messages = self.buffer.extract_messages()
                
                for msg_type, msg_value in messages:
                    if self.message_callback:
                        self.message_callback(msg_type, msg_value)
            
            except socket.timeout:
                continue
            except Exception as e:
                if self.running:
                    print(f"接收数据错误: {e}")
                break
        
        self.connected = False
    
    def set_message_callback(self, callback: Callable):
        """设置消息回调函数"""
        self.message_callback = callback
    
    def login(self, imei: str, imsi: str, lcd_width: int = 1920, lcd_height: int = 1080, 
              token: str = "", app_type: int = 0, network_type: int = 0):
        """
        发送登录消息
        
        Args:
            imei: IMEI
            imsi: IMSI
            lcd_width: 屏幕宽度（默认 1920）
            lcd_height: 屏幕高度（默认 1080）
            token: 用户 token（可选）
            app_type: 应用类型（可选）
            network_type: 网络类型（可选）
        """
        # 使用新的 TLV 格式，传递多个字段
        tlv_data = TLVEncoder.encode(
            MessageType.LOGIN, 
            b'',  # value 为空，使用字段传递
            imei=imei,
            imsi=imsi,
            lcdWidth=lcd_width,
            lcdHeight=lcd_height,
            token=token,
            appType=app_type,
            networkType=network_type
        )
        self._send(tlv_data)
    
    def send_heartbeat(self):
        """发送心跳消息"""
        # Type.HEARTBEATS = 2
        heartbeat_data = b''  # 心跳消息可以为空
        tlv_data = TLVEncoder.encode(MessageType.HEARTBEATS, heartbeat_data)
        self._send(tlv_data)
    
    def send_key_event(self, key_code: int, action: int):
        """
        发送按键事件
        
        Args:
            key_code: 按键代码
            action: 动作（0=按下，1=释放）
        """
        # 使用 Type.CONTROL = 4，ctrlType 表示按键事件类型，ctrlVal 包含 key_code 和 action
        # ctrlType: 1=按键事件, 2=触摸事件, 3=鼠标事件等
        # ctrlVal: 对于按键事件，高16位是key_code，低16位是action
        ctrl_val = (key_code << 16) | action
        print(f"[DEBUG] TCP 客户端准备发送按键事件: MessageType.CONTROL={MessageType.CONTROL}, key_code={key_code}, action={action}, ctrlVal={ctrl_val}")
        tlv_data = TLVEncoder.encode(
            MessageType.CONTROL,  # Type.CONTROL = 4
            b'',  # value 为空
            ctrlType=1,  # 1=按键事件
            ctrlVal=ctrl_val
        )
        print(f"[DEBUG] TCP 客户端发送按键事件: TLV数据大小={len(tlv_data)}字节, 前10字节={tlv_data[:10].hex() if len(tlv_data) >= 10 else tlv_data.hex()}")
        self._send(tlv_data)
    
    def send_touch_event(self, x: int, y: int, action: int):
        """
        发送触摸事件
        
        Args:
            x: X 坐标
            y: Y 坐标
            action: 动作（0=按下，1=释放，2=移动）
        """
        # 使用 Type.CONTROL = 4，ctrlType 表示触摸事件类型
        # ctrlVal: 对于触摸事件，需要编码 x, y, action
        # 由于 ctrlVal 是 int32，我们需要将 x, y, action 编码到 value 中
        # 或者使用 ctrlVal 的高16位存储 x 的低16位，低16位存储 y 的低16位
        # 但更好的方式是：ctrlType=2 表示触摸事件，将 x, y, action 作为 content
        touch_data = struct.pack('>IIB', x, y, action)  # 9 字节：x(4) + y(4) + action(1)
        tlv_data = TLVEncoder.encode(
            MessageType.CONTROL,  # Type.CONTROL = 4
            touch_data,  # value 包含触摸数据
            ctrlType=2,  # 2=触摸事件
            ctrlVal=action  # ctrlVal 存储 action
        )
        print(f"[DEBUG] TCP 客户端发送触摸事件: x={x}, y={y}, action={action}")
        self._send(tlv_data)
    
    def send_video_frame(self, frame_data: bytes, frame_type: int = FrameType.I_FRAME):
        """
        发送视频帧
        
        Args:
            frame_data: 视频帧数据（已编码）
            frame_type: 帧类型（1=I帧，2=P帧）
        """
        # 使用新的 TLV 格式，传递帧类型和视频数据
        # Type.VIDEO = 6 (对应 BrowserGateway 的 Type.VIDEO)
        tlv_data = TLVEncoder.encode(
            6,  # Type.VIDEO = 6
            frame_data,  # 视频数据（不包含帧类型头部）
            frame_type=frame_type  # 帧类型作为单独字段
        )
        print(f"[DEBUG] TCP 客户端发送视频帧: TLV 数据大小={len(tlv_data)} 字节, 原始帧大小={len(frame_data)} 字节, 帧类型={frame_type}")
        self._send(tlv_data)
    
    def send_audio_frame(self, audio_data: bytes):
        """
        发送音频帧
        
        Args:
            audio_data: 音频帧数据
        """
        tlv_data = TLVEncoder.encode(MessageType.AUDIO_FRAME, audio_data)
        self._send(tlv_data)
    
    def logout(self):
        """发送登出消息"""
        logout_data = b''
        tlv_data = TLVEncoder.encode(MessageType.LOGOUT, logout_data)
        self._send(tlv_data)
        time.sleep(0.1)  # 等待消息发送
        self.disconnect()
