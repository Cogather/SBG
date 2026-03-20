"""上下文管理 API 测试用例
对应文档: doc/测试用例文档.md - 第2章
"""
import pytest
from unittest.mock import AsyncMock, MagicMock

from browser_proxy.api.common import BrowserType, BrowserWrapper, ContextWrapper, PageWrapper, browser_list


# ---------------------------------------------------------------------------
# helpers
# ---------------------------------------------------------------------------

def _make_browser(browser_id: str = "browser-1") -> BrowserWrapper:
    mock_browser = MagicMock()
    mock_browser.is_connected.return_value = True
    mock_playwright = AsyncMock()
    return BrowserWrapper(
        browser_id=browser_id,
        browser=mock_browser,
        browser_type=BrowserType.KEYS,
        userdata="",
        playwright=mock_playwright,
    )


def _make_context(browser_id: str, context_id: str = None) -> ContextWrapper:
    mock_ctx = AsyncMock()
    mock_ctx.cookies = AsyncMock(return_value=[])
    mock_ctx.add_cookies = AsyncMock()
    mock_ctx.clear_cookies = AsyncMock()
    ctx = ContextWrapper(
        browser_id=browser_id,
        context=mock_ctx,
        userdata="",
    )
    if context_id:
        ctx._id = context_id
    return ctx


# ---------------------------------------------------------------------------
# 2.1 创建上下文 POST /api/browsers/{browser_id}/contexts
# ---------------------------------------------------------------------------

class TestCreateContext:
    """TC-CTX-001 ~ TC-CTX-003"""

    def test_TC_CTX_001_create_basic(self, client, mock_playwright):
        """TC-CTX-001: 成功创建基本上下文"""
        create_resp = client.post("/api/browsers", json={
            "executable_path": "/usr/bin/google-chrome-stable",
            "browser_type": "KEYS",
        })
        assert create_resp.status_code == 200
        browser_id = create_resp.json()["id"]

        resp = client.post(f"/api/browsers/{browser_id}/contexts", json={
            "url": "https://www.example.com",
            "userdata": "/tmp/test_userdata.json",
            "viewport": {"width": 375, "height": 812},
            "data": "{}",
        })
        assert resp.status_code == 200
        data = resp.json()
        assert "id" in data
        assert data["browser_id"] == browser_id

    def test_TC_CTX_002_create_with_url(self, client, mock_playwright):
        """TC-CTX-002: 创建上下文并导航到指定 URL"""
        create_resp = client.post("/api/browsers", json={
            "executable_path": "/usr/bin/google-chrome-stable",
            "browser_type": "KEYS",
        })
        browser_id = create_resp.json()["id"]

        resp = client.post(f"/api/browsers/{browser_id}/contexts", json={
            "url": "https://www.example.com",
            "userdata": "/tmp/test_userdata.json",
            "viewport": {"width": 375, "height": 812},
            "data": "{}",
        })
        assert resp.status_code == 200
        assert "id" in resp.json()

    def test_TC_CTX_003_create_nonexistent_browser(self, client):
        """TC-CTX-003: 对不存在的浏览器创建上下文返回 404"""
        resp = client.post("/api/browsers/nonexistent/contexts", json={})
        assert resp.status_code == 404


# ---------------------------------------------------------------------------
# 2.2 获取上下文列表 GET /api/browsers/{browser_id}/contexts
# ---------------------------------------------------------------------------

class TestListContexts:
    """TC-CTX-LIST-001 ~ TC-CTX-LIST-002"""

    def test_TC_CTX_LIST_001_empty(self, client):
        """TC-CTX-LIST-001: 浏览器无上下文时返回空列表"""
        b = _make_browser("browser-empty")
        browser_list.append(b)

        resp = client.get("/api/browsers/browser-empty/contexts")
        assert resp.status_code == 200
        assert resp.json() == []

    def test_TC_CTX_LIST_002_multiple_contexts(self, client):
        """TC-CTX-LIST-002: 返回多个上下文列表"""
        b = _make_browser("browser-multi")
        for _ in range(3):
            ctx = _make_context("browser-multi")
            b.append_context(ctx)
        browser_list.append(b)

        resp = client.get("/api/browsers/browser-multi/contexts")
        assert resp.status_code == 200
        assert len(resp.json()) == 3

    def test_TC_CTX_LIST_003_nonexistent_browser(self, client):
        """TC-CTX-LIST-003: 不存在的浏览器返回 404"""
        resp = client.get("/api/browsers/nonexistent/contexts")
        assert resp.status_code == 404


# ---------------------------------------------------------------------------
# 2.3 获取指定上下文 GET /api/browsers/{browser_id}/contexts/{context_id}
# ---------------------------------------------------------------------------

class TestGetContext:
    """TC-CTX-GET-001 ~ TC-CTX-GET-002"""

    def test_TC_CTX_GET_001_existing_context(self, client):
        """TC-CTX-GET-001: 获取存在的上下文"""
        b = _make_browser("browser-get")
        ctx = _make_context("browser-get", context_id="ctx-target")
        b.append_context(ctx)
        browser_list.append(b)

        resp = client.get("/api/browsers/browser-get/contexts/ctx-target")
        assert resp.status_code == 200
        assert resp.json()["id"] == "ctx-target"

    def test_TC_CTX_GET_002_nonexistent_context(self, client):
        """TC-CTX-GET-002: 不存在的上下文返回 404"""
        b = _make_browser("browser-get2")
        browser_list.append(b)

        resp = client.get("/api/browsers/browser-get2/contexts/nonexistent")
        assert resp.status_code == 404


# ---------------------------------------------------------------------------
# 2.4 删除上下文 DELETE /api/browsers/{browser_id}/contexts/{context_id}
# ---------------------------------------------------------------------------

class TestDeleteContext:
    """TC-CTX-DELETE-001 ~ TC-CTX-DELETE-002"""

    def test_TC_CTX_DELETE_001_delete_existing(self, client):
        """TC-CTX-DELETE-001: 成功删除指定上下文"""
        b = _make_browser("browser-del")
        ctx = _make_context("browser-del", context_id="ctx-to-del")
        b.append_context(ctx)
        browser_list.append(b)

        resp = client.delete("/api/browsers/browser-del/contexts/ctx-to-del")
        assert resp.status_code == 204

        get_resp = client.get("/api/browsers/browser-del/contexts/ctx-to-del")
        assert get_resp.status_code == 404

    def test_TC_CTX_DELETE_002_delete_nonexistent(self, client):
        """TC-CTX-DELETE-002: 删除不存在的上下文幂等返回 204"""
        b = _make_browser("browser-del2")
        browser_list.append(b)

        resp = client.delete("/api/browsers/browser-del2/contexts/nonexistent")
        assert resp.status_code == 204
