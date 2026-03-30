"""
GIDS Mock Server - 模拟 GIDS 认证服务

提供以下接口：
- GET /user-bind/v1/{sessionID} - 获取 UserBind 信息
- POST /user-bind/v1/update - 更新 UserBind 信息
- PUT /user-bind/v1/{sessionID} - 标记 UserBind 过期
- POST /stats/v1/traffic/media - 接收媒体流量统计
- POST /stats/v1/traffic/control - 接收控制流量统计
- POST /stats/v1/session - 接收会话统计

黑盒防护网（对齐 case.md 插件侧契约，供 test_http_blackbox.py）：
- POST /auth/v1/importIMEIList - multipart: file + operation
- GET /auth/v1/exportIMEIList
- POST /auth/v1/authIMEI - 限流 429
- GET /stats/v1/exportStaticData/{month}
- POST /config 支持 rate_limit_*、reset_rate_limits、clear_imei_whitelist
"""

import asyncio
import re
import time
from typing import Dict, List, Optional

import uvicorn
import logging
from fastapi import FastAPI, File, Form, HTTPException, Request, UploadFile
from fastapi.responses import JSONResponse, PlainTextResponse, HTMLResponse
from pydantic import BaseModel

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = FastAPI(title="GIDS Mock Server")

# 存储 UserBind 数据
user_binds: Dict[str, dict] = {}

# 配置
class GIDSConfig:
    scenario = "success"  # success, invalid_token, timeout, unavailable
    response_delay = 0  # 响应延迟（秒）
    # 黑盒防护网：限流（每分钟窗口）。生产 case 为 200；本地 pytest 默认用小窗口便于快速断言 429
    rate_limit_auth_imei_max = 200
    rate_limit_upload_max = 200

config = GIDSConfig()

# ---------- 黑盒：IMEI 白名单（内存，供 export/import 一致性验证）----------
imei_whitelist_rows: List[str] = []

# ---------- 黑盒：限流计数（每分钟重置）----------
def _rate_state():
    return {"count": 0, "window_start": 0.0}


_auth_imei_rl = _rate_state()
_upload_rl = _rate_state()

MAX_IMPORT_BYTES = int(3.5 * 1024 * 1024)
IMEI_RE = re.compile(r"^\d{14,15}$")


def _rate_allow(state: dict, max_req: int) -> bool:
    now = time.time()
    if now - state["window_start"] >= 60:
        state["count"] = 0
        state["window_start"] = now
    state["count"] += 1
    return state["count"] <= max_req


def _validate_imei_csv(content: str) -> Optional[str]:
    """返回 None 表示合法；否则返回错误信息。"""
    lines = [ln.strip() for ln in content.strip().splitlines() if ln.strip()]
    if not lines:
        return "empty csv"
    for line in lines:
        parts = line.split(",")
        if len(parts) != 2:
            return "bad csv row"
        imei_field, mode = parts[0].strip(), parts[1].strip()
        if mode == "single":
            if not IMEI_RE.match(imei_field):
                return "invalid single imei"
        elif mode == "range":
            if "-" not in imei_field:
                return "invalid range"
            a, b = imei_field.split("-", 1)
            a, b = a.strip(), b.strip()
            if not (IMEI_RE.match(a) and IMEI_RE.match(b)):
                return "invalid range imei"
        else:
            return "invalid mode"
    return None


def _apply_import_rows(content: str, operation: str) -> None:
    global imei_whitelist_rows
    lines = [ln.strip() for ln in content.strip().splitlines() if ln.strip()]
    if operation == "firstImport":
        imei_whitelist_rows = lines.copy()
    else:
        for ln in lines:
            if ln not in imei_whitelist_rows:
                imei_whitelist_rows.append(ln)

class UserBindUpdate(BaseModel):
    sessionId: str
    token: Optional[str] = None
    browserInstance: Optional[str] = None
    controlEndpoint: Optional[str] = None
    mediaEndpoint: Optional[str] = None
    controlTlsEndpoint: Optional[str] = None
    mediaTlsEndpoint: Optional[str] = None
    innerMediaEndpoint: Optional[str] = None
    innerBrowserEndpoint: Optional[str] = None

    class Config:
        extra = "allow"

