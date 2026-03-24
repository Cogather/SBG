# Project Instructions

## General Rules
1. Python项目用虚拟环境 (Python projects must use a virtual environment)

## Repository Structure

```
SBG/
├── BrowserGateway/BrowserGateway/
│   ├── browser-gateway/   # Java Spring Boot service (main gateway)
│   └── browser-proxy/     # Python FastAPI proxy service
├── mobile/                # Java Spring Boot cloud-phone control service
├── Test/browsergateway-test-client/  # Python test client & mock servers
├── openspec/              # OpenSpec change definitions
└── doc/                   # Documentation
```

## Module: browser-gateway (Java)
- **Stack:** Java 17, Spring Boot, Maven (`pom.xml`)
- **Run:** `mvn spring-boot:run` from `BrowserGateway/BrowserGateway/browser-gateway/`
- **Package root:** `com.huawei.browsergateway`
- **Key packages:** `adapter`, `api`, `config`, `entity`, `scheduled`, `sdk`, `service`, `tcpserver`, `websocket`
- **Adapter pattern:** interface + CspAdapter + CustomAdapter + AdapterFactory + AdapterConfig
- **Scheduled tasks:** use `ScheduledExecutorService` + `@PostConstruct`/`@PreDestroy` + `browsergw.scheduled` YAML keys (not Spring `@Scheduled`)

## Module: browser-proxy (Python)
- **Stack:** Python, FastAPI
- **Virtual env required** — create with `python -m venv .venv` in `BrowserGateway/BrowserGateway/browser-proxy/`
- **Entry:** `browser_proxy/` package, `pyproject.toml` / `setup.py`
- **Routes:** REST/CRUD under `/api` prefix, consistent with existing browser/context/page routers

## Module: mobile (Java)
- **Stack:** Java 17 (runtime Java 25), Spring Boot 2.7.18, Maven
- **Port:** 8088 (HTTP), 40002 (WebSocket)
- **WebSocket:** `ws://localhost:40002/app/websocket/{IMEI}_{IMSI}` (org.yeauty)
- **Protocol:** Custom TLV binary — magic `"mu"` (2B) + count (4B) + dataLen (4B) + TLV entries
- **GIDS default:** `http://127.0.0.1:9090`
- **Key files:** `BrowserContext.java`, `ControlChannelHandler.java`, `MediaChannelHandler.java`, `WebsocketServer.java`
- **TLV Types:** LOGIN=1, HEARTBEATS=2, CONTROL=4, AUDIO=5, VIDEO=6, ACK=7, RETURN_MEDIA=9, RETURN_CONTROL=12, MESSAGE=13, UPLOAD_FILE=16
- **WS binary push format:** Video: `[0x01, frameType] + H.264 bytes`; Audio: `[0x02] + MP3 bytes`

## Module: Test Client (Python)
- **Location:** `Test/browsergateway-test-client/`
- **Virtual env required**
- **Contains:** mock servers (GIDS, browser-proxy), WebSocket client, end-to-end tests

## Coding Conventions
- Do not use Spring `@Scheduled` in browser-gateway — use the `ScheduledExecutorService` pattern
- New HTTP mock routes must mirror method, path, fields, and status codes of the real service
- Java: avoid backwards-compatibility shims; delete unused code rather than commenting it out
- Python: follow existing FastAPI router structure; keep routes under `/api` prefix