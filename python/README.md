# Conduit — Python / FastAPI port

A Python re-implementation of the Spring Boot RealWorld ("Conduit") example app, built to be
**byte-for-byte compatible at the HTTP boundary** with the original Java service. It keeps the same
SQLite schema, the same JSON/error/auth contracts, and the same four-layer (DDD) architecture, with
the Java test suite re-expressed as pytest.

The original Java sources are untouched under `src/`; this port lives entirely in `python/`.

## Architecture (parity with the Java layering)

```
conduit/
├── api/              # FastAPI routers, request unwrapping, exception handlers, JWT auth deps
├── application/      # DTOs (data), params, pagination, CQRS query services + command services
├── core/             # domain entities (User, Article, Comment, ...), repository protocols, services
└── infrastructure/   # SQLAlchemy repositories (writes) + read services (reads), JWT, password, schema
```

* **CQRS split** is preserved: `infrastructure/repositories.py` holds the write side (mirrors
  `*Mapper.xml`), `infrastructure/read_services.py` holds the read side (mirrors `*ReadService.xml`).
* Requests are **root-wrapped** (`{"user": {...}}`, `{"article": {...}}`, `{"comment": {...}}`) and
  unwrapped in `api/body.py` (equivalent to Jackson `UNWRAP_ROOT_VALUE=true`).
* Responses are wrapped under a single root key (`user`, `profile`, `article`, `articles`,
  `comment`, `comments`, `tags`) with exact camelCase field names (`favoritesCount`, `createdAt`,
  `updatedAt`, `tagList`) and the `author` remap for the article/comment profile.

## Setup

```bash
cd python
python3 -m venv .venv
source .venv/bin/activate
pip install -e ".[test]"
```

## Run

```bash
# apply the schema to SQLite dev.db (Alembic == Flyway equivalent)
alembic upgrade head

# start on port 8080 with root paths (NOT under /api)
uvicorn conduit.main:app --host 0.0.0.0 --port 8080
```

## Test

```bash
pytest            # 71 tests: api/ (mocked services), application/ + infrastructure/ (real SQLite)
```

## Endpoints

| Method | Path | Auth | Status |
|--------|------|------|--------|
| POST | `/users` | public | 201 |
| POST | `/users/login` | public | 200 (422 on bad creds) |
| GET/PUT | `/user` | required | 200 |
| GET | `/profiles/{username}` | optional | 200 / 404 |
| POST/DELETE | `/profiles/{username}/follow` | required | 200 |
| POST | `/articles` | required | 200 |
| GET | `/articles/feed` | required | 200 |
| GET | `/articles` (`offset`,`limit`,`tag`,`favorited`,`author`) | optional | 200 |
| GET/PUT/DELETE | `/articles/{slug}` | GET optional; PUT/DELETE author-only | 200 / 204 / 403 / 404 |
| POST/DELETE | `/articles/{slug}/favorite` | required | 200 |
| POST | `/articles/{slug}/comments` | required | 201 |
| GET | `/articles/{slug}/comments` | optional | 200 |
| DELETE | `/articles/{slug}/comments/{id}` | author-only | 204 / 403 / 404 |
| GET | `/tags` | public | 200 |

## Contracts

* **Validation errors** → HTTP 422 with `{"errors": {"<field>": ["message", ...]}}` (see
  `api/validation.py` + `api/exceptions.py`). Bad login credentials → 422 `{"message": "invalid email or password"}`
  (matches `InvalidAuthenticationException` → `UNPROCESSABLE_ENTITY` in the Java handler and
  `UsersApiTest.should_fail_login_with_wrong_password`, which asserts 422).
* **Auth** → `Authorization: Token <jwt>` (NOT `Bearer`); parsed by splitting on space, index `[1]`.
  Stateless JWT, HS512, `sub`/`exp` claims. Missing/invalid token on a protected route → 401.
* **Not found** → 404; **unauthorized mutation** → 403.

## Config

Configured in `conduit/config.py` (overridable via env vars), matching `application.properties`:

| Setting | Env var | Default |
|---------|---------|---------|
| Datasource | `DATASOURCE_URL` | `sqlite:///dev.db` (also accepts `jdbc:sqlite:...`) |
| JWT secret | `JWT_SECRET` | (same value as the Java `jwt.secret`) |
| JWT session time | `JWT_SESSION_TIME` | `86400` |
| Default avatar | `IMAGE_DEFAULT` | `https://static.productionready.io/images/smiley-cyrus.jpg` |

Passwords are hashed with BCrypt (`passlib[bcrypt]`).

## Out of scope / follow-up

* **GraphQL (DGS)** is intentionally not ported — the original REST contract is fully covered here.
  Porting the GraphQL layer (e.g. via Strawberry/Ariadne over the same application services) is a
  possible follow-up.