@app.get("/user-bind/v1/{session_id}")
async def get_user_bind(session_id: str, token: Optional[str] = None):
    """获取 UserBind 信息"""
    logger.info(f"GET /user-bind/v1/{session_id}, token={token}")

    # 模拟响应延迟
    if config.response_delay > 0:
        await asyncio.sleep(config.response_delay)

    # 模拟不同场景
    if config.scenario == "invalid_token":
        logger.warning(f"Invalid token scenario: {token}")
        raise HTTPException(status_code=401, detail="Invalid token")

    if config.scenario == "unavailable":
        logger.error("Service unavailable scenario")
        raise HTTPException(status_code=503, detail="Service unavailable")

    # 检查是否存在
    if session_id in user_binds:
        user_bind = user_binds[session_id]
        user_bind["heartbeats"] = int(time.time() * 1000)
        logger.info(f"Found existing UserBind: {session_id}")
        return user_bind

    # 创建新的 UserBind
    # 注意：BrowserGateway 调用此接口时不会传递 token 参数
    # 所以我们使用一个默认的 token，测试客户端也应该使用相同的 token
    default_token = "mock-token-12345"

    user_bind = {
        "sessionId": session_id,
        "token": token if token else default_token,  # 使用传入的 token 或默认 token
        "browserInstance": "localhost:8090",
        "controlEndpoint": "http://127.0.0.1:30001",
        "mediaEndpoint": "ws://127.0.0.1:30002",
        "controlTlsEndpoint": "https://127.0.0.1:30012",
        "mediaTlsEndpoint": "wss://127.0.0.1:30013",
        "innerMediaEndpoint": "ws://127.0.0.1:30002",
        "innerBrowserEndpoint": "http://127.0.0.1:8090",
        "heartbeats": int(time.time() * 1000)
    }

    user_binds[session_id] = user_bind
    logger.info(f"Created new UserBind: {session_id}, token={user_bind['token']}")
    return user_bind

@app.post("/user-bind/v1/update")
async def update_user_bind(request: Request):
    """更新 UserBind 信息（兼容非 application/json Content-Type）"""
    try:
        body = await request.body()
        import json
        data = json.loads(body)
        update = UserBindUpdate(**data)
    except Exception as e:
        logger.warning(f"POST /user-bind/v1/update parse error: {e}, body={body}")
        raise HTTPException(status_code=422, detail=str(e))

    logger.info(f"POST /user-bind/v1/update, sessionId={update.sessionId}")

    if config.response_delay > 0:
        await asyncio.sleep(config.response_delay)

    # 保留已有记录中的 token（BGW updateUserBind 不携带 token）
    existing = user_binds.get(update.sessionId, {})
    user_bind = {
        "sessionId": update.sessionId,
        "token": update.token if update.token is not None else existing.get("token"),
        "browserInstance": update.browserInstance,
        "controlEndpoint": update.controlEndpoint,
        "mediaEndpoint": update.mediaEndpoint,
        "controlTlsEndpoint": update.controlTlsEndpoint,
        "mediaTlsEndpoint": update.mediaTlsEndpoint,
        "innerMediaEndpoint": update.innerMediaEndpoint,
        "innerBrowserEndpoint": update.innerBrowserEndpoint,
        "heartbeats": int(time.time() * 1000)
    }

    user_binds[update.sessionId] = user_bind
    logger.info(f"Updated UserBind: {update.sessionId}")
    return {"status": "success"}

@app.put("/user-bind/v1/{session_id}")
async def expire_user_bind(session_id: str):
    """标记 UserBind 过期"""
    logger.info(f"PUT /user-bind/v1/{session_id} - marking as expired")

    if config.response_delay > 0:
        await asyncio.sleep(config.response_delay)

    if session_id in user_binds:
        del user_binds[session_id]
        logger.info(f"Expired UserBind: {session_id}")

    return {"status": "success"}

@app.post("/stats/v1/traffic/media")
async def receive_media_stats(request: Request):
    """接收媒体流量统计"""
    data = await request.json()
    logger.debug(f"Received media stats: {data}")
    return {"status": "success"}

@app.post("/stats/v1/traffic/control")
async def receive_control_stats(request: Request):
    """接收控制流量统计"""
    data = await request.json()
    logger.debug(f"Received control stats: {data}")
    return {"status": "success"}

