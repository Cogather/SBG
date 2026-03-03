"""
视频流 Mock 生成器
生成合成视频帧（彩色渐变、移动图案）
支持 I 帧和 P 帧生成
"""

import cv2
import numpy as np
import threading
import time
from typing import Callable, Optional
from .frame_encoder import FrameEncoder, SimpleH264Encoder
from ..protocol.message import FrameType


class VideoGenerator:
    """视频流生成器"""
    
    def __init__(self, width: int = 1920, height: int = 1080, fps: int = 30, 
                 codec: str = 'H264', gop_size: int = 29):
        """
        初始化视频生成器
        
        Args:
            width: 视频宽度
            height: 视频高度
            fps: 帧率
            codec: 编码格式
            gop_size: GOP 大小（I 帧间隔）
        """
        self.width = width
        self.height = height
        self.fps = fps
        self.codec = codec
        self.gop_size = gop_size
        
        self.encoder = SimpleH264Encoder(width, height, fps)
        self.frame_count = 0
        self.running = False
        self.streaming_thread: Optional[threading.Thread] = None
        self.frame_callback: Optional[Callable] = None
        self.frame_interval = 1.0 / fps
    
    def generate_frame(self, frame_number: int = None, frame_type: int = FrameType.I_FRAME) -> np.ndarray:
        """
        生成单帧
        
        Args:
            frame_number: 帧编号（用于生成动态图案）
            frame_type: 帧类型（1=I帧，2=P帧）
            
        Returns:
            生成的帧（BGR 格式）
        """
        if frame_number is None:
            frame_number = self.frame_count
        
        # 创建彩色渐变背景
        frame = np.zeros((self.height, self.width, 3), dtype=np.uint8)
        
        # 生成渐变背景（HSV 色彩空间）
        for y in range(self.height):
            hue = int((y / self.height) * 180)  # 0-180
            frame[y, :] = [hue, 255, 255]
        
        # 转换为 BGR
        frame = cv2.cvtColor(frame, cv2.COLOR_HSV2BGR)
        
        # 添加移动图案（圆形）
        center_x = int(self.width / 2 + 200 * np.sin(frame_number * 0.1))
        center_y = int(self.height / 2 + 200 * np.cos(frame_number * 0.1))
        radius = 100 + int(50 * np.sin(frame_number * 0.05))
        
        cv2.circle(frame, (center_x, center_y), radius, (255, 255, 255), -1)
        
        # 添加帧编号文本
        text = f"Frame: {frame_number} Type: {'I' if frame_type == FrameType.I_FRAME else 'P'}"
        cv2.putText(frame, text, (50, 50), cv2.FONT_HERSHEY_SIMPLEX, 2, (0, 0, 0), 3)
        
        # 添加时间戳
        timestamp_text = f"Time: {time.strftime('%H:%M:%S')}"
        cv2.putText(frame, timestamp_text, (50, 150), cv2.FONT_HERSHEY_SIMPLEX, 2, (0, 0, 0), 3)
        
        # 添加移动的矩形
        rect_x = int(100 + 300 * np.sin(frame_number * 0.15))
        rect_y = int(200 + 200 * np.cos(frame_number * 0.12))
        cv2.rectangle(frame, (rect_x, rect_y), (rect_x + 200, rect_y + 150), (0, 255, 0), 5)
        
        return frame
    
    def encode_frame(self, frame: np.ndarray, frame_type: int = FrameType.I_FRAME) -> Optional[bytes]:
        """
        编码帧
        
        Args:
            frame: 输入帧
            frame_type: 帧类型
            
        Returns:
            编码后的数据
        """
        return self.encoder.encode(frame)
    
    def start_streaming(self, callback: Callable):
        """
        开始流式传输
        
        Args:
            callback: 回调函数，参数为 (frame_data, frame_type, raw_frame)
                     - frame_data: 编码后的帧数据（字节）
                     - frame_type: 帧类型（I帧或P帧）
                     - raw_frame: 原始帧（numpy数组，用于前端显示）
        """
        if self.running:
            return
        
        self.frame_callback = callback
        self.running = True
        self.frame_count = 0
        
        self.streaming_thread = threading.Thread(target=self._streaming_loop, daemon=True)
        self.streaming_thread.start()
    
    def stop_streaming(self):
        """停止流式传输"""
        self.running = False
        if self.streaming_thread:
            self.streaming_thread.join(timeout=2.0)
    
    def _streaming_loop(self):
        """流式传输循环"""
        last_time = time.time()
        
        while self.running:
            current_time = time.time()
            elapsed = current_time - last_time
            
            # 控制帧率
            if elapsed < self.frame_interval:
                time.sleep(self.frame_interval - elapsed)
            
            # 确定帧类型（每 GOP 大小生成一个 I 帧）
            if self.frame_count % self.gop_size == 0:
                frame_type = FrameType.I_FRAME
            else:
                frame_type = FrameType.P_FRAME
            
            # 生成帧
            frame = self.generate_frame(self.frame_count, frame_type)
            
            # 编码帧
            encoded_frame = self.encode_frame(frame, frame_type)
            
            if encoded_frame and self.frame_callback:
                # 传递编码帧和原始帧（用于前端显示）
                self.frame_callback(encoded_frame, frame_type, frame)
            
            self.frame_count += 1
            last_time = time.time()
    
    def get_frame(self, frame_number: int = None) -> tuple:
        """
        获取单帧（用于测试）
        
        Args:
            frame_number: 帧编号
            
        Returns:
            (frame_data, frame_type) 元组
        """
        if frame_number is None:
            frame_number = self.frame_count
            self.frame_count += 1
        
        frame_type = FrameType.I_FRAME if frame_number % self.gop_size == 0 else FrameType.P_FRAME
        frame = self.generate_frame(frame_number, frame_type)
        encoded_frame = self.encode_frame(frame, frame_type)
        
        return (encoded_frame, frame_type) if encoded_frame else (None, None)
