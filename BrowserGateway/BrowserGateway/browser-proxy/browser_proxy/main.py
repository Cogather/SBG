import argparse

import uvicorn
from fastapi import FastAPI, Request
from fastapi.responses import JSONResponse

from browser_proxy.api import browser, context, page
from browser_proxy.api.logger_config import logger

app = FastAPI(title="Browser Proxy")

# Mount routers
app.include_router(browser.router, prefix="/api")
app.include_router(context.router, prefix="/api")
app.include_router(page.router, prefix="/api")


@app.exception_handler(Exception)
async def global_exception_handler(request: Request, exc: Exception):
    logger.error(f"Unhandled exception: {exc}")
    return JSONResponse(status_code=500, content={"detail": str(exc)})


def main():
    parser = argparse.ArgumentParser(description="Browser Proxy Service")
    parser.add_argument("--host", default="0.0.0.0", help="Host to bind")
    parser.add_argument("--port", type=int, default=8000, help="Port to listen on")
    args = parser.parse_args()
    uvicorn.run(app, host=args.host, port=args.port)


if __name__ == "__main__":
    main()