@app.post("/stats/v1/session")
async def receive_session_stats(request: Request):
    """接收会话统计"""
    data = await request.json()
    logger.debug(f"Received session stats: {data}")
    return {"status": "success"}


# ---------- 黑盒防护网：鉴权 / 统计导出（对齐 case.md，插件侧契约模拟）----------

@app.post("/auth/v1/importIMEIList")
async def import_imei_list(
    operation: Optional[str] = Form(None),
    file: Optional[UploadFile] = File(None),
):
    """
    导入 IMEI 白名单（multipart：file + operation）。
    负面用例与 case.md 一致：缺参、非法 operation、空文件、超大文件、非法 CSV 行。
    """
    if operation is None or str(operation).strip() == "":
        return JSONResponse(
            status_code=400, content={"code": 1, "msg": "参数错误: operation 必填"}
        )
    op = str(operation).strip()
    if op not in ("firstImport", "update"):
        return JSONResponse(
            status_code=400, content={"code": 1, "msg": "参数错误: operation 非法"}
        )
    if file is None:
        return JSONResponse(
            status_code=400, content={"code": 1, "msg": "参数错误: file 必填"}
        )
    raw = await file.read()
    if len(raw) == 0:
        return JSONResponse(
            status_code=400, content={"code": 1, "msg": "参数错误: 空文件"}
        )
    if len(raw) > MAX_IMPORT_BYTES:
        return JSONResponse(
            status_code=400, content={"code": 1, "msg": "参数错误: 文件超过 3.5M"}
        )
    try:
        text = raw.decode("utf-8")
    except UnicodeDecodeError:
        return JSONResponse(
            status_code=400, content={"code": 1, "msg": "参数错误: 文件编码非法"}
        )
    err = _validate_imei_csv(text)
    if err:
        return JSONResponse(
            status_code=400, content={"code": 1, "msg": f"参数错误: {err}"}
        )
    _apply_import_rows(text, op)
    logger.info(f"importIMEIList ok operation={op} rows={len(imei_whitelist_rows)}")
    return {"code": 0, "msg": "success", "data": {}}


@app.get("/auth/v1/exportIMEIList")
async def export_imei_list():
    """导出全量白名单 CSV；无数据时返回空 body（200）。"""
    body = "\n".join(imei_whitelist_rows) + ("\n" if imei_whitelist_rows else "")
    return PlainTextResponse(body, media_type="text/csv; charset=utf-8")


@app.post("/auth/v1/authIMEI")
async def auth_imei(request: Request):
    """授权校验；超限返回 429（限流窗口内）。"""
    if not _rate_allow(_auth_imei_rl, config.rate_limit_auth_imei_max):
        return JSONResponse(
            status_code=429, content={"code": 429, "msg": "too many requests"}
        )
    try:
        _ = await request.body()
    except Exception:
        pass
    return {"code": 0, "msg": "success", "data": {"authorized": True}}


@app.get("/stats/v1/exportStaticData/{month}")
async def export_static_data(month: str):
    """导出静态统计 CSV；月份格式 YYYY-MM，非法则 400。"""
    if not re.match(r"^\d{4}-(0[1-9]|1[0-2])$", month):
        return JSONResponse(
            status_code=400, content={"code": 1, "msg": "非法月份"}
        )
    # 最小列集，便于黑盒断言「有 CSV 结构」
    csv = (
        "record_type,imei_imsi,app,login_time,tcp_open,tcp_close,bytes\n"
        "session,123456789012345_987654321098765,5,2026-01-01T00:00:00,,,\n"
        "control,123456789012345_987654321098765,5,,2026-01-01T00:00:01,2026-01-01T00:10:00,1024\n"
        "media,123456789012345_987654321098765,5,,2026-01-01T00:00:02,2026-01-01T00:10:00,2048\n"
    )
    return PlainTextResponse(csv, media_type="text/csv; charset=utf-8")


