#!/usr/bin/env python
"""
一键执行 mobile 端到端测试套件。

用法:
    python run_tests.py           # 运行全部测试
    python run_tests.py -k login  # 只运行匹配的测试
    python run_tests.py -v        # 详细输出（默认已开启）
"""

import os
import subprocess
import sys

_ROOT = os.path.dirname(os.path.abspath(__file__))
_SRC = os.path.join(_ROOT, "src")
if _SRC not in sys.path:
    sys.path.insert(0, _SRC)

from health.check_services import check_services  # noqa: E402


def main():
    print("=" * 50)
    print(" Mobile E2E Test Suite")
    print("=" * 50)
    print()

    print("[1/2] Checking services...")
    skip_check = os.environ.get("E2E_SKIP_SERVICE_CHECK", "").lower() in (
        "1",
        "true",
        "yes",
    )
    missing = [] if skip_check else check_services()
    if skip_check:
        print("  (E2E_SKIP_SERVICE_CHECK: skipping port check — e.g. HTTP-only from dashboard)")
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

    reports = os.path.join(_ROOT, "reports")
    os.makedirs(reports, exist_ok=True)

    python = os.path.join(_ROOT, ".venv", "Scripts", "python.exe")
    if not os.path.exists(python):
        python = sys.executable

    extra_args = sys.argv[1:]
    # junit.xml / pytest-html paths come from pytest.ini addopts
    cmd = [python, "-m", "pytest", "tests/", "-v"] + extra_args

    result = subprocess.run(cmd, cwd=_ROOT)
    sys.exit(result.returncode)


if __name__ == "__main__":
    main()
