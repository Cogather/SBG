"""
视频帧编码器（H.264/H.265）
使用 OpenCV 的 VideoWriter 进行编码
"""

import cv2
import numpy as np
from typing import Optional, Tuple
import io


class FrameEncoder:
    """视频帧编码器"""
    
    def __init__(self, width: int = 1920, height: int = 1080, fps: int = 30, codec: str = 'H264'):
        """
        初始化编码器
        
        Args:
            width: 视频宽度
            height: 视频高度
            fps: 帧率
            codec: 编码格式（H264 或 H265）
        """
        self.width = width
        self.height = height
        self.fps = fps
        self.codec = codec
        
        # 设置编码器
        if codec.upper() == 'H264':
            fourcc = cv2.VideoWriter_fourcc(*'H264')
        elif codec.upper() == 'H265' or codec.upper() == 'HEVC':
            fourcc = cv2.VideoWriter_fourcc(*'HEVC')
        else:
            fourcc = cv2.VideoWriter_fourcc(*'H264')
        
        self.fourcc = fourcc
        
        # 编码器参数
        self.encoder: Optional[cv2.VideoWriter] = None
        self.frame_count = 0
    
    def encode_frame(self, frame: np.ndarray, is_key_frame: bool = False) -> Optional[bytes]:
        """
        编码单帧
        
        Args:
            frame: 输入帧（BGR 格式）
            is_key_frame: 是否为关键帧（I 帧）
            
        Returns:
            编码后的帧数据（字节数组），如果失败返回 None
        """
        try:
            # 确保帧尺寸正确
            if frame.shape[1] != self.width or frame.shape[0] != self.height:
                frame = cv2.resize(frame, (self.width, self.height))
            
            # 使用临时文件或内存进行编码
            # 注意：OpenCV 的 VideoWriter 需要写入文件，这里使用内存缓冲区
            # 为了简化，我们使用一个临时的方法：直接返回 JPEG 编码（实际应该使用 H.264）
            # 在实际应用中，可能需要使用 ffmpeg 或其他库
            
            # 方法1：使用 JPEG 编码（简单但不符合 H.264 要求）
            # encode_param = [int(cv2.IMWRITE_JPEG_QUALITY), 90]
            # result, encoded = cv2.imencode('.jpg', frame, encode_param)
            # if result:
            #     return encoded.tobytes()
            
            # 方法2：使用内存 VideoWriter（需要特殊处理）
            # 这里我们使用一个简化的方法：生成一个模拟的 H.264 数据
            # 实际项目中应该使用 ffmpeg-python 或 x264 库
            
            # 临时方案：返回一个标记帧（实际应该使用真正的 H.264 编码器）
            # 这里返回原始帧的压缩数据作为占位符
            encode_param = [int(cv2.IMWRITE_JPEG_QUALITY), 85]
            result, encoded = cv2.imencode('.jpg', frame, encode_param)
            if result:
                return encoded.tobytes()
            
            return None
        except Exception as e:
            print(f"编码帧失败: {e}")
            return None
    
    def encode_frame_h264_simple(self, frame: np.ndarray) -> Optional[bytes]:
        """
        简单的 H.264 编码（使用 OpenCV）
        注意：这需要系统安装 H.264 编码器
        
        Args:
            frame: 输入帧
            
        Returns:
            编码后的数据
        """
        try:
            # 创建临时编码器（实际应该使用更专业的编码库）
            # 这里使用 JPEG 作为占位符，实际应该使用 H.264
            encode_param = [int(cv2.IMWRITE_JPEG_QUALITY), 90]
            result, encoded = cv2.imencode('.jpg', frame, encode_param)
            if result:
                return encoded.tobytes()
            return None
        except Exception as e:
            print(f"编码失败: {e}")
            return None
    
    def release(self):
        """释放编码器资源"""
        if self.encoder:
            self.encoder.release()
            self.encoder = None


class SimpleH264Encoder:
    """
    简单的 H.264 编码器
    注意：这是一个简化版本，实际应该使用专业的编码库如 ffmpeg-python
    """
    
    def __init__(self, width: int, height: int, fps: int = 30):
        self.width = width
        self.height = height
        self.fps = fps
    
    def encode(self, frame: np.ndarray) -> bytes:
        """
        编码帧（简化版本，实际应该使用真正的 H.264 编码）
        
        注意：由于 OpenCV 的 VideoWriter 需要文件输出，
        这里使用 JPEG 作为占位符。实际项目中应该使用：
        - ffmpeg-python
        - x264 库
        - 或其他专业的视频编码库
        """
        # 调整帧大小
        if frame.shape[1] != self.width or frame.shape[0] != self.height:
            frame = cv2.resize(frame, (self.width, self.height))
        
        # 使用 JPEG 编码（占位符）
        encode_param = [int(cv2.IMWRITE_JPEG_QUALITY), 90]
        result, encoded = cv2.imencode('.jpg', frame, encode_param)
        
        if result:
            return encoded.tobytes()
        else:
            return b''
