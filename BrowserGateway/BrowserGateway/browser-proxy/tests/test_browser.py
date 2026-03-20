"""浏览器管理 API 测试用例
对应文档: doc/测试用例文档.md - 第1章
"""
import pytest
from fastapi.testclient import TestClient
from unittest.mock import AsyncMock, MagicMock, patch

from browser_proxy.main import app
from browser_proxy.api.common import BrowserType, BrowserWrapper, browser_list


# ---------------------------------------------------------------------------
# helpers
# ---------------------------------------------------------------------------

def _make_browser_wrapper(browser_id: str = "test-browser",
                          browser_type: BrowserType = BrowserType.KEYS) -> BrowserWrapper:
    mock_browser = AsyncMock()
    mock_browser.is_connected.return_value = True
    mock_playwright = AsyncMock()
    return BrowserWrapper(
        browser_id=browser_id,
        browser=mock_browser,
        browser_type=browser_type,
        userdata="",
        playwright=mock_playwright,
    )


# ---------------------------------------------------------------------------
# 1.1 创建浏览器实例 POST /api/browsers
# ---------------------------------------------------------------------------

class TestCreateBrowser:
    """TC-BROWSER-001 ~ TC-BROWSER-008"""

    def test_TC_BROWSER_001_create_basic(self, client, mock_playwright):
        """TC-BROWSER-001: 创建基本浏览器实例 - 正常场景"""
        payload = {
            "executable_path": "/usr/bin/google-chrome-stable",
            "browser_type": "KEYS",
            "headless": True,
            "language": "en-US",
        }
        resp = client.post("/api/browsers", json=payload)
        assert resp.status_code == 200
        data = resp.json()
        assert "id" in data
        assert data["browser_type"] == "KEYS"
        assert len(browser_list) == 1

    def test_TC_BROWSER_002_create_with_specified_id(self, client, mock_playwright):
        """TC-BROWSER-002: 创建指定 ID 的浏览器实例"""
        payload = {
            "executable_path": "/usr/bin/google-chrome-stable",
            "browser_id": "test-browser-001",
            "browser_type": "KEYS",
            "headless": True,
        }
        resp = client.post("/api/browsers", json=payload)
        assert resp.status_code == 200
        data = resp.json()
        assert data["id"] == "test-browser-001"

        # 通过 ID 能成功获取浏览器信息
        get_resp = client.get("/api/browsers/test-browser-001")
        assert get_resp.status_code == 200
        assert get_resp.json()["id"] == "test-browser-001"

    def test_TC_BROWSER_003_create_touch_mode(self, client, mock_playwright):
        """TC-BROWSER-003: 创建 TOUCH 模式浏览器实例"""
        payload = {
            "executable_path": "/usr/bin/google-chrome-stable",
            "browser_type": "TOUCH",
            "headless": True,
        }
        resp = client.post("/api/browsers", json=payload)
        assert resp.status_code == 200
        assert resp.json()["browser_type"] == "TOUCH"

    def test_TC_BROWSER_004_create_with_base_data(self, client, mock_playwright):
        """TC-BROWSER-004: 创建使用自定义用户数据目录的浏览器实例"""
        payload = {
            "executable_path": "/usr/bin/google-chrome-stable",
            "browser_type": "KEYS",
            "base_data": "/tmp/test_user_data",
        }
        resp = client.post("/api/browsers", json=payload)
        assert resp.status_code == 200
        assert "id" in resp.json()

    def test_TC_BROWSER_005_create_with_extensions(self, client, mock_playwright):
        """TC-BROWSER-005: 创建带扩展的浏览器实例"""
        payload = {
            "executable_path": "/usr/bin/google-chrome-stable",
            "browser_type": "KEYS",
            "extension_paths": ["/path/to/test_extension.crx"],
            "extension_ids": ["test-extension-id"],
            "allowlisted_extension_id": "test-extension-id",
        }
        resp = client.post("/api/browsers", json=payload)
        assert resp.status_code == 200
        assert "id" in resp.json()

    def test_TC_BROWSER_006_invalid_executable_path(self, client):
        """TC-BROWSER-006: 无效 executable_path 返回 500"""
        with patch("browser_proxy.api.browser.async_playwright") as mock_pw:
            mock_pw.return_value.__aenter__ = AsyncMock(side_effect=Exception("invalid path"))
            mock_pw.return_value.__aexit__ = AsyncMock(return_value=None)
            payload = {
                "executable_path": "/invalid/path/to/browser",
                "browser_type": "KEYS",
            }
            resp = client.post("/api/browsers", json=payload)
            assert resp.status_code == 500

    def test_TC_BROWSER_008_invalid_browser_type(self, client):
        """TC-BROWSER-008: 无效 browser_type 返回 500（ValueError by Enum）"""
        with patch("browser_proxy.api.browser.async_playwright") as mock_pw:
            playwright_instance = AsyncMock()
            mock_pw.return_value.__aenter__ = AsyncMock(return_value=playwright_instance)
            mock_pw.return_value.__aexit__ = AsyncMock(return_value=None)
            payload = {
                "executable_path": "/usr/bin/google-chrome-stable",
                "browser_type": "INVALID_TYPE",
            }
            resp = client.post("/api/browsers", json=payload)
            assert resp.status_code in (422, 500)


