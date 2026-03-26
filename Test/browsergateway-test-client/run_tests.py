#!/usr/bin/env python
"""
一键执行 mobile 端到端测试套件。

用法:
    python run_tests.py           # 运行全部测试
    python run_tests.py -k login  # 只运行匹配的测试
    python run_tests.py -v        # 详细输出（默认已开启）
"""

import subprocess
import sys
import socket
import os

SERVICES = [
    ("gids-mock",      9090),
    ("browser-proxy",  8000),
    ("mobile-http",    8088),
    ("mobile-ws",      40002),
    ("bgw-tcp-ctrl",   30001),
    ("bgw-tcp-media",  30002),
    ("bgw-http",       8090),
]


def check_services():
    missing = []
    for name, port in SERVICES:
        s = socket.socket()
        s.settimeout(1)
        r = s.connect_ex(("127.0.0.1", port))
        s.close()
        status = "OK" if r == 0 else "MISSING"
        print(f"  {status:7s} {port}  {name}")
        if r != 0:
            missing.append((name, port))
    return missing


def main():
    print("=" * 50)
    print(" Mobile E2E Test Suite")
    print("=" * 50)
    print()

    print("[1/2] Checking services...")
    missing = check_services()
    print()

    if missing:
        print("ERROR: The following services are not running:")
        for name, port in missing:
            print(f"  - {name} (port {port})")
        print()
        print("Please start all services first (e.g. run start-dev.bat).")
        sys.exit(1)

    print("[2/2] Running tests...")
    print()

    root = os.path.dirname(os.path.abspath(__file__))
    python = os.path.join(root, ".venv", "Scripts", "python.exe")
    if not os.path.exists(python):
        python = sys.executable

    extra_args = sys.argv[1:]
    cmd = [python, "-m", "pytest", "tests/", "-v"] + extra_args

    result = subprocess.run(cmd, cwd=root)
    sys.exit(result.returncode)


if __name__ == "__main__":
    main()
