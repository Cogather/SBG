"""Append one summary line to reports/history.jsonl from reports/junit.xml."""

from __future__ import annotations

import json
import subprocess
import xml.etree.ElementTree as ET
from datetime import datetime, timezone
from pathlib import Path
from typing import Any, Dict, Optional


def _git_sha(cwd: Optional[Path] = None) -> Optional[str]:
    root = cwd or Path(__file__).resolve().parents[2]
    try:
        out = subprocess.run(
            ["git", "rev-parse", "--short", "HEAD"],
            cwd=root,
            capture_output=True,
            text=True,
            timeout=5,
        )
        if out.returncode == 0 and out.stdout.strip():
            return out.stdout.strip()
    except (OSError, subprocess.TimeoutExpired):
        pass
    return None


def parse_junit_summary(junit_path: Path) -> Optional[Dict[str, Any]]:
    if not junit_path.is_file():
        return None
    try:
        tree = ET.parse(junit_path)
        root_el = tree.getroot()
        if root_el.tag == "testsuites":
            suites = [c for c in root_el if c.tag == "testsuite"]
        elif root_el.tag == "testsuite":
            suites = [root_el]
        else:
            suites = []
        total_tests = 0
        failed = 0
        skipped = 0
        errors = 0
        total_time = 0.0
        for su in suites:
            total_tests += int(su.attrib.get("tests", 0) or 0)
            failed += int(su.attrib.get("failures", 0) or 0)
            errors += int(su.attrib.get("errors", 0) or 0)
            skipped += int(su.attrib.get("skipped", 0) or 0)
            total_time += float(su.attrib.get("time", 0) or 0)
        passed = total_tests - failed - errors - skipped
        return {
            "tests": total_tests,
            "passed": max(0, passed),
            "failed": failed,
            "errors": errors,
            "skipped": skipped,
            "time": round(total_time, 3),
        }
    except ET.ParseError:
        return None


def append_history(project_root: Path) -> None:
    junit = project_root / "reports" / "junit.xml"
    hist = project_root / "reports" / "history.jsonl"
    summary = parse_junit_summary(junit)
    if summary is None or summary.get("tests", 0) == 0:
        return
    row = {
        "timestamp": datetime.now(timezone.utc).isoformat(),
        "git_sha": _git_sha(project_root),
        **summary,
    }
    hist.parent.mkdir(parents=True, exist_ok=True)
    with open(hist, "a", encoding="utf-8") as f:
        f.write(json.dumps(row, ensure_ascii=False) + "\n")
