#!/usr/bin/env python
"""
测试运行脚本
启动 Web 界面服务器
"""

import uvicorn
import os
import sys

# 添加项目根目录到路径
project_root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
sys.path.insert(0, project_root)

from src.ui.app import app

if __name__ == "__main__":
    # 启动 FastAPI 服务器
    uvicorn.run(
        app,
        host="0.0.0.0",
        port=8000,
        log_level="info"
    )
