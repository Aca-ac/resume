# AI Resume and Job Search Assistant

[![CI](https://github.com/Aca-ac/resume/actions/workflows/ci.yml/badge.svg)](https://github.com/Aca-ac/resume/actions/workflows/ci.yml)

前后端分离的 AI 简历与求职助手：简历管理、JD 匹配、模拟面试。

## Stack

- **Backend**: Spring Boot 3.2, Java 17, MyBatis-Plus, Flyway, MySQL 8, Redis 7, JWT, PDFBox, SpringDoc
- **Frontend**: Vue 3, TypeScript, Vite, Element Plus, Pinia
- **AI**: OpenAI / 通义千问（可配置）

## Quick start

直接运行start.cmd即可
```

- API: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui.html
- Web: http://localhost:5173 (proxies `/api` to backend)

## Environment

Copy `.env.example` to `.env` and adjust DB, Redis, JWT, and AI keys.

For Chinese PDF export, place `NotoSansSC-Regular.otf` under `server/src/main/resources/fonts/` (or use system fonts on Windows).

## API overview

| Area | Endpoints |
|------|-----------|
| Auth | POST `/api/v1/auth/register`, `/login`, `/refresh` |
| Resume | CRUD `/api/v1/resumes?page=&size=`, POST `/{id}/optimize`, GET `/{id}/export/pdf` |
| Match | POST `/api/v1/match/jd`, GET `/api/v1/match/history?page=&size=` |
| Interview | POST `/api/v1/interview/start`, `/{sessionId}/answer`, SSE `/{sessionId}/stream`, GET `/{sessionId}/report` |

## Tests

```bash
cd server && mvn test
```

## CI (GitHub Actions)

Push or PR to `master` triggers `.github/workflows/ci.yml`:

| Job | What it does |
|-----|--------------|
| **Backend** | JDK 17 + Maven verify with MySQL 8 & Redis 7 services |
| **Frontend** | `npm ci` → typecheck (`vue-tsc`) → `vite build` |
| **Docker check** | Build both images (backend + frontend) to validate Dockerfiles |
