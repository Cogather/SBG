"""
TLV 协议编解码
BrowserGateway 使用的复合 TLV 格式：
- Magic: 2 字节（28021）
- Count: 4 字节（字段数量）
- DataLen: 4 字节（总数据长度）
- Fields: 多个 TLV 字段，每个包含：
  - Type: 4 字节
  - Length: 4 字节
  - Value: N 字节
"""

import struct
from typing import Optional, List, Tuple

# BrowserGateway TLV 常量
MAGIC = 28021  # 魔数
ID_TYPE = 1    # 消息类型字段 ID
ID_VIDEO_DATA = 16  # 视频数据字段 ID
ID_AUDIO_DATA = 15  # 音频数据字段 ID
ID_FRAME_TYPE = 23  # 帧类型字段 ID


class TLVEncoder:
    """TLV 编码器（符合 BrowserGateway 格式）"""
    
    @staticmethod
    def encode(message_type: int, value: bytes, **kwargs) -> bytes:
        """
        编码 TLV 消息（符合 BrowserGateway 格式）
        
        Args:
            message_type: 消息类型（对应 Type.java 中的常量）
            value: 消息值（字节数组）
            **kwargs: 其他字段（如 imei, imsi, lcdWidth, lcdHeight 等）
            
        Returns:
            编码后的字节数组
        """
        # 构建 TLV 字段列表
        fields = []
        
        # 字段1：消息类型 (ID.TYPE = 1) - 必需字段
        type_data = struct.pack('>I', message_type)  # 4 字节，大端序
        fields.append((ID_TYPE, type_data))
        print(f"[DEBUG] TLV编码: message_type={message_type}, type_data={type_data.hex()}, 解码验证={struct.unpack('>I', type_data)[0]}")
        
        # 根据消息类型添加相应字段
        if message_type == 1:  # Type.LOGIN = 1
            # 登录消息需要多个字段
            if 'imei' in kwargs:
                fields.append((5, kwargs['imei'].encode('utf-8')))  # ID.IMEI = 5
            if 'imsi' in kwargs:
                fields.append((4, kwargs['imsi'].encode('utf-8')))  # ID.IMSI = 4
            if 'lcdWidth' in kwargs:
                fields.append((6, struct.pack('>I', kwargs['lcdWidth'])))  # ID.LCD_WIDTH = 6
            if 'lcdHeight' in kwargs:
                fields.append((7, struct.pack('>I', kwargs['lcdHeight'])))  # ID.LCD_HEIGHT = 7
            if 'token' in kwargs:
                fields.append((21, kwargs['token'].encode('utf-8')))  # ID.TOKEN = 21
            if 'appType' in kwargs:
                fields.append((19, struct.pack('>I', kwargs['appType'])))  # ID.APP_TYPE = 19
            if 'networkType' in kwargs:
                fields.append((48, struct.pack('>I', kwargs['networkType'])))  # ID.NETWORK_TYPE = 48
            # 如果 value 不为空，作为 content
            if value:
                fields.append((26, value))  # ID.CONTENT = 26
        elif message_type == 6:  # Type.VIDEO = 6
            # 视频帧：包含帧类型和视频数据
            if 'frame_type' in kwargs:
                frame_type_data = struct.pack('>I', kwargs['frame_type'])
                fields.append((ID_FRAME_TYPE, frame_type_data))  # ID.FRAME_TYPE = 23
            # 视频数据
            if value and len(value) > 0:
                # 如果 value 的第一个字节是帧类型（旧格式），需要去掉
                if len(value) > 0 and value[0] in [1, 2]:  # I帧或P帧
                    # 可能是旧格式：1字节帧类型 + 数据，去掉第一个字节
                    video_data = value[1:] if len(value) > 1 else b''
                else:
                    video_data = value
                if len(video_data) > 0:
                    fields.append((ID_VIDEO_DATA, video_data))
        elif message_type == 5:  # Type.AUDIO = 5
            # 音频帧
            fields.append((ID_AUDIO_DATA, value))
        elif message_type == 2:  # Type.HEARTBEATS = 2
            # 心跳消息，通常只需要 type 字段
            pass
        elif message_type == 4:  # Type.CONTROL = 4
            # 控制消息：包含 ctrlType 和 ctrlVal
            if 'ctrlType' in kwargs:
                ctrl_type_data = struct.pack('>I', kwargs['ctrlType'])
                fields.append((12, ctrl_type_data))  # ID.CTRL_TYPE = 12
            if 'ctrlVal' in kwargs:
                ctrl_val_data = struct.pack('>I', kwargs['ctrlVal'])
                fields.append((13, ctrl_val_data))  # ID.CTRL_VAL = 13
            # 如果 value 不为空，作为 content
            if value:
                fields.append((26, value))  # ID.CONTENT = 26
        else:
            # 其他消息类型：将 value 作为 CONTENT 字段
            if value:
                fields.append((26, value))  # ID.CONTENT = 26
        
        # 计算总数据长度（所有字段的 Type(4) + Length(4) + Value 的总和）
        total_data_len = 0
        for field_type, field_value in fields:
            total_data_len += 4 + 4 + len(field_value)  # Type(4) + Length(4) + Value
        
        # 构建完整的 TLV 消息
        result = bytearray()
        
        # 1. Magic (2 字节)
        result.extend(struct.pack('>H', MAGIC))
        
        # 2. Count (4 字节) - 字段数量
        result.extend(struct.pack('>I', len(fields)))
        
        # 3. DataLen (4 字节) - 总数据长度
        result.extend(struct.pack('>I', total_data_len))
        
        # 4. 写入所有字段
        for field_type, field_value in fields:
            # Type (4 字节)
            result.extend(struct.pack('>I', field_type))
            # Length (4 字节)
            result.extend(struct.pack('>I', len(field_value)))
            # Value (N 字节)
            result.extend(field_value)
        
        return bytes(result)
    
    @staticmethod
    def decode(data: bytes) -> Optional[tuple]:
        """
        解码 TLV 消息（BGW 格式：Magic(2)+Count(4)+DataLen(4)+Fields）

        Args:
            data: 待解码的字节数组

        Returns:
            (message_type, fields_dict, consumed) 元组，如果数据不完整返回 None
            fields_dict: {field_id: field_value} 的字典
        """
        # 头部最少 2+4+4=10 字节
        if len(data) < 10:
            return None

        magic = struct.unpack('>H', data[0:2])[0]
        if magic != MAGIC:
            # 跳过一字节尝试重新同步
            return None

        count = struct.unpack('>I', data[2:6])[0]
        data_len = struct.unpack('>I', data[6:10])[0]

        total_len = 10 + data_len
        if len(data) < total_len:
            return None  # 数据不完整

        # 解析所有字段
        fields_dict = {}
        offset = 10
        for _ in range(count):
            if offset + 8 > total_len:
                break
            field_type = struct.unpack('>I', data[offset:offset+4])[0]
            field_len = struct.unpack('>I', data[offset+4:offset+8])[0]
            offset += 8
            if offset + field_len > total_len:
                break
            field_value = data[offset:offset+field_len]
            fields_dict[field_type] = field_value
            offset += field_len

        # 从 ID=1 (ID_TYPE) 字段读取消息类型
        message_type = 0
        if ID_TYPE in fields_dict and len(fields_dict[ID_TYPE]) >= 4:
            message_type = struct.unpack('>I', fields_dict[ID_TYPE])[0]

        return (message_type, fields_dict, total_len)


class TLVBuffer:
    """TLV 消息缓冲区，用于处理不完整的消息"""
    
    def __init__(self):
        self.buffer = b''
    
    def append(self, data: bytes):
        """追加数据到缓冲区"""
        self.buffer += data
    
    def extract_messages(self):
        """
        从缓冲区中提取完整的 TLV 消息

        Returns:
            消息列表，每个元素为 (message_type, fields_dict)
        """
        messages = []

        while True:
            result = TLVEncoder.decode(self.buffer)
            if result is None:
                break  # 没有完整的消息

            message_type, fields_dict, consumed = result
            messages.append((message_type, fields_dict))
            self.buffer = self.buffer[consumed:]

        return messages
    
    def clear(self):
        """清空缓冲区"""
        self.buffer = b''
