"""
MobileTestClient — WebSocket test client for the mobile service.

Protocol (mobile WebSocket, port 40002):
  Client → Server: JSON text messages
    login:     {"type":"login", "cs":"<W>x<H>", "dv":<int>, "at":<int>, "ga":"<gids_url>"}
    direction: {"type":"direction", "ct":<int>, "cv":<int>}
    logout:    {"type":"logout"}
  Server → Client:
    Text:   {"code":<int>, "msg":"..."} — only on error
    Binary: [0x01, frameType] + H.264  — video frame
             [0x02] + MP3              — audio frame
"""

import asyncio
import json
import os
import sys

import websockets
import yaml

_CONFIG_PATH = os.path.join(
    os.path.dirname(__file__), "..", "..", "config", "config.yaml"
)


def _load_config():
    with open(_CONFIG_PATH, "r", encoding="utf-8") as f:
        return yaml.safe_load(f)


class MobileTestClient:
    """
    Async WebSocket client for mobile service end-to-end tests.

    Usage (inside an async test):
        client = MobileTestClient()
        await client.connect(imei, imsi)
        await client.login(width=240, height=320, app_type=5)
        await client.send_direction(ct=0, cv=12)
        frame = await client.recv_binary(timeout=10)
        await client.disconnect()
    """

    GIDS_ADDR = "http://127.0.0.1:9090"

    def __init__(self):
        cfg = _load_config()
        mobile_cfg = cfg.get("mobile", {})
        host = mobile_cfg.get("address", "127.0.0.1")
        port = mobile_cfg.get("websocket_port", 40002)
        self._ws_base = f"ws://{host}:{port}/app/websocket"
        self._ws = None
        self._logged_in = False

    async def connect(self, imei: str, imsi: str):
        """Connect to ws://host:port/app/websocket/{IMEI}_{IMSI}"""
        if self._ws is not None:
            return  # already connected
        url = f"{self._ws_base}/{imei}_{imsi}"
        self._ws = await websockets.connect(url, open_timeout=10)

    async def login(self, width: int = 240, height: int = 320,
                    device_type: int = 0, app_type: int = 5,
                    gids_addr: str = None):
        """
        Send login JSON message.
        mobile processes it asynchronously; success = no error text message within timeout.
        """
        if self._ws is None:
            raise RuntimeError("Not connected")
        if self._logged_in:
            return  # already logged in, skip
        msg = {
            "type": "login",
            "cs": f"{width}x{height}",
            "dv": device_type,
            "at": app_type,
            "ga": gids_addr or self.GIDS_ADDR,
        }
        await self._ws.send(json.dumps(msg))
        self._logged_in = True

    async def send_direction(self, ct: int, cv: int):
        """Send a direction/control message. No WS-level response is expected."""
        if self._ws is None:
            raise RuntimeError("Not connected")
        await self._ws.send(json.dumps({"type": "direction", "ct": ct, "cv": cv}))

    async def recv_text(self, timeout: float = 5.0) -> dict:
        """
        Wait for a text (JSON) message from server.
        Returns parsed dict.
        Raises TimeoutError if nothing arrives in time.
        """
        if self._ws is None:
            raise RuntimeError("Not connected")
        deadline = asyncio.get_event_loop().time() + timeout
        while True:
            remaining = deadline - asyncio.get_event_loop().time()
            if remaining <= 0:
                raise TimeoutError(f"No text message received within {timeout}s")
            try:
                raw = await asyncio.wait_for(self._ws.recv(), timeout=remaining)
            except asyncio.TimeoutError:
                raise TimeoutError(f"No text message received within {timeout}s")
            if isinstance(raw, str):
                return json.loads(raw)
            # binary frame — skip and keep waiting

    async def recv_binary(self, timeout: float = 5.0) -> bytes:
        """
        Wait for a binary (media) frame from server.
        Returns raw bytes.
        Raises TimeoutError if nothing arrives in time.
        """
        if self._ws is None:
            raise RuntimeError("Not connected")
        deadline = asyncio.get_event_loop().time() + timeout
        while True:
            remaining = deadline - asyncio.get_event_loop().time()
            if remaining <= 0:
                raise TimeoutError(f"No binary frame received within {timeout}s")
            try:
                raw = await asyncio.wait_for(self._ws.recv(), timeout=remaining)
            except asyncio.TimeoutError:
                raise TimeoutError(f"No binary frame received within {timeout}s")
            if isinstance(raw, bytes):
                return raw
            # text frame — skip and keep waiting

    async def disconnect(self):
        """Send logout and close the WebSocket connection."""
        if self._ws is not None:
            try:
                await self._ws.send(json.dumps({"type": "logout"}))
            except Exception:
                pass
            await self._ws.close()
            self._ws = None
            self._logged_in = False
