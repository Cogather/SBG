#!/usr/bin/env python
"""
在本仓库测试客户端目录下提供静态 HTTP 根目录（不依赖当前工作目录）。

用法: python serve_dashboard.py [--port 8765] [--bind 127.0.0.1] [--open-browser] [--strict-port]

Windows 上 WinError 10013 常见于: 端口被 Hyper-V / 动态保留、与其它进程冲突、安全软件拦截。
本脚本会默认在首选端口失败后自动尝试其它端口（可用 --strict-port 关闭）。

端到端重跑 API（仅本机回环，供看板「执行测试」）：
  POST /api/run-tests  Content-Type: application/json
    {"check_services": true, "run_probe": true, "pytest_args": ["-k", "http"]}
  GET  /api/run-tests/status
  GET  /api/run-tests/log?tail=16000
"""

from __future__ import annotations

import argparse
import http.server
import json
import os
import socketserver
import urllib.parse
import webbrowser

from dashboard_runner import DashboardRunState, run_tests_in_background


def _port_candidates(preferred: int, strict: bool) -> list[int]:
    if strict:
        return [preferred]
    seen: set[int] = set()
    out: list[int] = []
    chain = [preferred] + list(range(preferred + 1, preferred + 12)) + [18765, 27654, 35432, 41523, 49876]
    for p in chain:
        if 1024 <= p <= 65535 and p not in seen:
            seen.add(p)
            out.append(p)
    return out


def _explain_bind_error(exc: OSError) -> str:
    win = getattr(exc, "winerror", None)
    if win == 10013:
        return (
            "Windows 拒绝绑定该端口 (WinError 10013)。常见原因:\n"
            "  • 端口落在系统保留段: 以管理员 PowerShell 执行\n"
            "      netsh interface ipv4 show excludedportrange protocol=tcp\n"
            "  • 其它程序占用或安全软件拦截\n"
            "  • 可改用: python serve_dashboard.py --port 27654\n"
        )
    if win == 10048 or exc.errno == 98:  # WSAEADDRINUSE / EADDRINUSE
        return "端口已被占用，请换 --port 或结束占用进程。\n"
    return str(exc) + "\n"


def _client_allowed(host: str) -> bool:
    h = (host or "").lower()
    if h in ("127.0.0.1", "::1", "localhost"):
        return True
    if h.startswith("::ffff:") and "127.0.0.1" in h:
        return True
    return False


