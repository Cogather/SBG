import asyncio
import base64

from fastapi import APIRouter, HTTPException, Request
from fastapi.responses import Response

from .common import ElementWrapper, PageWrapper, browser_get
from .logger_config import logger

router = APIRouter()

PREFIX = "/browsers/{browser_id}/contexts/{context_id}"


def _get_ctx(browser_id: str, context_id: str):
    try:
        browser = browser_get(browser_id)
    except KeyError:
        raise HTTPException(status_code=404, detail=f"Browser {browser_id} not found")
    try:
        return browser.get_context(context_id)
    except KeyError:
        raise HTTPException(status_code=404, detail=f"Context {context_id} not found")


# Task 21: POST .../pages — create new page
@router.post(PREFIX + "/pages")
async def create_page(browser_id: str, context_id: str, request: Request):
    ctx = _get_ctx(browser_id, context_id)
    try:
        body = await request.json()
        url = body.get("url")
        page = await ctx.context.new_page()
        page_wrapper = PageWrapper(
            page=page,
            browser_id=browser_id,
            context_id=context_id,
        )
        ctx.append_page(page_wrapper)
        if url:
            await page.goto(url)
        logger.info(f"Created page {page_wrapper.id} in context {context_id}")
        return ctx.to_json()
    except Exception as e:
        logger.error(f"Failed to create page: {e}")
        raise HTTPException(status_code=500, detail=str(e))


# Task 22: POST .../pages/goto — navigate current page
@router.post(PREFIX + "/pages/goto")
async def goto(browser_id: str, context_id: str, request: Request):
    ctx = _get_ctx(browser_id, context_id)
    try:
        body = await request.json()
        url = body["url"]
        page = ctx.current
        if page is None:
            raise HTTPException(status_code=400, detail="No current page")
        await page.page.goto(url)
        return ctx.to_json()
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


# Task 23: POST .../pages/execute — execute JS
@router.post(PREFIX + "/pages/execute")
async def execute(browser_id: str, context_id: str, request: Request):
    ctx = _get_ctx(browser_id, context_id)
    try:
        body = await request.json()
        expression = body["expression"]
        page = ctx.current
        if page is None:
            raise HTTPException(status_code=400, detail="No current page")
        result = await page.page.evaluate(expression)
        if result is None:
            return {"result_type": "none", "value": None}
        if isinstance(result, bool):
            return {"result_type": "bool", "value": result}
        if isinstance(result, int):
            return {"result_type": "int", "value": result}
        if isinstance(result, float):
            return {"result_type": "float", "value": result}
        if isinstance(result, str):
            return {"result_type": "string", "value": result}
        if isinstance(result, dict):
            # Check if it looks like an element handle (has nodeType etc.) — treat as dict
            return {"result_type": "dict", "value": result}
        if isinstance(result, list):
            return {"result_type": "list", "value": result}
        # Fallback
        return {"result_type": "string", "value": str(result)}
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


# Task 24: POST .../pages/execute_cdp — execute CDP command
@router.post(PREFIX + "/pages/execute_cdp")
async def execute_cdp(browser_id: str, context_id: str, request: Request):
    ctx = _get_ctx(browser_id, context_id)
    try:
        page = ctx.current
        if page is None:
            raise HTTPException(status_code=400, detail="No current page")
        if page.cdp_session is None:
            raise HTTPException(status_code=500, detail="CDP session not supported")
        body = await request.json()
        method = body["method"]
        params = body.get("params", {})
        result = await page.cdp_session.send(method, params)
        return result
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


# Task 25: POST .../pages/find_element — find element by CSS selector
@router.post(PREFIX + "/pages/find_element")
async def find_element(browser_id: str, context_id: str, request: Request):
    ctx = _get_ctx(browser_id, context_id)
    try:
        body = await request.json()
        selector = body["selector"]
        page = ctx.current
        if page is None:
            raise HTTPException(status_code=400, detail="No current page")
        locator = page.page.locator(selector).first
        count = await page.page.locator(selector).count()
        if count == 0:
            return {"result_type": "none", "value": None}
        preview = await locator.evaluate("el => el.outerHTML")
        elem = ElementWrapper(element=locator, preview=preview)
        page.set_element(elem)
        return {"result_type": "element", "value": elem.to_json()}
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


