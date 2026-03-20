import pytest
from fastapi.testclient import TestClient
from unittest.mock import AsyncMock, MagicMock, patch

from browser_proxy.main import app
from browser_proxy.api.common import browser_list


@pytest.fixture(autouse=True)
def clear_browser_list():
    browser_list.clear()
    yield
    browser_list.clear()


@pytest.fixture
def client():
    with TestClient(app) as c:
        yield c


@pytest.fixture
def mock_playwright():
    with patch("browser_proxy.api.browser.async_playwright") as mock_pw, \
         patch("browser_proxy.api.browser.os.path.exists", return_value=True), \
         patch("browser_proxy.api.context.os.path.exists", return_value=True):
        playwright_instance = AsyncMock()
        mock_pw.return_value.start = AsyncMock(return_value=playwright_instance)

        # mock for launch_persistent_context (create_browser)
        mock_browser = AsyncMock()
        mock_browser.is_connected.return_value = True
        playwright_instance.chromium.launch_persistent_context = AsyncMock(return_value=mock_browser)

        # mock for browser.browser.new_context (create_context)
        mock_context = AsyncMock()
        mock_page = AsyncMock()
        mock_page.url = "about:blank"
        mock_page.title = AsyncMock(return_value="Test Page")
        mock_page2 = AsyncMock()
        mock_page2.url = "about:blank"

        mock_context.new_page = AsyncMock(side_effect=[mock_page, mock_page2])
        mock_context.new_cdp_session = AsyncMock(return_value=AsyncMock())
        mock_context.cookies = AsyncMock(return_value=[])
        mock_browser.new_context = AsyncMock(return_value=mock_context)
        mock_browser.browser = AsyncMock()
        mock_browser.browser.new_context = AsyncMock(return_value=mock_context)

        yield {
            "playwright": playwright_instance,
            "browser": mock_browser,
            "context": mock_context,
            "page": mock_page,
            "page2": mock_page2,
        }
