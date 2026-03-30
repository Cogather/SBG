"""
看板触发的端到端测试执行（由 serve_dashboard 在后台线程中调用）。

与命令行 `python run_tests.py` 行为一致：可选跳过依赖检查、pytest 附加参数、结束后写探针。
"""

from __future__ import annotations

import os
import subprocess
import sys
import threading
import time
from typing import Any, Dict, List, Optional


def _python_exe(root: str) -> str:
    win = os.path.join(root, ".venv", "Scripts", "python.exe")
    if os.path.isfile(win):
        return win
    return sys.executable


class DashboardRunState:
    """单次全局任务状态（仅本机看板使用）。"""

    lock = threading.Lock()
    running = False
    exit_code: Optional[int] = None
    error_message: Optional[str] = None
    started_at: Optional[float] = None
    finished_at: Optional[float] = None
    log_rel_path = "reports/run-tests-last.log"

    @classmethod
    def snapshot(cls) -> Dict[str, Any]:
        with cls.lock:
            return {
                "running": cls.running,
                "exit_code": cls.exit_code,
                "error_message": cls.error_message,
                "started_at": cls.started_at,
                "finished_at": cls.finished_at,
                "log_path": cls.log_rel_path,
            }


def run_tests_in_background(
    root: str,
    pytest_args: Optional[List[str]] = None,
    check_services: bool = True,
    run_probe: bool = True,
) -> tuple[bool, str]:
    """
    若当前无任务在跑，启动后台线程执行测试。
    返回 (是否成功启动, 说明信息)。
    """
    pytest_args = pytest_args or []
    with DashboardRunState.lock:
        if DashboardRunState.running:
            return False, "已有测试正在执行，请等待结束后再试。"
        DashboardRunState.running = True
        DashboardRunState.exit_code = None
        DashboardRunState.error_message = None
        DashboardRunState.started_at = time.time()
        DashboardRunState.finished_at = None

    thread = threading.Thread(
        target=_worker,
        args=(root, pytest_args, check_services, run_probe),
        name="dashboard-e2e-runner",
        daemon=True,
    )
    thread.start()
    return True, "started"


def _worker(
    root: str,
    pytest_args: List[str],
    check_services: bool,
    run_probe: bool,
) -> None:
    log_path = os.path.join(root, DashboardRunState.log_rel_path)
    os.makedirs(os.path.dirname(log_path), exist_ok=True)
    py = _python_exe(root)
    env = os.environ.copy()
    if not check_services:
        env["E2E_SKIP_SERVICE_CHECK"] = "1"
    rc: Optional[int] = None
    err_msg: Optional[str] = None
    try:
        with open(log_path, "w", encoding="utf-8", newline="\n") as logf:
            logf.write(f"=== run_tests.py {' '.join(pytest_args) or '(default)'} ===\n")
            logf.write(
                f"check_services={check_services} run_probe={run_probe}\n\n"
            )
            logf.flush()
            cmd = [py, os.path.join(root, "run_tests.py")] + pytest_args
            logf.write(f"$ {' '.join(cmd)}\n\n")
            logf.flush()
            p = subprocess.run(
                cmd,
                cwd=root,
                env=env,
                stdout=logf,
                stderr=subprocess.STDOUT,
                timeout=3600,
            )
            rc = p.returncode
            if run_probe:
                logf.write("\n=== probe_services.py ===\n")
                logf.flush()
                subprocess.run(
                    [py, os.path.join(root, "probe_services.py")],
                    cwd=root,
                    env=os.environ.copy(),
                    stdout=logf,
                    stderr=subprocess.STDOUT,
                    timeout=180,
                )
    except subprocess.TimeoutExpired:
        err_msg = "执行超时（>1h）"
        rc = 124
    except Exception as exc:
        err_msg = str(exc)
        rc = 1
        try:
            with open(log_path, "a", encoding="utf-8") as logf:
                logf.write(f"\n[runner error] {err_msg}\n")
        except OSError:
            pass
    finally:
        with DashboardRunState.lock:
            DashboardRunState.running = False
            DashboardRunState.exit_code = rc if rc is not None else 1
            DashboardRunState.error_message = err_msg
            DashboardRunState.finished_at = time.time()