# Task 26: POST .../pages/element — element actions
@router.post(PREFIX + "/pages/element", status_code=204)
async def element_action(browser_id: str, context_id: str, request: Request):
    ctx = _get_ctx(browser_id, context_id)
    try:
        body = await request.json()
        element_id = body["element_id"]
        action = body["action"]
        value = body.get("value")
        page = ctx.current
        if page is None:
            raise HTTPException(status_code=400, detail="No current page")
        elem = page.get_element(element_id)
        if action == "send_key":
            await elem.element.fill(value or "")
        elif action == "set_file":
            await elem.element.set_input_files(value)
        elif action == "focus":
            await elem.element.focus()
        else:
            raise HTTPException(status_code=400, detail=f"Unknown action: {action}")
        return Response(status_code=204)
    except HTTPException:
        raise
    except KeyError:
        raise HTTPException(status_code=404, detail="Element not found")
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


# Task 27: POST .../pages/element/{element_id}/get_size — get element size
@router.post(PREFIX + "/pages/element/{element_id}/get_size")
async def get_element_size(browser_id: str, context_id: str, element_id: str):
    ctx = _get_ctx(browser_id, context_id)
    try:
        page = ctx.current
        if page is None:
            raise HTTPException(status_code=400, detail="No current page")
        elem = page.get_element(element_id)
        box = await elem.element.bounding_box()
        if box is None:
            raise HTTPException(status_code=404, detail="Element has no bounding box")
        return {"width": int(box["width"]), "height": int(box["height"])}
    except HTTPException:
        raise
    except KeyError:
        raise HTTPException(status_code=404, detail="Element not found")
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


# Task 28: DELETE .../pages/{page_id} — close page
@router.delete(PREFIX + "/pages/{page_id}", status_code=204)
async def delete_page(browser_id: str, context_id: str, page_id: str):
    ctx = _get_ctx(browser_id, context_id)
    try:
        page = ctx.get_page(page_id)
        await page.page.close()
        ctx.remove_page(page)
        return Response(status_code=204)
    except KeyError:
        raise HTTPException(status_code=404, detail=f"Page {page_id} not found")
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


# Task 29: POST .../pages/go_back and go_forward
@router.post(PREFIX + "/pages/go_back", status_code=204)
async def go_back(browser_id: str, context_id: str):
    ctx = _get_ctx(browser_id, context_id)
    try:
        page = ctx.current
        if page is None:
            raise HTTPException(status_code=400, detail="No current page")
        await page.page.go_back()
        return Response(status_code=204)
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@router.post(PREFIX + "/pages/go_forward", status_code=204)
async def go_forward(browser_id: str, context_id: str):
    ctx = _get_ctx(browser_id, context_id)
    try:
        page = ctx.current
        if page is None:
            raise HTTPException(status_code=400, detail="No current page")
        await page.page.go_forward()
        return Response(status_code=204)
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


# Task 30: POST .../pages/screenshot — screenshot as base64 PNG
@router.post(PREFIX + "/pages/screenshot")
async def screenshot(browser_id: str, context_id: str, request: Request):
    ctx = _get_ctx(browser_id, context_id)
    try:
        body = {}
        try:
            body = await request.json()
        except Exception:
            pass
        full_page = body.get("full_page", False)
        page = ctx.current
        if page is None:
            raise HTTPException(status_code=400, detail="No current page")
        png_bytes = await page.page.screenshot(full_page=full_page)
        return base64.b64encode(png_bytes).decode("utf-8")
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


# Task 31: POST .../pages/scroll — scroll page
@router.post(PREFIX + "/pages/scroll", status_code=204)
async def scroll(browser_id: str, context_id: str, request: Request):
    ctx = _get_ctx(browser_id, context_id)
    try:
        body = await request.json()
        x = body["x"]
        y = body["y"]
        delta_x = body["delta_x"]
        delta_y = body["delta_y"]
        page = ctx.current
        if page is None:
            raise HTTPException(status_code=400, detail="No current page")
        await page.page.mouse.wheel(delta_x, delta_y)
        return Response(status_code=204)
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


