"""
FastAPI Web 应用
提供模拟手机 Web 界面
"""

import asyncio
import yaml
import os
from fastapi import FastAPI, WebSocket, WebSocketDisconnect, Request
from fastapi.staticfiles import StaticFiles
from fastapi.templating import Jinja2Templates
from fastapi.responses import HTMLResponse
from typing import Dict, Optional
import json

from ..client.tcp_client import TCPClient
from ..client.websocket_client import WebSocketClient
from ..client.rest_client import RESTClient
from ..mock.video_generator import VideoGenerator
from ..protocol.message import KeyCode, TouchAction, FrameType

app = FastAPI(title="BrowserGateway 测试客户端")

# 模板和静态文件
templates = Jinja2Templates(directory=os.path.join(os.path.dirname(__file__), "templates"))
app.mount("/static", StaticFiles(directory=os.path.join(os.path.dirname(__file__), "static")), name="static")

# 全局状态
clients: Dict[str, Dict] = {}  # 存储客户端连接
config = None


def load_config():
    """加载配置"""
    global config
    config_path = os.path.join(os.path.dirname(__file__), "../../config/config.yaml")
    with open(config_path, 'r', encoding='utf-8') as f:
        config = yaml.safe_load(f)
    return config


@app.on_event("startup")
async def startup():
    """启动时加载配置"""
    load_config()


@app.get("/", response_class=HTMLResponse)
async def index(request: Request):
    """主页面"""
    return templates.TemplateResponse("index.html", {"request": request, "config": config})


@app.websocket("/ws/{client_id}")
async def websocket_endpoint(websocket: WebSocket, client_id: str):
    """WebSocket 端点，用于前端通信"""
    await websocket.accept()
    clients[client_id] = {
        "websocket": websocket,
        "tcp_client": None,
        "ws_client": None,
        "rest_client": None,
        "video_generator": None,
        "connected": False
    }
    
    try:
        while True:
            data = await websocket.receive_json()
            await handle_message(client_id, data)
    except WebSocketDisconnect:
        pass
    finally:
        # 清理资源
        if client_id in clients:
            client = clients[client_id]
            if client.get("tcp_client"):
                client["tcp_client"].disconnect()
            if client.get("ws_client"):
                await client["ws_client"].disconnect_all()
            if client.get("video_generator"):
                client["video_generator"].stop_streaming()
            del clients[client_id]


async def handle_message(client_id: str, message: Dict):
    """处理来自前端的消息"""
    client = clients.get(client_id)
    if not client:
        return
    
    msg_type = message.get("type")
    
    if msg_type == "connect_tcp_control":
        await handle_connect_tcp_control(client, message)
    elif msg_type == "connect_tcp_media":
        await handle_connect_tcp_media(client, message)
    elif msg_type == "connect_websocket_control":
        await handle_connect_websocket_control(client, message)
    elif msg_type == "connect_websocket_media":
        await handle_connect_websocket_media(client, message)
    elif msg_type == "disconnect":
        await handle_disconnect(client)
    elif msg_type == "key_event":
        await handle_key_event(client, message)
    elif msg_type == "touch_event":
        await handle_touch_event(client, message)
    elif msg_type == "start_video_stream":
        await handle_start_video_stream(client, message)
    elif msg_type == "stop_video_stream":
        await handle_stop_video_stream(client)
    elif msg_type == "rest_api":
        await handle_rest_api(client, message)


async def handle_connect_tcp_control(client: Dict, message: Dict):
    """处理 TCP 控制流连接"""
    try:
        host = message.get("host", config["server"]["address"])
        # 支持 TLS 和非 TLS 模式
        use_tls = message.get("use_tls", False)
        if use_tls:
            port = message.get("port", config["server"]["tcp"].get("control_tls_port", 30012))
        else:
            port = message.get("port", config["server"]["tcp"].get("control_port", 30001))
        imei = message.get("imei", config["test"]["imei"])
        imsi = message.get("imsi", config["test"]["imsi"])
        
        tcp_client = TCPClient()
        
        def message_callback(msg_type, msg_value):
            asyncio.create_task(send_to_frontend(client["websocket"], {
                "type": "tcp_message",
                "message_type": msg_type,
                "data": msg_value.hex() if isinstance(msg_value, bytes) else str(msg_value)
            }))
        
        tcp_client.set_message_callback(message_callback)
        
        success, error_msg = tcp_client.connect(host, port, use_tls=use_tls)
        if success:
            # 跳过登录步骤（用于测试）
            # tcp_client.login(imei, imsi)
            print("[INFO] 跳过登录步骤，直接连接 TCP 控制流")
            client["tcp_client"] = tcp_client
            client["connected"] = True
            
            await send_to_frontend(client["websocket"], {
                "type": "connection_status",
                "status": "connected",
                "protocol": "tcp_control"
            })
        else:
            await send_to_frontend(client["websocket"], {
                "type": "connection_status",
                "status": "failed",
                "error": error_msg or "连接失败",
                "protocol": "tcp_control"
            })
    except Exception as e:
        await send_to_frontend(client["websocket"], {
            "type": "error",
            "message": str(e)
        })


