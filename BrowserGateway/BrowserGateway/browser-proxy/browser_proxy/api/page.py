import asyncio
import base64
import json
import re

from fastapi import APIRouter, HTTPException, Request
from fastapi.responses import Response
from playwright.async_api import JSHandle

from .common import ElementWrapper, PageWrapper, browser_get
from .logger_config import logger

router = APIRouter()

PREFIX = "/browsers/{browser_id}/contexts/{context_id}"

ELEMENT_JSON = 'ref: <Node>'
FOCUS_ELEMENT = 'focusElement'


async def handle_element_node(js_handler: JSHandle, page: PageWrapper, key=None):
    if key is None:
        element_p = js_handler.as_element()
    else:
        handler_p = await js_handler.get_property(key)
        element_p = handler_p.as_element()
    element = ElementWrapper(element_p)
    ret = await element.as_json()
    if key == FOCUS_ELEMENT:
        await element_p.focus()
        await element.close()
    else:
        page.set_element(element)
    return ret


@router.post(PREFIX + "/pages/goto")
async def goto_page(request: Request):
    try:
        browser_id = request.path_params.get('browser_id')
        context_id = request.path_params.get('context_id')
        browser = browser_get(browser_id)
        context = browser.get_context(context_id)

        request_body = await request.json()
        url = request_body.get('url')

        page_p = context.current.page
        await page_p.goto(url)
        return context.as_json()
    except HTTPException:
        raise
    except Exception as error:
        logger.error(str(error))
        raise HTTPException(status_code=500, detail=str(error))


@router.post(PREFIX + "/pages/execute_cdp")
async def pages_execute_cdp(request: Request):
    try:
        browser_id = request.path_params.get('browser_id')
        context_id = request.path_params.get('context_id')

        browser = browser_get(browser_id)
        context = browser.get_context(context_id)

        request_body = await request.json()
        method = request_body.get('method')
        params = request_body.get('params')

        result = await context.current.cdp_session.send(method, params)
        return result
    except HTTPException:
        raise
    except Exception as error:
        logger.error(str(error))
        raise HTTPException(status_code=500, detail=str(error))


@router.post(PREFIX + "/pages/execute")
async def pages_execute(request: Request):
    try:
        browser_id = request.path_params.get('browser_id')
        context_id = request.path_params.get('context_id')

        request_body = await request.json()
        expression = request_body.get('expression')
        formatted_expression = f"() => {{{expression}}}"
        if 'await' in expression:
            formatted_expression = f"(async () => {{{expression}}})()"
        browser = browser_get(browser_id)
        context = browser.get_context(context_id)
        js_handler = await context.current.page.evaluate_handle(formatted_expression)
        eva_result = await js_handler.json_value()

        if eva_result is None and 'activeElement' in expression:
            js_handler = await context.current.page.evaluate_handle('() => document.activeElement')
            await js_handler.evaluate('(e) => console.log(e)')
            eva_result = await js_handler.json_value()

        if isinstance(eva_result, str):
            if eva_result == ELEMENT_JSON:
                eva_result = await handle_element_node(js_handler, context.current)
                return {
                    'result_type': 'element',
                    'value': json.dumps(eva_result)
                }
            return {
                'result_type': 'string',
                'value': eva_result
            }
        elif isinstance(eva_result, int):
            return {
                'result_type': 'int',
                'value': eva_result
            }
        elif eva_result is None:
            return {
                'result_type': 'none',
                'value': eva_result,
            }
        elif isinstance(eva_result, dict):
            element_keys = []
            for key in eva_result.keys():
                if eva_result[key] == ELEMENT_JSON:
                    eva_result[key] = await handle_element_node(js_handler, context.current, key=key)
                    element_keys.append(key)
            return {
                'result_type': 'dict',
                'element_keys': element_keys,
                'value': json.dumps(eva_result),
            }
        raise HTTPException(status_code=500, detail='eva_result type is not supported')
    except HTTPException:
        raise
    except Exception as error:
        if "Execution context was destroyed" in str(error):
            return {'result_type': 'none'}
        if "is not a function" in str(error):
            logger.warning("function not match")
            return {'result_type': 'none'}
        logger.error(str(error))
        raise HTTPException(status_code=500, detail=str(error))


