import shutil
import uuid
from enum import Enum
from typing import Dict, List, Optional
from .logger_config import logger

from fastapi import HTTPException
from playwright.async_api import Browser, BrowserContext, CDPSession, Page, ElementHandle, Playwright


class BrowserType(Enum):
    KEYS = 'KEYS'
    TOUCH = 'TOUCH'

class ElementWrapper:
    def __init__(self, element: ElementHandle):
        self._element = element
        self._id = uuid.uuid4().hex

    @property
    def element(self):
        return self._element

    @property
    def id(self):
        return self._id

    @property
    async def preview(self):
        await self.element.inner_html()
        return self.element.__str__().replace('JSHandle@', '')

    async def close(self):
        await self._element.dispose()

    async def as_json(self):
        return {
            'id': self.id,
            'preview': await self.preview,
        }

class PageWrapper:
    def __init__(self, page: Page, browser_id: str, context_id: str, cdp_session: CDPSession = None):
        self._page = page
        self._id = uuid.uuid4().hex
        self._browser_id = browser_id
        self._context_id = context_id
        self._cdp_session: CDPSession = cdp_session
        self._elements:Dict[str, ElementWrapper] = {}

    @property
    def id(self) -> str:
        return self._id

    @property
    def page(self) -> Page:
        return self._page

    @property
    def browser_id(self) -> str:
        return self._browser_id

    @property
    def context_id(self) -> str:
        return self._context_id

    @property
    def cdp_session(self) -> CDPSession:
        if self._cdp_session is None:
            raise HTTPException(status_code=500, detail=f"current page is not support cdp session: {self._id}")
        return self._cdp_session

    @property
    def url(self) -> str:
        return self._page.url

    def as_json(self):
        return {
            'id': self.id,
            'url': self.url,
            'browser_id': self.browser_id,
            'context_id': self.context_id,
            'support_cdp_session': self._cdp_session is not None
        }

    def set_element(self, element: ElementWrapper):
        self._elements[element.id] = element

    def get_element(self, key:str):
        element = self._elements[key]
        if element is None:
            raise HTTPException(status_code=404)
        return element

    def del_element(self, key:str):
        element = self._elements[key]
        if element is not None:
            del self._elements[key]


    async def close(self):
        try:
            for val in self._elements.values():
                await val.close()
        except Exception as e:
            logger.error(str(e))
        await self._page.close()


class ContextWrapper:
    def __init__(self, browser_id: str, context: BrowserContext, userdata: str):
        self._browser_id = browser_id
        self._context = context
        self._id = uuid.uuid4().hex
        self._pages: List[PageWrapper] = []
        self._current = None
        self._userdata = userdata

    @property
    def id(self) -> str:
        return self._id

    @property
    def context(self) -> BrowserContext:
        return self._context

    @property
    def pages(self) -> List[PageWrapper]:
        return self._pages

    def append_page(self, page: PageWrapper):
        self._current = page
        self._pages.append(page)

    @property
    def current(self) -> PageWrapper:
        return self._current

    async def close(self):
        await self._context.storage_state(path=self._userdata, indexed_db=False)
        return await self._context.close()

    def as_json(self):
        return {
            'id': self.id,
            'current': self.current.id if self.current else None,
            'browser_id': self._browser_id,
            'pages': [page.as_json() for page in self._pages]
        }

    def get_page(self, page_id: str) -> PageWrapper:
        for page in self.pages:
            if page.id == page_id:
                return page
        raise HTTPException(status_code=404)

    def remove_page(self, page: PageWrapper):
        self._pages.remove(page)
        if self._current.id != page.id:
            return
        self._current = self._pages[-1]


class BrowserWrapper:
    def __init__(self, browser: Browser, browser_type: BrowserType, browser_id: str, userdata: str, playwright: Playwright):
        self._browser = browser
        self._browser_type = browser_type
        self._id = browser_id
        if self._id == '':
            self._id = str(uuid.uuid4().hex)
        self._contexts = []
        self._userdata = userdata
        self._playwright = playwright

    @property
    def used(self) -> int:
        return len(self._contexts)

    @property
    def id(self) -> str:
        return self._id

    @property
    def browser_type(self) -> BrowserType:
        return self._browser_type

    @property
    def browser(self) -> Browser:
        return self._browser

    @property
    def userdata(self) -> str:
        return self._userdata

    async def close(self) -> None:
        await self.browser.close()
        await self._playwright.stop();
        try:
            shutil.rmtree(self.userdata)
        except FileNotFoundError:
            pass
        except PermissionError:
            logger.error('delete userdata:{}, permission denied'.format(self.userdata))
        except Exception as e:
            logger.error('delete userdata:{}, error occurred, details:{}'.format(self.userdata, str(e)))

    def as_json(self):
        return {
            'id': self.id,
            'used': self.used,
            'browser_type': self.browser_type.value,
        }

    def append_context(self, context: ContextWrapper):
        self._contexts.append(context)

    @property
    def contexts(self) -> List[ContextWrapper]:
        return self._contexts

    def get_context(self, context_id: str) -> ContextWrapper:
        for context in self._contexts:
            if context.id == context_id:
                return context
        raise HTTPException(status_code=404)

    def remove_context(self, context: ContextWrapper):
        self._contexts.remove(context)

    async def is_active(self) -> bool:
        try:
            if not self.browser.is_connected():
                return False
            version = self.browser.version
            return True
        except Exception as error:
            log.error(str(error))
            return False


browser_list: List[BrowserWrapper] = []


def browser_get(browser_id) -> BrowserWrapper:
    for browser in browser_list:
        if browser.id == browser_id:
            return browser
    raise HTTPException(status_code=404)