# Task 32: Touch operations via CDP
@router.post(PREFIX + "/pages/tap", status_code=204)
async def tap(browser_id: str, context_id: str, request: Request):
    ctx = _get_ctx(browser_id, context_id)
    try:
        body = await request.json()
        x = body["x"]
        y = body["y"]
        page = ctx.current
        if page is None:
            raise HTTPException(status_code=400, detail="No current page")
        if page.cdp_session is None:
            raise HTTPException(status_code=500, detail="CDP session not supported")
        cdp = page.cdp_session
        touch_point = {"x": x, "y": y, "radiusX": 1, "radiusY": 1, "force": 1}
        await cdp.send("Input.dispatchTouchEvent", {
            "type": "touchStart", "touchPoints": [touch_point]
        })
        await cdp.send("Input.dispatchTouchEvent", {
            "type": "touchEnd", "touchPoints": []
        })
        return Response(status_code=204)
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@router.post(PREFIX + "/pages/swipe", status_code=204)
async def swipe(browser_id: str, context_id: str, request: Request):
    ctx = _get_ctx(browser_id, context_id)
    try:
        body = await request.json()
        start_x = body["start_x"]
        start_y = body["start_y"]
        end_x = body["end_x"]
        end_y = body["end_y"]
        duration_ms = body.get("duration_ms", 300)
        page = ctx.current
        if page is None:
            raise HTTPException(status_code=400, detail="No current page")
        if page.cdp_session is None:
            raise HTTPException(status_code=500, detail="CDP session not supported")
        cdp = page.cdp_session
        steps = max(int(duration_ms / 16), 2)
        touch_point = {"x": start_x, "y": start_y, "radiusX": 1, "radiusY": 1, "force": 1}
        await cdp.send("Input.dispatchTouchEvent", {
            "type": "touchStart", "touchPoints": [touch_point]
        })
        for i in range(1, steps + 1):
            t = i / steps
            ix = start_x + (end_x - start_x) * t
            iy = start_y + (end_y - start_y) * t
            move_point = {"x": ix, "y": iy, "radiusX": 1, "radiusY": 1, "force": 1}
            await cdp.send("Input.dispatchTouchEvent", {
                "type": "touchMove", "touchPoints": [move_point]
            })
            await asyncio.sleep(duration_ms / steps / 1000)
        await cdp.send("Input.dispatchTouchEvent", {
            "type": "touchEnd", "touchPoints": []
        })
        return Response(status_code=204)
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@router.post(PREFIX + "/pages/touch_scroll", status_code=204)
async def touch_scroll(browser_id: str, context_id: str, request: Request):
    ctx = _get_ctx(browser_id, context_id)
    try:
        body = await request.json()
        x = body["x"]
        y = body["y"]
        delta_x = body["delta_x"]
        delta_y = body["delta_y"]
        page = ctx.current
        if page is None:
            raise HTTPException(status_code=400, detail="No current page")
        if page.cdp_session is None:
            raise HTTPException(status_code=500, detail="CDP session not supported")
        cdp = page.cdp_session
        touch_point = {"x": x, "y": y, "radiusX": 1, "radiusY": 1, "force": 1}
        await cdp.send("Input.dispatchTouchEvent", {
            "type": "touchStart", "touchPoints": [touch_point]
        })
        end_point = {"x": x - delta_x, "y": y - delta_y, "radiusX": 1, "radiusY": 1, "force": 1}
        await cdp.send("Input.dispatchTouchEvent", {
            "type": "touchMove", "touchPoints": [end_point]
        })
        await cdp.send("Input.dispatchTouchEvent", {
            "type": "touchEnd", "touchPoints": []
        })
        return Response(status_code=204)
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


# Task 33: Cookie management
@router.get(PREFIX + "/pages/cookies")
async def get_cookies(browser_id: str, context_id: str):
    ctx = _get_ctx(browser_id, context_id)
    try:
        cookies = await ctx.context.cookies()
        return cookies
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@router.post(PREFIX + "/pages/cookies", status_code=204)
async def set_cookies(browser_id: str, context_id: str, request: Request):
    ctx = _get_ctx(browser_id, context_id)
    try:
        cookies = await request.json()
        await ctx.context.add_cookies(cookies)
        return Response(status_code=204)
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@router.delete(PREFIX + "/pages/cookies", status_code=204)
async def clear_cookies(browser_id: str, context_id: str):
    ctx = _get_ctx(browser_id, context_id)
    try:
        await ctx.context.clear_cookies()
        return Response(status_code=204)
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
