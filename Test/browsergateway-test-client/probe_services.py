#!/usr/bin/env python
"""
Write reports/probe-result.json for the evaluation dashboard (port matrix + optional WS smoke).

Usage:
    python probe_services.py
    python probe_services.py --host 127.0.0.1 --ws-smoke
"""

from __future__ import annotations

import argparse
import asyncio
import json
import os
import sys
import time
from pathlib import Path

_ROOT = Path(__file__).resolve().parent
_SRC = _ROOT / "src"
sys.path.insert(0, str(_SRC))

from health.check_services import DEFAULT_HOST, probe_all  # noqa: E402


def main() -> None:
    p = argparse.ArgumentParser(description="Probe dependency ports and optional WS login smoke.")
    p.add_argument("--host", default=DEFAULT_HOST, help="TCP probe target host")
    p.add_argument("--ws-smoke", action="store_true", help="Run async WS connect+login smoke")
    p.add_argument(
        "-o",
        "--output",
        default=str(_ROOT / "reports" / "probe-result.json"),
        help="Output JSON path",
    )
    args = p.parse_args()

    out_path = Path(args.output)
    out_path.parent.mkdir(parents=True, exist_ok=True)

    t0 = time.perf_counter()
    services = probe_all(host=args.host)
    probe_ms = round((time.perf_counter() - t0) * 1000.0, 2)

    payload: dict = {
        "checked_at": time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime()),
        "host": args.host,
        "probe_duration_ms": probe_ms,
        "services": services,
    }

    if args.ws_smoke:
        from health.ws_smoke import run_ws_smoke  # noqa: E402

        ws_t0 = time.perf_counter()
        ws_result = asyncio.run(run_ws_smoke())
        ws_result["duration_ms"] = round((time.perf_counter() - ws_t0) * 1000.0, 2)
        payload["ws_smoke"] = ws_result

    with open(out_path, "w", encoding="utf-8") as f:
        json.dump(payload, f, ensure_ascii=False, indent=2)
    print(f"Wrote {out_path}")


if __name__ == "__main__":
    main()
