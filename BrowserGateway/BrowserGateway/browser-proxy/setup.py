from setuptools import setup, find_packages

setup(
    name="browser-proxy",
    version="0.1.0",
    packages=find_packages(),
    entry_points={
        "console_scripts": [
            "browser_proxy=browser_proxy.main:main",
        ],
    },
    install_requires=[
        "fastapi==0.116.1",
        "playwright==1.53.0",
        "uvicorn==0.35.0",
        "pydantic==2.11.9",
    ],
)
