import json
import os
import uuid
from pathlib import Path

from fastapi import APIRouter, HTTPException, Request
from playwright.async_api import async_playwright

from .common import BrowserType, BrowserWrapper, browser_get, browser_list
from .logger_config import logger

router = APIRouter()


@router.delete("/browsers/{browser_id}", status_code=204)
async def delete_browser(browser_id: str):
    try:
        browser = browser_get(browser_id)
    except HTTPException:
        raise
    try:
        await browser.close()
        browser_list.remove(browser)
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@router.get("/browsers/{browser_id}")
async def get_browser(browser_id: str):
    try:
        browser = browser_get(browser_id)
        return browser.as_json()
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@router.get("/browsers")
async def list_browsers():
    try:
        return [b.as_json() for b in browser_list]
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@router.post("/browsers")
async def create_browser(request: Request):
    try:
        request_body = await request.json()
        executable_path = Path(request_body.get('executable_path'))
        userdata = request_body.get('base_data')
        extension_paths = ",".join(request_body.get('extension_paths') or [])
        extension_ids = ",".join(request_body.get('extension_ids') or [])
        allowlisted_extension_id = request_body.get('allowlisted_extension_id')
        browser_type = request_body.get('browser_type')
        browser_id = request_body.get('browser_id', '')
        headless = request_body.get('headless', True)
        language = request_body.get('language', 'en-US')
        format_language = language.replace("_", "-")

        if not os.path.exists(userdata):
            os.makedirs(os.path.join(userdata, 'Default'), exist_ok=True)
            preferences_path = os.path.join(userdata, 'Default', 'Preferences')
            with open(preferences_path, 'w', encoding='utf-8') as f:
                json_content = {
                    'extensions': {
                        'settings': {
                            request_body.get('extension_ids')[0]: {
                                'incognito': True
                            },
                            request_body.get('extension_ids')[1]: {
                                'incognito': True
                            }
                        },
                        'ui': {'developer_mode': True},
                    },
                }
                json.dump(json_content, f, ensure_ascii=False, indent=4)

        playwright = await async_playwright().start()
        context = await playwright.chromium.launch_persistent_context(
            user_data_dir=userdata,
            headless=headless,
            executable_path=str(executable_path),
            ignore_default_args=[
                "--enable-automation",
                "--enable-logging",
                "--mute-audio",
            ],
            ignore_https_errors=True,
            chromium_sandbox=False,
            args=[
                "--load-extension=" + extension_paths,
                "--disable-extensions-except=" + extension_paths,
                "--enable-extension=" + extension_ids,
                *((["--allowlisted-extension-id=" + allowlisted_extension_id]) if allowlisted_extension_id else []),
                "--lang=" + format_language,

                "--touch-events=enabled",
                "--disable-web-security",
                "--profile.default_content_settings.popups=0",
                "--no-sandbox",
                "--disable-dev-shm-usage",
                "--autoplay-policy=no-user-gesture-required",
                "--disable-blink-features=AutomationControlled",
                "--disable-gpu",
                "--log-level=3",
                "--ignore-certificate-errors",
            ]
        )

        browser = BrowserWrapper(browser=context.browser, browser_type=BrowserType(browser_type), browser_id=browser_id
                                 , userdata=userdata, playwright=playwright)
        browser_list.append(browser)
        return browser.as_json()

    except HTTPException:
        raise  # 直接抛出已定义的HTTP异常
    except Exception as error:
        logger.error(str(error))
        raise HTTPException(status_code=500, detail=str(error))

@router.post("/browsers/health_check")
async def health_check():
    try:
        err_contexts = []
        for browser in browser_list:
            for ctx in browser.contexts:
                try:
                    page = ctx.current
                    if page is None:
                        err_contexts.append(ctx.id)
                        continue
                    await page.page.title()
                except Exception:
                    err_contexts.append(ctx.id)
        if err_contexts:
            return {"success": False, "err_contexts": err_contexts}
        return {"success": True}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