@router.post(PREFIX + "/pages/element")
async def pages_element(request: Request):
    try:
        browser_id = request.path_params.get('browser_id')
        context_id = request.path_params.get('context_id')
        browser = browser_get(browser_id)
        context = browser.get_context(context_id)

        request_body = await request.json()
        element_id = request_body.get('element_id')
        action = request_body.get('action')
        value = request_body.get('value')
        value = re.sub(r'\x00+', '', value)
        element = context.current.get_element(element_id)
        await element.element.focus()
        if action == 'send_key':
            await element.element.fill(value)
        elif action == 'set_file':
            await element.element.set_input_files(value)
        elif action == 'focus':
            pass
        else:
            raise HTTPException(status_code=500, detail='element action is not supported')
        await element.close()
        context.current.del_element(element_id)
    except HTTPException:
        raise
    except Exception as error:
        logger.error(str(error))
        raise HTTPException(status_code=500, detail=str(error))


@router.post(PREFIX + "/pages")
async def new_page(request: Request):
    try:
        browser_id = request.path_params.get('browser_id')
        context_id = request.path_params.get('context_id')
        browser = browser_get(browser_id)
        context = browser.get_context(context_id)

        request_body = await request.json()
        url = request_body.get('url')

        context_p = context.context
        page_p = await context_p.new_page()
        await page_p.goto(url)
        page = PageWrapper(page_p, browser_id, context_id)
        context.append_page(page)

        return context.as_json()
    except HTTPException:
        raise
    except Exception as error:
        logger.error(str(error))
        raise HTTPException(status_code=500, detail=str(error))


@router.delete(PREFIX + "/pages/{page_id}")
async def del_pages(request: Request):
    try:
        browser_id = request.path_params.get('browser_id')
        context_id = request.path_params.get('context_id')
        page_id = request.path_params.get('page_id')

        browser = browser_get(browser_id)
        context = browser.get_context(context_id)
        if len(context.pages) == 1:
            raise HTTPException(status_code=500, detail='only one page is not supported')
        page = context.get_page(page_id)
        await page.close()
        context.remove_page(page)
        await context.current.page.bring_to_front()
        return context.as_json()
    except HTTPException:
        raise
    except Exception as error:
        logger.error(str(error))
        raise HTTPException(status_code=500, detail=str(error))


@router.post(PREFIX + "/pages/go_back")
async def go_back(request: Request):
    try:
        browser_id = request.path_params.get('browser_id')
        context_id = request.path_params.get('context_id')

        browser = browser_get(browser_id)
        context = browser.get_context(context_id)
        await context.current.page.go_back()
    except HTTPException:
        raise
    except Exception as error:
        logger.error(str(error))
        raise HTTPException(status_code=500, detail=str(error))


@router.post(PREFIX + "/pages/go_forward")
async def go_forward(request: Request):
    try:
        browser_id = request.path_params.get('browser_id')
        context_id = request.path_params.get('context_id')

        browser = browser_get(browser_id)
        context = browser.get_context(context_id)
        await context.current.page.go_forward()
    except HTTPException:
        raise
    except Exception as error:
        logger.error(str(error))
        raise HTTPException(status_code=500, detail=str(error))


@router.post(PREFIX + "/pages/find_element")
async def find_element(request: Request):
    try:
        browser_id = request.path_params.get('browser_id')
        context_id = request.path_params.get('context_id')
        request_body = await request.json()
        selector = request_body.get('selector')

        browser = browser_get(browser_id)
        context = browser.get_context(context_id)
        page = context.current.page
        element_handle = await page.query_selector(selector)
        ele_result = await handle_element_node(element_handle, context.current)
        return {
            'result_type': 'element',
            'value': json.dumps(ele_result)
        }
    except HTTPException:
        raise
    except Exception as error:
        logger.error(str(error))
        raise HTTPException(status_code=500, detail=str(error))


