"""
Optional WebSocket connect + login smoke (short timeout), no gids_mock fixture.
Requires mobile + GIDS already up if login path hits GIDS.
"""

from __future__ import annotations

import asyncio
import json
import os
import sys
from typing import Any, Dict

_ROOT = os.path.join(os.path.dirname(__file__), "..", "..")
_SRC = os.path.join(_ROOT, "src")
if _SRC not in sys.path:
    sys.path.insert(0, _SRC)

import yaml  # noqa: E402

_CONFIG_PATH = os.path.join(_ROOT, "config", "config.yaml")


def _load_test_ids() -> tuple[str, str]:
    with open(_CONFIG_PATH, "r", encoding="utf-8") as f:
        cfg = yaml.safe_load(f)
    t = cfg.get("test", {}) or {}
    imei = t.get("imei", "123456789012345")
    imsi = t.get("imsi", "987654321098765")
    return str(imei), str(imsi)


async def run_ws_smoke(login_wait_s: float = 5.0) -> Dict[str, Any]:
    """
    Connect to mobile WS, send login, wait for error text or timeout (success).

    Returns:
      ok, stage (connect|login|recv), message (human-readable)
    """
    from client.mobile_client import MobileTestClient

    imei, imsi = _load_test_ids()
    client = MobileTestClient()
    try:
        try:
            await asyncio.wait_for(client.connect(imei, imsi), timeout=10.0)
        except Exception as e:
            return {"ok": False, "stage": "connect", "message": str(e)}

        try:
            await client.login()
        except Exception as e:
            return {"ok": False, "stage": "login", "message": str(e)}

        try:
            msg = await asyncio.wait_for(client.recv_text(timeout=login_wait_s), timeout=login_wait_s + 1)
            code = msg.get("code", 0) if isinstance(msg, dict) else None
            if code not in (None, 0):
                return {"ok": False, "stage": "recv", "message": json.dumps(msg, ensure_ascii=False)}
            return {"ok": True, "stage": "recv", "message": "login ack path clear"}
        except TimeoutError:
            return {"ok": True, "stage": "login", "message": "no error within timeout (treated as success)"}
        except Exception as e:
            return {"ok": False, "stage": "recv", "message": str(e)}
    finally:
        try:
            await client.disconnect()
        except Exception:
            pass