@app.post("/app-api/devicetcp/app/login/v1/gridLoginAuth")
async def grid_login_auth(request: Request):
    """登录第一步"""
    data = await request.json()
    logger.info(f"POST gridLoginAuth: imei={data.get('imei')}, imsi={data.get('imsi')}")
    return {
        "code": 200,
        "msg": "success",
        "data": {
            "token": "mock-token-12345",
            "expiresTime": 1774321223548944,
            "timeAxis": 1774317623,
            "nodeGateWayUrl": "127.0.0.1:30001",
            "nodeIntranetWayUrl": "127.0.0.1:30001"
        }
    }

@app.post("/app-api/devicetcp/app/login/v1/gridLoginAuthOpenBrowser")
async def grid_login_auth_open_browser(request: Request):
    """登录第二步"""
    data = await request.json()
    logger.info(f"POST gridLoginAuthOpenBrowser: imei={data.get('imei')}")
    return {
        "code": 200,
        "msg": "success",
        "data": {
            "token": "mock-token-12345",
            "expiresTime": 1774321223548944,
            "timeAxis": 1774317623,
            "nodeGateWayUrl": "127.0.0.1:30001",
            "nodeIntranetWayUrl": "127.0.0.1:30001"
        }
    }

@app.post("/app-api/devicetcp/app/login/v1/deviceLoginAuth")
async def device_login_auth(request: Request):
    """登录第三步，返回 token 和 BGW TCP 地址，同时预注册 session"""
    data = await request.json()
    imei = data.get('imei', '')
    imsi = data.get('imsi', '')
    session_id = f"{imei}_{imsi}"
    token = "mock-token-12345"
    logger.info(f"POST deviceLoginAuth: imei={imei}, imsi={imsi}, session_id={session_id}")
    # 预注册 session，供 BGW 验证 token
    user_binds[session_id] = {
        "sessionId": session_id,
        "token": token,
        "browserInstance": "",
        "controlEndpoint": "127.0.0.1:30001",
        "mediaEndpoint": "127.0.0.1:30002",
        "controlTlsEndpoint": "https://127.0.0.1:30003",
        "mediaTlsEndpoint": "https://127.0.0.1:30004",
        "innerMediaEndpoint": "127.0.0.1:30002",
        "innerBrowserEndpoint": "127.0.0.1:8090",
        "heartbeats": int(time.time() * 1000)
    }
    return {
        "code": 200,
        "msg": "success",
        "data": {
            "token": token,
            "expiresTime": 1774321223548944,
            "timeAxis": 1774317623,
            "tcpAddr": "127.0.0.1:30001",
            "videoMode": 1,
            "shortAddr": "127.0.0.1:30001",
            "nodeGateWayUrl": "127.0.0.1:30001",
            "nodeIntranetWayUrl": "127.0.0.1:30001"
        }
    }

@app.post("/server/event/v1/uploadEvent")
async def upload_event(request: Request):
    """接收事件上报"""
    data = await request.json()
    logger.info(f"POST /server/event/v1/uploadEvent: {data}")
    return {"code": 200, "msg": "success", "data": {}}

@app.post("/app-api/center/public/client/sendClientEvent")
async def send_client_event(request: Request):
    """接收客户端事件埋点（sendError / sendUseTime）"""
    data = await request.json()
    logger.info(f"POST /app-api/center/public/client/sendClientEvent: type={data.get('type')}")
    return {"code": 0, "msg": "success", "data": {}}

@app.post("/app-api/control/file/upload")
async def upload_control_file(request: Request, fileName: str = ""):
    """接收前端文件上传，返回文件路径供 upload_file TLV 使用；限流与 authIMEI 同策略。"""
    if not _rate_allow(_upload_rl, config.rate_limit_upload_max):
        return JSONResponse(
            status_code=429, content={"code": 429, "msg": "too many requests"}
        )
    body = await request.body()
    logger.info(f"POST /app-api/control/file/upload: fileName={fileName}, size={len(body)}")
    saved_path = f"/tmp/{fileName}" if fileName else "/tmp/upload"
    return {"code": 0, "msg": "success", "data": saved_path}

@app.get("/health")
async def health_check():
    """健康检查"""
    return {"status": "healthy", "active_sessions": len(user_binds)}

# 插件包本地路径（本地验证用）
PLUGIN_ZIP_PATH = "D:/workspace/plugin-src/muen-plugin-v0.0.22.zip"
PLUGIN_BUCKET = "plugin"
PLUGIN_PACKAGE_NAME = "muen-plugin-v0.0.22.zip"

