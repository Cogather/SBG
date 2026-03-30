"""
TCP port reachability checks (same contract as run_tests.py SERVICES).

Returns structured results for dashboards and CLI.
"""

from __future__ import annotations

import socket
import time
from typing import Any, List, Tuple

# (display_name, port) — host default 127.0.0.1
SERVICES: List[Tuple[str, int]] = [
    ("gids-mock", 9090),
    ("browser-proxy", 8000),
    ("mobile-http", 8088),
    ("mobile-ws", 40002),
    ("bgw-tcp-ctrl", 30001),
    ("bgw-tcp-media", 30002),
    ("bgw-http", 8090),
]

DEFAULT_HOST = "127.0.0.1"
CONNECT_TIMEOUT_S = 1.0


def _probe_one(host: str, port: int, timeout_s: float = CONNECT_TIMEOUT_S) -> tuple[bool, float | None]:
    """Return (ok, latency_ms). latency is None if failed."""
    t0 = time.perf_counter()
    s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    s.settimeout(timeout_s)
    try:
        r = s.connect_ex((host, port))
        elapsed_ms = (time.perf_counter() - t0) * 1000.0
        return (r == 0, round(elapsed_ms, 2) if r == 0 else None)
    finally:
        s.close()


def probe_all(host: str = DEFAULT_HOST, timeout_s: float = CONNECT_TIMEOUT_S) -> List[dict[str, Any]]:
    """
    Probe all SERVICES on host.

    Each item: {"name": str, "port": int, "ok": bool, "latency_ms": float | null}
    """
    out: List[dict[str, Any]] = []
    for name, port in SERVICES:
        ok, lat = _probe_one(host, port, timeout_s)
        out.append(
            {
                "name": name,
                "port": port,
                "ok": ok,
                "latency_ms": lat,
            }
        )
    return out


def check_services(host: str = DEFAULT_HOST, timeout_s: float = CONNECT_TIMEOUT_S) -> List[Tuple[str, int]]:
    """
    Print status lines like legacy run_tests; return list of missing (name, port).
    """
    missing: List[Tuple[str, int]] = []
    for row in probe_all(host, timeout_s):
        status = "OK" if row["ok"] else "MISSING"
        lat = row["latency_ms"]
        extra = f"  ({lat} ms)" if lat is not None else ""
        print(f"  {status:7s} {row['port']}  {row['name']}{extra}")
        if not row["ok"]:
            missing.append((row["name"], row["port"]))
    return missing
