"""
黑盒防护网：HTTP 契约（PLAN-HTTP-01～04）

对 GIDS Mock 上实现的 /auth/*、/stats/v1/exportStaticData、限流 与 case.md 插件侧契约对齐。
基址默认 http://127.0.0.1:9090（与 session fixture gids_mock 一致）。

环境变量：
  BLACKBOX_HTTP_BASE  覆盖基址（对真实环境联调时可指向网关）
"""

from __future__ import annotations

import os

import httpx
import pytest

BASE = os.environ.get("BLACKBOX_HTTP_BASE", "http://127.0.0.1:9090")


@pytest.fixture
def bb_client(gids_mock):
    with httpx.Client(base_url=BASE, timeout=30.0) as client:
        # 小限流阈值便于快速打出 429；与 case 生产 200 可在联调时改回
        client.post(
            "/config",
            json={
                "clear_imei_whitelist": True,
                "reset_rate_limits": True,
                "rate_limit_auth_imei_max": 5,
                "rate_limit_upload_max": 5,
            },
        )
        yield client


# --- PLAN-HTTP-01 importIMEIList ---


def test_import_imei_missing_operation(bb_client):
    files = {"file": ("a.csv", b"123456789012345,single\n", "text/csv")}
    r = bb_client.post("/auth/v1/importIMEIList", files=files, data={})
    assert r.status_code == 400
    assert r.json().get("code") != 0


def test_import_imei_invalid_operation(bb_client):
    files = {"file": ("a.csv", b"123456789012345,single\n", "text/csv")}
    r = bb_client.post(
        "/auth/v1/importIMEIList", files=files, data={"operation": "badOp"}
    )
    assert r.status_code == 400


def test_import_imei_empty_file(bb_client):
    files = {"file": ("empty.csv", b"", "text/csv")}
    r = bb_client.post(
        "/auth/v1/importIMEIList", files=files, data={"operation": "firstImport"}
    )
    assert r.status_code == 400


def test_import_imei_file_too_large(bb_client):
    big = b"0" * (int(3.5 * 1024 * 1024) + 1)
    files = {"file": ("big.csv", big, "text/csv")}
    r = bb_client.post(
        "/auth/v1/importIMEIList", files=files, data={"operation": "firstImport"}
    )
    assert r.status_code == 400


def test_import_imei_invalid_row_wrong_single(bb_client):
    """case: (11111-22222, single) 应失败"""
    files = {"file": ("bad.csv", b"11111-22222,single\n", "text/csv")}
    r = bb_client.post(
        "/auth/v1/importIMEIList", files=files, data={"operation": "firstImport"}
    )
    assert r.status_code == 400


def test_import_imei_invalid_row_bad_range(bb_client):
    """case: (11111, range) 应失败"""
    files = {"file": ("bad.csv", b"11111,range\n", "text/csv")}
    r = bb_client.post(
        "/auth/v1/importIMEIList", files=files, data={"operation": "firstImport"}
    )
    assert r.status_code == 400


def test_import_imei_success_then_export_matches(bb_client):
    body = (
        b"123456789012345,single\n"
        b"123456789012346-123456789012350,range\n"
    )
    files = {"file": ("ok.csv", body, "text/csv")}
    r = bb_client.post(
        "/auth/v1/importIMEIList", files=files, data={"operation": "firstImport"}
    )
    assert r.status_code == 200
    assert r.json().get("code") == 0
    ex = bb_client.get("/auth/v1/exportIMEIList")
    assert ex.status_code == 200
    assert "123456789012345,single" in ex.text
    assert "123456789012346-123456789012350,range" in ex.text


def test_import_imei_missing_file_part(bb_client):
    r = bb_client.post(
        "/auth/v1/importIMEIList", data={"operation": "firstImport"}
    )
    assert r.status_code == 400


# --- PLAN-HTTP-02 exportIMEIList ---


def test_export_imei_empty_whitelist(bb_client):
    r = bb_client.get("/auth/v1/exportIMEIList")
    assert r.status_code == 200
    assert r.text.strip() == ""


# --- PLAN-HTTP-03 exportStaticData ---


def test_export_static_invalid_month(bb_client):
    r = bb_client.get("/stats/v1/exportStaticData/13")
    assert r.status_code == 400
    r2 = bb_client.get("/stats/v1/exportStaticData/2026-13")
    assert r2.status_code == 400


def test_export_static_valid_month(bb_client):
    r = bb_client.get("/stats/v1/exportStaticData/2026-03")
    assert r.status_code == 200
    assert "session" in r.text.lower() or "record_type" in r.text.lower()


# --- PLAN-HTTP-04 限流 429 ---


def test_auth_imei_rate_limit_429(bb_client):
    last = None
    for _ in range(6):
        last = bb_client.post("/auth/v1/authIMEI", json={"imei": "123456789012345"})
    assert last.status_code == 429


def test_file_upload_rate_limit_429(bb_client):
    bb_client.post("/config", json={"reset_rate_limits": True})
    last = None
    for _ in range(6):
        last = bb_client.post(
            "/app-api/control/file/upload?fileName=t.txt",
            content=b"x",
        )
    assert last.status_code == 429