def _build_handler(root: str):
    class DashboardHandler(http.server.SimpleHTTPRequestHandler):
        def __init__(self, *args, **kwargs):
            kwargs["directory"] = root
            super().__init__(*args, **kwargs)

        def log_message(self, fmt, *a):
            if self.path.startswith("/api/"):
                return
            super().log_message(fmt, *a)

        def _reject_nonlocal(self) -> bool:
            if not _client_allowed(self.client_address[0]):
                self.send_error(403, "API 仅允许本机访问")
                return True
            return False

        def _send_json(self, code: int, obj: object) -> None:
            raw = json.dumps(obj, ensure_ascii=False).encode("utf-8")
            self.send_response(code)
            self.send_header("Content-Type", "application/json; charset=utf-8")
            self.send_header("Content-Length", str(len(raw)))
            self.send_header("Cache-Control", "no-store")
            self.end_headers()
            self.wfile.write(raw)

        def _send_text(self, code: int, text: str, ctype: str) -> None:
            raw = text.encode("utf-8", errors="replace")
            self.send_response(code)
            self.send_header("Content-Type", ctype)
            self.send_header("Content-Length", str(len(raw)))
            self.send_header("Cache-Control", "no-store")
            self.end_headers()
            self.wfile.write(raw)

        def do_GET(self) -> None:
            parsed = urllib.parse.urlparse(self.path)
            if parsed.path == "/api/run-tests/status":
                if self._reject_nonlocal():
                    return
                snap = dict(DashboardRunState.snapshot())
                if snap.get("running"):
                    prog_path = os.path.join(root, "reports", "e2e-progress.json")
                    if os.path.isfile(prog_path):
                        try:
                            with open(prog_path, encoding="utf-8") as pf:
                                snap["progress"] = json.load(pf)
                        except (OSError, json.JSONDecodeError):
                            snap["progress"] = None
                    else:
                        snap["progress"] = None
                self._send_json(200, snap)
                return
            if parsed.path == "/api/run-tests/log":
                if self._reject_nonlocal():
                    return
                qs = urllib.parse.parse_qs(parsed.query)
                try:
                    tail = max(1000, min(256_000, int(qs.get("tail", ["16000"])[0])))
                except ValueError:
                    tail = 16000
                log_path = os.path.join(root, DashboardRunState.log_rel_path)
                if not os.path.isfile(log_path):
                    self._send_text(404, "暂无日志（尚未执行过测试）", "text/plain; charset=utf-8")
                    return
                with open(log_path, "rb") as f:
                    data = f.read()
                if len(data) > tail:
                    data = data[-tail:]
                self._send_text(200, data.decode("utf-8", errors="replace"), "text/plain; charset=utf-8")
                return
            super().do_GET()

        def do_POST(self) -> None:
            parsed = urllib.parse.urlparse(self.path)
            if parsed.path != "/api/run-tests":
                self.send_error(404)
                return
            if self._reject_nonlocal():
                return
            length = int(self.headers.get("Content-Length", "0") or "0")
            body = self.rfile.read(length) if length > 0 else b"{}"
            try:
                opts = json.loads(body.decode("utf-8") or "{}")
            except json.JSONDecodeError:
                self._send_json(400, {"ok": False, "message": "JSON 无效"})
                return
            if not isinstance(opts, dict):
                self._send_json(400, {"ok": False, "message": "请求体须为 JSON 对象"})
                return
            check_services = bool(opts.get("check_services", True))
            run_probe = bool(opts.get("run_probe", True))
            pytest_args = opts.get("pytest_args")
            if pytest_args is None:
                pytest_args = []
            if not isinstance(pytest_args, list) or not all(
                isinstance(x, str) for x in pytest_args
            ):
                self._send_json(400, {"ok": False, "message": "pytest_args 须为字符串数组"})
                return
            ok, msg = run_tests_in_background(
                root,
                pytest_args=pytest_args,
                check_services=check_services,
                run_probe=run_probe,
            )
            if ok:
                self._send_json(202, {"ok": True, "message": msg})
            else:
                self._send_json(409, {"ok": False, "message": msg})

    return DashboardHandler


def main() -> None:
    p = argparse.ArgumentParser(description="Serve browsergateway-test-client over HTTP.")
    p.add_argument("--port", type=int, default=8765, help="首选端口（失败时自动换端口，除非 --strict-port）")
    p.add_argument("--bind", default="127.0.0.1", help="监听地址，默认仅本机")
    p.add_argument("--open-browser", action="store_true", help="绑定成功后打开看板 URL")
    p.add_argument("--strict-port", action="store_true", help="不自动换端口，绑定失败直接退出")
    args = p.parse_args()

    root = os.path.dirname(os.path.abspath(__file__))
    os.chdir(root)

    handler = _build_handler(root)

    class _Reuse(socketserver.TCPServer):
        allow_reuse_address = True

    candidates = _port_candidates(args.port, args.strict_port)
    binds = [args.bind]
    if args.bind == "127.0.0.1":
        binds.append("0.0.0.0")

    httpd: socketserver.TCPServer | None = None
    listen_host = ""
    listen_port = 0
    last_err: OSError | None = None

    for host in binds:
        for port in candidates:
            try:
                httpd = _Reuse((host, port), handler)
                listen_host, listen_port = host, port
                break
            except OSError as e:
                last_err = e
                continue
        if httpd is not None:
            break

    if httpd is None:
        print(_explain_bind_error(last_err) if last_err else "无法绑定端口。")
        raise SystemExit(1)

    display_host = "127.0.0.1" if listen_host == "0.0.0.0" else listen_host
    base = f"http://{display_host}:{listen_port}"
    url = f"{base}/dashboard/"

    print(f"Serving: {root}")
    print(f"Open: {url}")
    if listen_port != args.port:
        print(f"(首选端口 {args.port} 不可用，已改用 {listen_port})")
    if listen_host == "0.0.0.0":
        print("(监听 0.0.0.0；请优先用上方 127.0.0.1 链接访问)")
    print("Ctrl+C to stop.")
    print("E2E API: POST /api/run-tests (本机) 可在看板中重新执行 run_tests.py + 探针。")

    if args.open_browser:
        webbrowser.open(url)

    try:
        with httpd:
            httpd.serve_forever()
    except KeyboardInterrupt:
        print("\nStopped.")


if __name__ == "__main__":
    main()