# ---------------------------------------------------------------------------
# 1.2 获取浏览器列表 GET /api/browsers
# ---------------------------------------------------------------------------

class TestListBrowsers:
    """TC-BROWSER-LIST-001 ~ TC-BROWSER-LIST-003"""

    def test_TC_BROWSER_LIST_001_empty_list(self, client):
        """TC-BROWSER-LIST-001: 无浏览器时返回空列表"""
        resp = client.get("/api/browsers")
        assert resp.status_code == 200
        assert resp.json() == []

    def test_TC_BROWSER_LIST_002_multiple_browsers(self, client):
        """TC-BROWSER-LIST-002: 返回多个浏览器实例"""
        for i in range(3):
            browser_list.append(_make_browser_wrapper(browser_id=f"browser-{i}"))

        resp = client.get("/api/browsers")
        assert resp.status_code == 200
        data = resp.json()
        assert len(data) == 3
        for item in data:
            assert "id" in item
            assert "used" in item
            assert "browser_type" in item

    def test_TC_BROWSER_LIST_003_used_field(self, client):
        """TC-BROWSER-LIST-003: used 字段显示正确的上下文数量"""
        b1 = _make_browser_wrapper(browser_id="browser-1")
        b2 = _make_browser_wrapper(browser_id="browser-2")

        # 向 b1 添加 2 个 mock context
        for _ in range(2):
            mock_ctx = MagicMock()
            mock_ctx.id = "ctx-" + str(id(mock_ctx))
            b1._contexts.append(mock_ctx)

        browser_list.extend([b1, b2])

        resp = client.get("/api/browsers")
        assert resp.status_code == 200
        data = {item["id"]: item for item in resp.json()}
        assert data["browser-1"]["used"] == 2
        assert data["browser-2"]["used"] == 0


# ---------------------------------------------------------------------------
# 1.3 获取指定浏览器 GET /api/browsers/{browser_id}
# ---------------------------------------------------------------------------

class TestGetBrowser:
    """TC-BROWSER-GET-001 ~ TC-BROWSER-GET-002"""

    def test_TC_BROWSER_GET_001_existing_browser(self, client):
        """TC-BROWSER-GET-001: 获取存在的浏览器信息"""
        browser_list.append(_make_browser_wrapper(browser_id="target-browser"))
        resp = client.get("/api/browsers/target-browser")
        assert resp.status_code == 200
        data = resp.json()
        assert data["id"] == "target-browser"
        assert "used" in data
        assert "browser_type" in data

    def test_TC_BROWSER_GET_002_nonexistent_browser(self, client):
        """TC-BROWSER-GET-002: 不存在的 browser_id 返回 404"""
        resp = client.get("/api/browsers/nonexistent")
        assert resp.status_code == 404
        assert "nonexistent" in resp.json()["detail"].lower() or "not found" in resp.json()["detail"].lower()


# ---------------------------------------------------------------------------
# 1.4 删除浏览器 DELETE /api/browsers/{browser_id}
# ---------------------------------------------------------------------------

class TestDeleteBrowser:
    """TC-BROWSER-DELETE-001 ~ TC-BROWSER-DELETE-002"""

    def test_TC_BROWSER_DELETE_001_delete_existing(self, client):
        """TC-BROWSER-DELETE-001: 成功删除指定浏览器"""
        wrapper = _make_browser_wrapper(browser_id="to-delete")
        browser_list.append(wrapper)

        resp = client.delete("/api/browsers/to-delete")
        assert resp.status_code == 204

        get_resp = client.get("/api/browsers/to-delete")
        assert get_resp.status_code == 404
        assert len(browser_list) == 0

    def test_TC_BROWSER_DELETE_002_delete_nonexistent(self, client):
        """TC-BROWSER-DELETE-002: 删除不存在的浏览器返回 404"""
        resp = client.delete("/api/browsers/nonexistent")
        assert resp.status_code == 404


# ---------------------------------------------------------------------------
# 1.5 健康检查 POST /api/browsers/health_check
# ---------------------------------------------------------------------------

class TestHealthCheck:
    """TC-BROWSER-HEALTH-001 ~ TC-BROWSER-HEALTH-002"""

    def test_TC_BROWSER_HEALTH_001_all_healthy(self, client):
        """TC-BROWSER-HEALTH-001: 所有上下文正常时返回 success=True"""
        resp = client.post("/api/browsers/health_check")
        assert resp.status_code == 200
        assert resp.json()["success"] is True

    def test_TC_BROWSER_HEALTH_002_unhealthy_context(self, client):
        """TC-BROWSER-HEALTH-002: 存在问题上下文时返回 success=False"""
        wrapper = _make_browser_wrapper(browser_id="browser-with-bad-ctx")

        mock_ctx = MagicMock()
        mock_ctx.id = "bad-ctx"
        mock_ctx.current = None  # 模拟没有当前页面
        wrapper._contexts.append(mock_ctx)
        browser_list.append(wrapper)

        resp = client.post("/api/browsers/health_check")
        assert resp.status_code == 200
        data = resp.json()
        assert data["success"] is False
        assert "bad-ctx" in data["err_contexts"]
