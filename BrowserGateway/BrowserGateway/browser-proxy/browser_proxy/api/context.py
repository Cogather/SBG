import json
import os

from fastapi import APIRouter, HTTPException, Request
from playwright.async_api import ViewportSize

from .common import ContextWrapper, PageWrapper, browser_get
from .logger_config import logger

router = APIRouter()


@router.delete("/browsers/{browser_id}/contexts/{context_id}", status_code=204)
async def delete_context(browser_id: str, context_id: str):
    try:
        browser = browser_get(browser_id)
        context = browser.get_context(context_id)
        await context.close()
        browser.remove_context(context)
    except HTTPException as error:
        if error.status_code == 404:
            return
        raise
    except Exception as error:
        logger.error(str(error))
        raise HTTPException(status_code=500, detail=str(error))


@router.get("/browsers/{browser_id}/contexts/{context_id}")
async def get_context(browser_id: str, context_id: str):
    try:
        browser = browser_get(browser_id)
        context = browser.get_context(context_id)
        return context.as_json()
    except HTTPException:
        raise
    except Exception as error:
        logger.error(str(error))
        raise HTTPException(status_code=500, detail=str(error))


@router.get("/browsers/{browser_id}/contexts")
async def list_contexts(browser_id: str):
    try:
        browser = browser_get(browser_id)
        ret = []
        for context in browser.contexts:
            ret.append(context.as_json())
        return ret
    except HTTPException:
        raise
    except Exception as error:
        logger.error(str(error))
        raise HTTPException(status_code=500, detail=str(error))


@router.post("/browsers/{browser_id}/contexts")
async def create_context(browser_id: str, request: Request):
    try:
        request_body = await request.json()
        url = request_body.get('url')
        data = request_body.get('data')
        viewport = request_body.get('viewport')
        userdata = request_body.get('userdata')
        language = request_body.get('language', 'en_US')
        locale = language.replace("_", "-")

        browser = browser_get(browser_id)

        if not os.path.exists(userdata):
            directory = os.path.dirname(userdata)
            if directory and not os.path.exists(directory):
                os.makedirs(directory, exist_ok=True)
            with open(userdata, 'w', encoding='utf-8') as f:
                json.dump({}, f, ensure_ascii=False, indent=4)
        context_p = await browser.browser.new_context(
            storage_state=userdata,
            viewport=ViewportSize({'width': viewport.get('width'), 'height': viewport.get('height')}),
            locale=locale,
            is_mobile=True,
            has_touch=True
        )

        page_p = await context_p.new_page()
        await page_p.goto(url)
        page_p2 = await context_p.new_page()
        formatted_expression = f"(async () => await START_RECORDING({data}))()"
        await page_p.evaluate(formatted_expression)
        cdp_session = await context_p.new_cdp_session(page_p2)

        context = ContextWrapper(browser_id, context_p, userdata)
        page = PageWrapper(page_p2, browser_id, context.id, cdp_session)
        context.append_page(page)
        browser.append_context(context)

        return context.as_json()
    except HTTPException:
        raise
    except Exception as error:
        logger.error(str(error))
        raise HTTPException(status_code=500, detail=str(error))
