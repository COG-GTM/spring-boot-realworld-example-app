---
name: testing-realworld-api
description: Test the RealWorld Conduit Spring Boot backend end-to-end. Use when verifying REST API, GraphQL, JWT auth, or date serialization changes.
---

# Testing the RealWorld Spring Boot Backend

## Quick Start

```bash
cd /home/ubuntu/repos/spring-boot-realworld-example-app
rm -f dev.db                    # fresh SQLite DB
./gradlew bootRun &             # starts on port 8080
sleep 15                        # wait for startup
curl -s http://localhost:8080/articles  # verify it's up
```

## Key Details

- **Port**: 8080 (default, no override in application.properties)
- **Database**: SQLite file `dev.db` in project root. Delete for clean state.
- **Auth**: JWT tokens via `Authorization: Token <jwt>` header
- **JSON wrapping**: `spring.jackson.deserialization.UNWRAP_ROOT_VALUE=true` — request bodies use root wrapper (e.g., `{"user": {...}}`)
- **Date format**: ISO-8601 via `spring.jackson.serialization.WRITE_DATES_AS_TIMESTAMPS=false` + `JavaTimeModule`
- **No external services needed**: Everything runs locally with SQLite

## API Testing Patterns

### Register + Login
```bash
# Register
curl -s -X POST http://localhost:8080/users \
  -H 'Content-Type: application/json' \
  -d '{"user": {"username":"testuser","email":"test@test.com","password":"pass123"}}'
# Returns: {"user": {"token": "eyJ...", "username": "testuser", ...}}

# Login
curl -s -X POST http://localhost:8080/users/login \
  -H 'Content-Type: application/json' \
  -d '{"user": {"email":"test@test.com","password":"pass123"}}'
```

### Article CRUD (requires auth)
```bash
TOKEN="eyJ..."  # from register or login response

# Create
curl -s -X POST http://localhost:8080/articles \
  -H 'Content-Type: application/json' \
  -H "Authorization: Token $TOKEN" \
  -d '{"article": {"title":"My Article","description":"d","body":"b","tagList":["test"]}}'

# Read (public)
curl -s http://localhost:8080/articles/my-article

# Update
curl -s -X PUT http://localhost:8080/articles/my-article \
  -H 'Content-Type: application/json' \
  -H "Authorization: Token $TOKEN" \
  -d '{"article": {"title":"New Title"}}'

# Delete
curl -s -X DELETE http://localhost:8080/articles/new-title \
  -H "Authorization: Token $TOKEN"
```

### GraphQL
```bash
curl -s -X POST http://localhost:8080/graphql \
  -H 'Content-Type: application/json' \
  -d '{"query": "{ articles(first: 10) { edges { node { title createdAt updatedAt } } } }"}'
```

## Security Endpoint Rules

| Endpoint | Auth Required |
|----------|---------------|
| `GET /articles/**` | No |
| `GET /profiles/**` | No |
| `GET /tags` | No |
| `POST /users` | No |
| `POST /users/login` | No |
| `/graphql`, `/graphiql` (any method) | No |
| `GET /articles/feed` | **Yes** |
| `POST/PUT/DELETE /articles/**` | **Yes** |
| Everything else | **Yes** |

## Running Unit Tests
```bash
./gradlew test    # 68 tests, takes ~15s
```

## Common Issues

- **Port 8080 already in use**: Kill existing process with `pkill -f bootRun` or `lsof -i :8080 | awk 'NR>1{print $2}' | xargs kill -9`
- **Stale DB state**: Delete `dev.db` and restart for clean state
- **JJWT key size**: The app uses JJWT 0.12.6 with HS512. The secret from `jwt.secret` in application.properties is used as raw bytes (`secret.getBytes()`) to construct the signing key — it is NOT hashed. The configured secret must be at least 64 bytes for HS512.

## Devin Secrets Needed
None — the app runs entirely locally with SQLite and no external services.