@app.api_route("/plugin/v1/current", methods=["GET", "POST"])
async def get_current_plugin():
    """获取当前激活插件信息，供 BrowserGateway 下载并加载 SDK"""
    logger.info("GET/POST /plugin/v1/current - returning plugin info")
    return [
        {
            "name": "muen",
            "version": "0.0.22",
            "type": "default",
            "status": "active",
            "bucket": PLUGIN_BUCKET,
            "packageName": PLUGIN_PACKAGE_NAME
        }
    ]

@app.get("/file/v1/userdata/{user_id}/{filename:path}/exist")
async def check_file_exist(user_id: str, filename: str):
    """检查用户数据文件是否存在（本地调试始终返回不存在，用 404 表示）"""
    # 处理 Windows 路径中的反斜杠
    filename = filename.replace('\\', '/')
    logger.info(f"GET /file/v1/userdata/{user_id}/{filename}/exist - returning 404 not found")
    raise HTTPException(status_code=404, detail="userdata not found")

@app.get("/file/v1/userdata/{user_id}/{filename:path}")
async def get_user_file(user_id: str, filename: str):
    """获取用户数据文件（本地调试不存在远端文件，返回 404）"""
    # 处理 Windows 路径中的反斜杠
    filename = filename.replace('\\', '/')
    logger.info(f"GET /file/v1/userdata/{user_id}/{filename} - returning 404 not found")
    raise HTTPException(status_code=404, detail=f"File not found: {user_id}/{filename}")

@app.post("/file/v1/userdata/{user_id}/{filename:path}")
async def upload_user_file(user_id: str, filename: str, request: Request):
    """上传用户数据文件（接受但不保存）"""
    # 处理 Windows 路径中的反斜杠
    filename = filename.replace('\\', '/')
    logger.info(f"POST /file/v1/userdata/{user_id}/{filename} - accepting upload")
    return {"status": "success"}

@app.get("/config/v1")
async def get_muen_config():
    """返回 SDK 初始化所需的 MuenConfig（chromeConfigList/routeAppConfigList/urlConfigList）"""
    logger.info("GET /config/v1 - returning MuenConfig")
    return {
        "chromeConfigList": [
            {
                "manufacturer": "default",
                "model": "default",
                "country": "en",
                "appFrameRate": 10,
                "videoFrameRate": 10,
                "appBitRate": 1000,
                "videoBitRate": 1000,
                "sampleRate": 48000,
                "channels": 1,
                "machineType": 2,
                "ffCode": "240x320",
                "resolution": "240x320",
                "recordMode": 1,
                "controlExtentionId": "jjndjgheafjngoipoacpjgeicjeomjli",
                "controlExtentionPath": "D:/workspace/extension/keys",
                "frameRate": 10,
                "bitRite": 1000,
                "chromeWidth": 240,
                "chromeHeight": 320
            },
            {
                "manufacturer": "uct02",
                "model": "xma240",
                "country": "en",
                "appFrameRate": 10,
                "videoFrameRate": 10,
                "appBitRate": 1000,
                "videoBitRate": 1000,
                "sampleRate": 48000,
                "channels": 1,
                "machineType": 2,
                "ffCode": "240x320",
                "resolution": "240x320",
                "recordMode": 0,
                "controlExtentionId": "jjndjgheafjngoipoacpjgeicjeomjli",
                "controlExtentionPath": "D:/workspace/extension/keys",
                "frameRate": 10,
                "bitRite": 1000,
                "chromeWidth": 240,
                "chromeHeight": 320
            }
        ],
        "routeAppConfigList": [],
        "urlConfigList": [
            {"nodeIdent": "", "appType": 1, "url": "https://m.youtube.com", "appID": "1", "name": "Youtube", "isVideoType": True, "isWebType": False, "isShortType": True},
            {"nodeIdent": "", "appType": 2, "url": "https://www.tiktok.com/foryou?lang=en_US", "appID": "2", "name": "TikTok", "isVideoType": True, "isWebType": False, "isShortType": True},
            {"nodeIdent": "", "appType": 3, "url": "https://www.facebook.com", "appID": "3", "name": "FaceBook", "isVideoType": False, "isWebType": True, "isShortType": False},
            {"nodeIdent": "", "appType": 5, "url": "https://www.bbc.com", "appID": "5", "name": "BBC", "isVideoType": False, "isWebType": True, "isShortType": False},
            {"nodeIdent": "", "appType": 6, "url": "about:blank", "appID": "6", "name": "Upload", "isVideoType": False, "isWebType": True, "isShortType": False},
            {"nodeIdent": "", "appType": 8, "url": "https://www.instagram.com", "appID": "8", "name": "Ins", "isVideoType": False, "isWebType": True, "isShortType": False},
            {"nodeIdent": "", "appType": 10, "url": "https://www.snapchat.com", "appID": "10", "name": "SNAPCHAT", "isVideoType": False, "isWebType": True, "isShortType": False},
            {"nodeIdent": "", "appType": 12, "url": "https://www.google.com", "appID": "12", "name": "Google", "isVideoType": False, "isWebType": True, "isShortType": False},
            {"nodeIdent": "", "appType": 18, "url": "https://www.cnn.com", "appID": "18", "name": "CNN", "isVideoType": False, "isWebType": True, "isShortType": False},
            {"nodeIdent": "", "appType": 920425, "url": "https://web.telegram.org", "appID": "920425", "name": "TELE", "isVideoType": False, "isWebType": True, "isShortType": False}
        ]
    }

