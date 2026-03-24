"""
WebSocket Browser Client
Simulates browser sending video stream to BrowserGateway via WebSocket
"""

import asyncio
import websockets
import json
import struct
from typing import Optional, Callable
from ..protocol.message import FrameType


class WebSocketBrowserClient:
    """WebSocket Browser Client (simulates browser side)"""

    def __init__(self):
        self.websocket: Optional[websockets.WebSocketClientProtocol] = None
        self.connected = False
        self.uri = None
        self.frame_count = 0

    async def connect(self, host: str, port: int, imei: str, imsi: str):
        """
        Connect to BrowserGateway WebSocket server

        Args:
            host: Server address
            port: WebSocket port (default 30002)
            imei: IMEI
            imsi: IMSI

        Returns:
            tuple: (success, error_message)
        """
        try:
            # Build WebSocket URI
            # Format: ws://host:port/browser/websocket/{imeiAndImsi}
            imei_and_imsi = f"{imei}{imsi}"
            self.uri = f"ws://{host}:{port}/browser/websocket/{imei_and_imsi}"

            print(f"[WebSocket Browser] Connecting to: {self.uri}")

            # Establish WebSocket connection
            self.websocket = await websockets.connect(self.uri)
            self.connected = True

            print(f"[WebSocket Browser] Connected successfully")
            return True, None

        except Exception as e:
            error_msg = f"WebSocket connection failed: {str(e)}"
            print(f"[WebSocket Browser] ERROR: {error_msg}")
            self.connected = False
            return False, error_msg

    async def send_init_params(self, width: int = 1920, height: int = 1080,
                               frame_rate: int = 30, codec: str = "H264"):
        """
        Send initialization parameters (video configuration)

        Args:
            width: Video width
            height: Video height
            frame_rate: Frame rate
            codec: Codec format
        """
        if not self.connected or not self.websocket:
            raise Exception("WebSocket not connected")

        # Send initialization parameters (JSON format)
        init_params = {
            "type": "init",
            "width": width,
            "height": height,
            "frameRate": frame_rate,
            "codec": codec
        }

        print(f"[WS BROWSER SEND] Sending init params: {init_params}")
        await self.websocket.send(json.dumps(init_params))
        print(f"[WS BROWSER SEND] Init params sent successfully")

    async def send_video_frame(self, frame_data: bytes, frame_type: int = FrameType.I_FRAME):
        """
        Send video frame (binary data)

        Args:
            frame_data: H.264 encoded video frame data
            frame_type: Frame type (1=I-frame, 2=P-frame)
        """
        if not self.connected or not self.websocket:
            raise Exception("WebSocket not connected")

        # Build binary message: type(1 byte) + frame_type(1 byte) + frame_data
        # type: 1=VIDEO, 2=AUDIO
        # frame_type: 1=I-frame, 2=P-frame
        VIDEO_TYPE = 1
        message = struct.pack('BB', VIDEO_TYPE, frame_type) + frame_data

        await self.websocket.send(message)
        self.frame_count += 1

        frame_type_str = "I-frame" if frame_type == FrameType.I_FRAME else "P-frame"
        if self.frame_count % 30 == 0:  # Print every 30 frames
            print(f"[WS BROWSER SEND] Sent frame {self.frame_count} ({frame_type_str}), size: {len(frame_data)} bytes")
        elif self.frame_count <= 3:  # Print first 3 frames
            print(f"[WS BROWSER SEND] Sent frame {self.frame_count} ({frame_type_str}), size: {len(frame_data)} bytes, hex (first 20 bytes): {frame_data[:20].hex()}")

    async def receive_messages(self, callback: Optional[Callable] = None):
        """
        Receive messages from server (if any)

        Args:
            callback: Message callback function
        """
        if not self.connected or not self.websocket:
            return

        try:
            async for message in self.websocket:
                if callback:
                    callback(message)
                else:
                    print(f"[WebSocket Browser] Received message: {message}")
        except websockets.exceptions.ConnectionClosed:
            print("[WebSocket Browser] Connection closed")
            self.connected = False

    async def disconnect(self):
        """Disconnect"""
        if self.websocket:
            await self.websocket.close()
            self.connected = False
            print("[WebSocket Browser] Disconnected")