async def handle_connect_tcp_media(client: Dict, message: Dict):
    """处理 TCP 媒体流连接"""
    try:
        host = message.get("host", config["server"]["address"])
        # 支持 TLS 和非 TLS 模式
        use_tls = message.get("use_tls", False)
        if use_tls:
            port = message.get("port", config["server"]["tcp"].get("media_tls_port", 30013))
        else:
            port = message.get("port", config["server"]["tcp"].get("media_port", 30011))
        imei = message.get("imei", config["test"]["imei"])
        imsi = message.get("imsi", config["test"]["imsi"])
        
        tcp_client = TCPClient()
        
        success, error_msg = tcp_client.connect(host, port, use_tls=use_tls)
        if success:
            # 跳过登录步骤（用于测试）
            # tcp_client.login(imei, imsi)
            print("[INFO] 跳过登录步骤，直接连接 TCP 媒体流")
            client["tcp_client"] = tcp_client
            client["connected"] = True
            
            await send_to_frontend(client["websocket"], {
                "type": "connection_status",
                "status": "connected",
                "protocol": "tcp_media"
            })
        else:
            await send_to_frontend(client["websocket"], {
                "type": "connection_status",
                "status": "failed",
                "error": error_msg or "连接失败",
                "protocol": "tcp_media"
            })
    except Exception as e:
        await send_to_frontend(client["websocket"], {
            "type": "error",
            "message": str(e)
        })


async def handle_connect_websocket_control(client: Dict, message: Dict):
    """处理 WebSocket 控制流连接"""
    try:
        host = message.get("host", config["server"]["address"])
        port = message.get("port", config["server"]["websocket"]["control_port"])
        imei = message.get("imei", config["test"]["imei"])
        imsi = message.get("imsi", config["test"]["imsi"])
        imei_and_imsi = f"{imei}:{imsi}"
        
        ws_client = WebSocketClient()
        
        def message_callback(msg_type, data):
            asyncio.create_task(send_to_frontend(client["websocket"], {
                "type": "websocket_message",
                "message_type": msg_type,
                "data": data
            }))
        
        ws_client.set_message_callback(message_callback)
        
        success, error_msg = await ws_client.connect_control(host, port, imei_and_imsi)
        if success:
            client["ws_client"] = ws_client
            client["connected"] = True
            
            await send_to_frontend(client["websocket"], {
                "type": "connection_status",
                "status": "connected",
                "protocol": "websocket_control"
            })
        else:
            await send_to_frontend(client["websocket"], {
                "type": "connection_status",
                "status": "failed",
                "error": error_msg or "连接失败",
                "protocol": "websocket_control"
            })
    except Exception as e:
        await send_to_frontend(client["websocket"], {
            "type": "error",
            "message": str(e)
        })


async def handle_connect_websocket_media(client: Dict, message: Dict):
    """处理 WebSocket 媒体流连接"""
    try:
        host = message.get("host", config["server"]["address"])
        port = message.get("port", config["server"]["websocket"]["media_port"])
        imei = message.get("imei", config["test"]["imei"])
        imsi = message.get("imsi", config["test"]["imsi"])
        imei_and_imsi = f"{imei}:{imsi}"
        
        init_params = message.get("init_params", {})
        
        ws_client = WebSocketClient()
        
        def media_callback(frame_type, timestamp, data):
            asyncio.create_task(send_to_frontend(client["websocket"], {
                "type": "media_frame",
                "frame_type": frame_type,
                "timestamp": timestamp,
                "data_length": len(data) if isinstance(data, bytes) else 0
            }))
        
        ws_client.set_media_callback(media_callback)
        
        success, error_msg = await ws_client.connect_media(host, port, imei_and_imsi, init_params)
        if success:
            client["ws_client"] = ws_client
            client["connected"] = True
            
            await send_to_frontend(client["websocket"], {
                "type": "connection_status",
                "status": "connected",
                "protocol": "websocket_media"
            })
        else:
            await send_to_frontend(client["websocket"], {
                "type": "connection_status",
                "status": "failed",
                "error": error_msg or "连接失败",
                "protocol": "websocket_media"
            })
    except Exception as e:
        await send_to_frontend(client["websocket"], {
            "type": "error",
            "message": str(e)
        })


