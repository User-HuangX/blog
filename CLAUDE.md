# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**my_blog** ("Earth乛的个人博客") is a full-stack blog application with a Spring Boot backend and Vue 3 frontend, backed by PostgreSQL. Supports blog post CRUD, draft/publish workflow, rich-text editing, and image uploads. Author operations are protected by password-based session authentication.

## Architecture

```
Browser (port 80)
  |
  v
nginx -- serves Vue SPA, proxies /api/ and /uploads/ to Spring Boot
  |
  v
Spring Boot (port 19080 local, 8080 in Docker)
  |-- PostController (/api/posts)    -- CRUD, drafts, publish, author verify
  |-- UploadController (/api/uploads) -- image upload & retrieval
  |-- PostService, AuthorSessionService, FileStorageService
  |-- MyBatis-Plus Mappers (PostMeta, PostContent, PostImage)
  |
  v
PostgreSQL (remote host, db=my_blog)
```

### Backend (Spring Boot 4.0.5, Java 21)

- **ORM**: MyBatis-Plus with SqlSessionFactory/SqlSessionTemplate
- **Auth**: Session-based author auth via `AuthorSessionService` with IP + session key rate limiting
- **Images**: Stored as BYTEA in the database (not filesystem)
- **Logging**: `HttpAccessLoggingFilter` logs request/response bodies with UUID requestId in MDC; `OperationLogAspect` AOP logs controller/service method timing
- **Schema migration**: `DatabaseSchemaMigrator` (ApplicationRunner) applies incremental patches, controlled by `DB_AUTO_MIGRATE` env var
- **CORS**: `WebCorsConfig` allows localhost/127.0.0.1/[::1] + configurable extra origins
- **Error handling**: `ApiExceptionHandler` for NotFoundException, validation errors, etc.

### Frontend (Vue 3 + Vite 7 + Tailwind CSS v4)

- **UI**: Radix Vue (dialogs), AG Grid Vue 3 (post list), Lucide icons, shadcn-style components
- **Rich text editor**: `contenteditable` + `document.execCommand` (bold/italic/headings/links/images/tables)
- **Auto-save**: Drafts auto-save after 1.2s debounce
- **Routing**: Hash-based (`#/posts/:id`, `#/write`, `#/edit/:id`) -- single `App.vue` SPA
- **API calls**: `fetch` with `credentials: "include"` for JSESSIONID cookies

### Database Tables

| Table | Key columns |
|---|---|
| `post_meta` | id, title, author_name, is_published, created_at, updated_at |
| `post_content` | post_id (PK/FK), summary, content (TEXT) |
| `post_image` | id, file_name, content_type, size_bytes, data (BYTEA), created_at |

## Key Commands

### Backend (Maven)
```bash
mvn spring-boot:run                          # Run locally
mvn -DskipTests clean package                # Build JAR
```

### Frontend (npm/Vite)
```bash
cd src-frontend && npm install               # Install dependencies
npm run dev                                  # Dev server (port 5173, proxies /api to localhost:19080)
npm run build                                # Production build
```

### Docker
```bash
./build-images.sh                            # Build + deploy to remote host (set DEPLOY_HOST/DEPLOY_USER)
docker compose up -d                         # Run locally
docker compose -f docker-compose.remote.yml up -d  # Run with pre-loaded images
```

### Configuration

Key environment variables:
- `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD` -- PostgreSQL connection (defaults to remote DB)
- `SERVER_PORT` -- Backend port (default 19080, overridden to 8080 in Docker)
- `BLOG_AUTHOR_PASSWORD` -- Author session password
- `DB_AUTO_MIGRATE` -- Enable auto schema migration (default true)
- `CORS_EXTRA_ORIGIN_PATTERNS` -- Additional CORS origins
- `SERVER_SESSION_TIMEOUT` -- Session timeout (default 2h)

### Testing

- No test files currently exist. Test framework is `spring-boot-starter-webmvc-test`.
- Run tests with `mvn test` (skipped in build script via `-DskipTests`).