@router.post(PREFIX + "/pages/element/{element_id}/get_size")
async def pages_get_size(request: Request):
    try:
        browser_id = request.path_params.get('browser_id')
        context_id = request.path_params.get('context_id')
        element_id = request.path_params.get('element_id')

        browser = browser_get(browser_id)
        context = browser.get_context(context_id)
        element = context.current.get_element(element_id)

        ele_info = await element.element.bounding_box()
        if ele_info is None:
            raise HTTPException(status_code=500, detail='element is not visible')
        width = ele_info['width']
        height = ele_info['height']
        await element.close()
        context.current.del_element(element_id)
        return {
            "width": width,
            "height": height
        }
    except HTTPException:
        raise
    except Exception as error:
        logger.error(str(error))
        raise HTTPException(status_code=500, detail=str(error))


@router.post(PREFIX + "/pages/screenshot")
async def screenshot(request: Request):
    try:
        browser_id = request.path_params.get('browser_id')
        context_id = request.path_params.get('context_id')
        browser = browser_get(browser_id)
        context = browser.get_context(context_id)
        body = {}
        try:
            body = await request.json()
        except Exception:
            pass
        full_page = body.get("full_page", False)
        page = context.current
        if page is None:
            raise HTTPException(status_code=400, detail="No current page")
        png_bytes = await page.page.screenshot(full_page=full_page)
        return base64.b64encode(png_bytes).decode("utf-8")
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@router.post(PREFIX + "/pages/scroll", status_code=204)
async def scroll(request: Request):
    try:
        browser_id = request.path_params.get('browser_id')
        context_id = request.path_params.get('context_id')
        browser = browser_get(browser_id)
        context = browser.get_context(context_id)
        body = await request.json()
        x = body["x"]
        y = body["y"]
        delta_x = body["delta_x"]
        delta_y = body["delta_y"]
        page = context.current
        if page is None:
            raise HTTPException(status_code=400, detail="No current page")
        await page.page.mouse.wheel(delta_x, delta_y)
        return Response(status_code=204)
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@router.post(PREFIX + "/pages/tap", status_code=204)
async def tap(request: Request):
    try:
        browser_id = request.path_params.get('browser_id')
        context_id = request.path_params.get('context_id')
        browser = browser_get(browser_id)
        context = browser.get_context(context_id)
        body = await request.json()
        x = body["x"]
        y = body["y"]
        page = context.current
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
async def swipe(request: Request):
    try:
        browser_id = request.path_params.get('browser_id')
        context_id = request.path_params.get('context_id')
        browser = browser_get(browser_id)
        context = browser.get_context(context_id)
        body = await request.json()
        start_x = body["start_x"]
        start_y = body["start_y"]
        end_x = body["end_x"]
        end_y = body["end_y"]
        duration_ms = body.get("duration_ms", 300)
        page = context.current
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
async def touch_scroll(request: Request):
    try:
        browser_id = request.path_params.get('browser_id')
        context_id = request.path_params.get('context_id')
        browser = browser_get(browser_id)
        context = browser.get_context(context_id)
        body = await request.json()
        x = body["x"]
        y = body["y"]
        delta_x = body["delta_x"]
        delta_y = body["delta_y"]
        page = context.current
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


@router.get(PREFIX + "/pages/cookies")
async def get_cookies(request: Request):
    try:
        browser_id = request.path_params.get('browser_id')
        context_id = request.path_params.get('context_id')
        browser = browser_get(browser_id)
        context = browser.get_context(context_id)
        cookies = await context.context.cookies()
        return cookies
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@router.post(PREFIX + "/pages/cookies", status_code=204)
async def set_cookies(request: Request):
    try:
        browser_id = request.path_params.get('browser_id')
        context_id = request.path_params.get('context_id')
        browser = browser_get(browser_id)
        context = browser.get_context(context_id)
        cookies = await request.json()
        await context.context.add_cookies(cookies)
        return Response(status_code=204)
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@router.delete(PREFIX + "/pages/cookies", status_code=204)
async def clear_cookies(request: Request):
    try:
        browser_id = request.path_params.get('browser_id')
        context_id = request.path_params.get('context_id')
        browser = browser_get(browser_id)
        context = browser.get_context(context_id)
        await context.context.clear_cookies()
        return Response(status_code=204)
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