async def handle_disconnect(client: Dict):
    """处理断开连接"""
    try:
        if client.get("tcp_client"):
            client["tcp_client"].disconnect()
        if client.get("ws_client"):
            await client["ws_client"].disconnect_all()
        if client.get("video_generator"):
            client["video_generator"].stop_streaming()
        
        client["connected"] = False
        
        await send_to_frontend(client["websocket"], {
            "type": "connection_status",
            "status": "disconnected"
        })
    except Exception as e:
        await send_to_frontend(client["websocket"], {
            "type": "error",
            "message": str(e)
        })


async def handle_key_event(client: Dict, message: Dict):
    """处理按键事件"""
    try:
        key_code = message.get("key_code")
        action = message.get("action", 0)  # 0=按下，1=释放
        
        if client.get("tcp_client") and client["tcp_client"].connected:
            client["tcp_client"].send_key_event(key_code, action)
        elif client.get("ws_client") and client["ws_client"].control_connected:
            # WebSocket 控制流发送按键事件
            await client["ws_client"].send_control_message("key", {
                "key_code": key_code,
                "action": action
            })
    except Exception as e:
        await send_to_frontend(client["websocket"], {
            "type": "error",
            "message": str(e)
        })


async def handle_touch_event(client: Dict, message: Dict):
    """处理触摸事件"""
    try:
        x = message.get("x")
        y = message.get("y")
        action = message.get("action", 0)  # 0=按下，1=释放，2=移动
        
        if client.get("tcp_client") and client["tcp_client"].connected:
            client["tcp_client"].send_touch_event(x, y, action)
        elif client.get("ws_client") and client["ws_client"].control_connected:
            await client["ws_client"].send_screen_click(x, y)
    except Exception as e:
        await send_to_frontend(client["websocket"], {
            "type": "error",
            "message": str(e)
        })


async def handle_start_video_stream(client: Dict, message: Dict):
    """开始视频流"""
    try:
        # 检查连接状态
        tcp_client = client.get("tcp_client")
        ws_client = client.get("ws_client")
        tcp_connected = tcp_client and tcp_client.connected
        ws_media_connected = ws_client and ws_client.media_connected
        
        if not tcp_connected and not ws_media_connected:
            error_msg = "请先连接 TCP 媒体流或 WebSocket 媒体流，然后再开始视频流"
            print(f"[ERROR] {error_msg}")
            print(f"[DEBUG] 当前连接状态: TCP客户端存在={tcp_client is not None}, "
                  f"TCP已连接={tcp_connected}, WebSocket客户端存在={ws_client is not None}, "
                  f"WebSocket媒体流已连接={ws_media_connected}")
            await send_to_frontend(client["websocket"], {
                "type": "error",
                "message": error_msg
            })
            return
        
        width = message.get("width", config["test"]["video"]["width"])
        height = message.get("height", config["test"]["video"]["height"])
        fps = message.get("fps", config["test"]["video"]["fps"])
        gop_size = message.get("gop_size", config["test"]["video"]["gop_size"])
        
        print(f"[INFO] 开始视频流: {width}x{height}@{fps}fps, 连接方式={'TCP' if tcp_connected else 'WebSocket'}")
        
        video_generator = VideoGenerator(width, height, fps, "H264", gop_size)
        
        # 获取当前事件循环（用于在线程中调用协程）
        loop = asyncio.get_event_loop()
        
        def frame_callback(frame_data, frame_type, raw_frame=None):
            # 在线程中安全地调用异步函数
            # 发送到服务器
            asyncio.run_coroutine_threadsafe(
                send_video_frame(client, frame_data, frame_type),
                loop
            )
            # 发送到前端显示（如果有原始帧）
            if raw_frame is not None:
                asyncio.run_coroutine_threadsafe(
                    send_video_frame_to_frontend(client, raw_frame),
                    loop
                )
        
        # 修改 VideoGenerator 以支持传递原始帧
        video_generator.start_streaming(frame_callback)
        client["video_generator"] = video_generator
        
        await send_to_frontend(client["websocket"], {
            "type": "video_stream_status",
            "status": "started"
        })
    except Exception as e:
        await send_to_frontend(client["websocket"], {
            "type": "error",
            "message": str(e)
        })