@app.get("/file/v1/{bucket}/{filename:path}")
async def download_plugin_file(bucket: str, filename: str):
    """下载插件包文件，供 BrowserGateway 加载 SDK 使用"""
    import os
    from fastapi.responses import FileResponse
    filename = filename.replace('\\', '/')
    logger.info(f"GET /file/v1/{bucket}/{filename} - serving plugin file")
    if bucket == PLUGIN_BUCKET and filename == PLUGIN_PACKAGE_NAME:
        if os.path.isfile(PLUGIN_ZIP_PATH):
            return FileResponse(PLUGIN_ZIP_PATH, media_type="application/octet-stream", filename=filename)
        logger.error(f"Plugin zip not found: {PLUGIN_ZIP_PATH}")
        raise HTTPException(status_code=404, detail=f"Plugin zip not found: {PLUGIN_ZIP_PATH}")
    raise HTTPException(status_code=404, detail=f"File not found: {bucket}/{filename}")

@app.post("/config")
async def update_config(request: Request):
    """更新配置（用于测试场景切换）"""
    data = await request.json()
    if "scenario" in data:
        config.scenario = data["scenario"]
        logger.info(f"Updated scenario to: {config.scenario}")
    if "response_delay" in data:
        config.response_delay = data["response_delay"]
        logger.info(f"Updated response_delay to: {config.response_delay}")
    if "rate_limit_auth_imei_max" in data:
        config.rate_limit_auth_imei_max = int(data["rate_limit_auth_imei_max"])
    if "rate_limit_upload_max" in data:
        config.rate_limit_upload_max = int(data["rate_limit_upload_max"])
    if data.get("reset_rate_limits"):
        _auth_imei_rl["count"] = 0
        _auth_imei_rl["window_start"] = 0.0
        _upload_rl["count"] = 0
        _upload_rl["window_start"] = 0.0
        logger.info("Rate limit counters reset")
    if data.get("clear_imei_whitelist"):
        imei_whitelist_rows.clear()
        logger.info("IMEI whitelist cleared")
    return {
        "status": "success",
        "config": {
            "scenario": config.scenario,
            "response_delay": config.response_delay,
            "rate_limit_auth_imei_max": config.rate_limit_auth_imei_max,
            "rate_limit_upload_max": config.rate_limit_upload_max,
        },
    }

def start_server(host: str = "127.0.0.1", port: int = 9090):
    """启动 GIDS Mock 服务器"""
    logger.info(f"Starting GIDS Mock Server on {host}:{port}")
    # 增加并发连接数和超时时间
    uvicorn.run(
        app,
        host=host,
        port=port,
        log_level="info",
        timeout_keep_alive=75,  # 保持连接时间
        limit_concurrency=1000,  # 最大并发连接数
        limit_max_requests=10000  # 最大请求数
    )

if __name__ == "__main__":
    start_server()
