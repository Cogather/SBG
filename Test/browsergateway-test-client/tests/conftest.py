"""
pytest fixtures for mobile end-to-end tests.

Requirements:
- GIDS Mock is started automatically by the `gids_mock` fixture.
- mobile service must already be running on localhost:40002.

Session-level shared client:
  All tests share a single logged-in MobileTestClient to avoid repeated
  Chrome cold-starts and reduce total test time.

Function-level fresh client:
  `fresh_mobile_client` provides a new per-test connection for tests that
  need to test connect/disconnect behaviour (e.g. test_logout).
"""

import asyncio
import json
import os
import subprocess
import sys
import time
from pathlib import Path

import httpx
import pytest
import pytest_asyncio
from asyncio import AbstractEventLoop
from typing import Generator

_ROOT = os.path.join(os.path.dirname(__file__), "..")
_SRC = os.path.join(_ROOT, "src")
if _SRC not in sys.path:
    sys.path.insert(0, _SRC)

from client.mobile_client import MobileTestClient

_REPORTS = Path(__file__).resolve().parent.parent / "reports"
_E2E_PROGRESS = _REPORTS / "e2e-progress.json"


def _e2e_progress_read() -> dict:
    if not _E2E_PROGRESS.is_file():
        return {}
    try:
        return json.loads(_E2E_PROGRESS.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError):
        return {}


def _e2e_progress_write(data: dict) -> None:
    _REPORTS.mkdir(parents=True, exist_ok=True)
    tmp = _E2E_PROGRESS.with_suffix(".json.tmp")
    tmp.write_text(json.dumps(data, ensure_ascii=False, indent=0), encoding="utf-8")
    tmp.replace(_E2E_PROGRESS)


def pytest_configure(config):
    _REPORTS.mkdir(parents=True, exist_ok=True)


@pytest.hookimpl(tryfirst=True)
def pytest_sessionstart(session):
    """看板轮询用：会话开始即标记运行中（收集用例前）。"""
    _e2e_progress_write(
        {
            "version": 1,
            "running": True,
            "pytest_finished": False,
            "phase": "collecting",
            "order": [],
            "current": None,
            "completed": [],
        }
    )


@pytest.hookimpl(trylast=True)
def pytest_collection_finish(session):
    """收集完成后写入完整用例顺序，供看板展示「待执行 / 运行中」。"""
    _e2e_progress_write(
        {
            "version": 1,
            "running": True,
            "pytest_finished": False,
            "phase": "running",
            "order": [item.nodeid for item in session.items],
            "current": None,
            "completed": [],
        }
    )


def pytest_runtest_logstart(nodeid, location):
    _data = _e2e_progress_read()
    if not _data.get("running"):
        return
    _data["current"] = nodeid
    _data["phase"] = "running"
    _e2e_progress_write(_data)


@pytest.hookimpl(trylast=True)
def pytest_runtest_logreport(report):
    if report.when == "call":
        pass
    elif report.when == "setup" and report.failed:
        pass
    else:
        return
    _data = _e2e_progress_read()
    if not _data:
        return
    outcome = report.outcome if report.when == "call" else "failed"
    dur = getattr(report, "duration", 0) or 0 if report.when == "call" else 0.0
    _data.setdefault("completed", []).append(
        {"nodeid": report.nodeid, "outcome": outcome, "duration": dur}
    )
    _e2e_progress_write(_data)


@pytest.hookimpl(trylast=True)
def pytest_sessionfinish(session, exitstatus):
    """结束会话：标记 running=false，并追加 history。"""
    _data = _e2e_progress_read()
    _data["running"] = False
    _data["pytest_finished"] = True
    _data["current"] = None
    _data["phase"] = "done"
    _data["exitstatus"] = exitstatus
    _e2e_progress_write(_data)

    root = _REPORTS.parent
    try:
        from health.history import append_history

        append_history(root)
    except Exception:
        pass


IMEI = "123456789012345"
IMSI = "987654321098765"
GIDS_PORT = 9090
GIDS_MOCK_SCRIPT = os.path.join(_SRC, "mock", "gids_mock_server.py")


@pytest.fixture(scope="session")
def event_loop() -> Generator[AbstractEventLoop, None, None]:
    """Session-scoped event loop so all async fixtures and tests share one loop."""
    loop = asyncio.new_event_loop()
    yield loop
    loop.close()


@pytest.fixture(scope="session")
def gids_mock():
    """Start GIDS Mock Server as a subprocess for the test session."""
    proc = subprocess.Popen(
        [sys.executable, GIDS_MOCK_SCRIPT],
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
    )
    health_url = f"http://127.0.0.1:{GIDS_PORT}/health"
    deadline = time.time() + 10
    while time.time() < deadline:
        try:
            r = httpx.get(health_url, timeout=1)
            if r.status_code == 200:
                break
        except Exception:
            pass
        time.sleep(0.3)
    else:
        proc.terminate()
        raise RuntimeError(
            f"GIDS Mock Server did not become healthy within 10s. "
            f"stdout: {proc.stdout.read()}"
        )
    yield proc
    proc.terminate()
    try:
        proc.wait(timeout=5)
    except subprocess.TimeoutExpired:
        proc.kill()


@pytest_asyncio.fixture(scope="session")
async def mobile_client(gids_mock):
    """
    Session-scoped shared MobileTestClient.
    Connects and logs in once; all tests reuse the same connection.
    """
    client = MobileTestClient()
    await client.connect(IMEI, IMSI)
    await client.login()
    # Allow mobile time to establish TCP channel and start streaming
    await asyncio.sleep(5)
    yield client
    await client.disconnect()


@pytest.fixture
def fresh_mobile_client(gids_mock):
    """
    Function-scoped MobileTestClient for tests that need a fresh connection
    (e.g. logout / reconnect tests).
    """
    client = MobileTestClient()
    yield client
    try:
        loop = asyncio.get_event_loop()
        if not loop.is_closed():
            loop.run_until_complete(client.disconnect())
    except Exception:
        pass