async def handle_stop_video_stream(client: Dict):
    """停止视频流"""
    try:
        if client.get("video_generator"):
            client["video_generator"].stop_streaming()
        
        await send_to_frontend(client["websocket"], {
            "type": "video_stream_status",
            "status": "stopped"
        })
    except Exception as e:
        await send_to_frontend(client["websocket"], {
            "type": "error",
            "message": str(e)
        })


async def send_video_frame(client: Dict, frame_data: bytes, frame_type: int):
    """发送视频帧到服务器"""
    try:
        # 检查 TCP 客户端连接状态
        tcp_client = client.get("tcp_client")
        if tcp_client and tcp_client.connected:
            print(f"[DEBUG] 发送视频帧到 TCP 服务器: 帧类型={frame_type}, 数据大小={len(frame_data)} 字节")
            client["tcp_client"].send_video_frame(frame_data, frame_type)
            return
        
        # 检查 WebSocket 客户端连接状态
        ws_client = client.get("ws_client")
        if ws_client and ws_client.media_connected:
            print(f"[DEBUG] 发送视频帧到 WebSocket 服务器: 帧类型={frame_type}, 数据大小={len(frame_data)} 字节")
            await client["ws_client"].send_media_frame(frame_data, frame_type)
            return
        
        # 详细的状态信息
        tcp_exists = tcp_client is not None
        tcp_connected = tcp_client.connected if tcp_client else False
        ws_exists = ws_client is not None
        ws_media_connected = ws_client.media_connected if ws_client else False
        
        print(f"[DEBUG] 警告: 未连接到服务器，视频帧仅在前端显示，未发送到服务器")
        print(f"[DEBUG] 连接状态详情: TCP客户端存在={tcp_exists}, TCP已连接={tcp_connected}, "
              f"WebSocket客户端存在={ws_exists}, WebSocket媒体流已连接={ws_media_connected}")
    except Exception as e:
        print(f"[DEBUG] 发送视频帧失败: {e}")
        import traceback
        traceback.print_exc()


async def send_video_frame_to_frontend(client: Dict, raw_frame):
    """发送视频帧到前端显示"""
    try:
        import cv2
        import base64
        
        # 将 BGR 转换为 RGB（OpenCV 使用 BGR，浏览器需要 RGB）
        rgb_frame = cv2.cvtColor(raw_frame, cv2.COLOR_BGR2RGB)
        
        # 编码为 JPEG
        encode_param = [int(cv2.IMWRITE_JPEG_QUALITY), 85]
        result, encoded = cv2.imencode('.jpg', rgb_frame, encode_param)
        
        if result:
            # 转换为 base64
            frame_base64 = base64.b64encode(encoded.tobytes()).decode('utf-8')
            
            await send_to_frontend(client["websocket"], {
                "type": "video_frame",
                "frame_data": frame_base64,
                "format": "jpeg"
            })
    except Exception as e:
        print(f"发送视频帧到前端失败: {e}")


async def handle_rest_api(client: Dict, message: Dict):
    """处理 REST API 请求"""
    try:
        api_type = message.get("api_type")
        params = message.get("params", {})
        
        if not client.get("rest_client"):
            base_url = f"http://{config['server']['address']}:{config['server']['http_port']}"
            client["rest_client"] = RESTClient(base_url)
            print(f"[INFO] 创建 REST 客户端: {base_url}")
        
        rest_client = client["rest_client"]
        result = None
        
        print(f"[INFO] 调用 REST API: {api_type}")
        if params:
            print(f"[INFO] 请求参数: {params}")
        
        if api_type == "pre_open_browser":
            result = await rest_client.pre_open_browser(params)
        elif api_type == "delete_user_data":
            result = await rest_client.delete_user_data(params.get("imei"), params.get("imsi"))
        elif api_type == "load_extension":
            result = await rest_client.load_extension(params)
        elif api_type == "get_plugin_info":
            result = await rest_client.get_plugin_info()
        else:
            print(f"[WARN] 未知的 API 类型: {api_type}")
            await send_to_frontend(client["websocket"], {
                "type": "error",
                "message": f"未知的 API 类型: {api_type}"
            })
            return
        
        print(f"[INFO] REST API 响应 ({api_type}): {result}")
        
        await send_to_frontend(client["websocket"], {
            "type": "rest_api_response",
            "api_type": api_type,
            "result": result
        })
    except Exception as e:
        error_msg = str(e)
        print(f"[ERROR] REST API 调用失败 ({api_type}): {error_msg}")
        import traceback
        traceback.print_exc()
        await send_to_frontend(client["websocket"], {
            "type": "error",
            "message": error_msg
        })


async def send_to_frontend(websocket: WebSocket, message: Dict):
    """发送消息到前端"""
    try:
        await websocket.send_json(message)
    except:
        pass
