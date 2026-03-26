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
import os
import subprocess
import sys
import time

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
