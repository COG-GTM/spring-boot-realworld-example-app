# Client contract — conventions the frontends depend on

This backend is a [RealWorld](https://github.com/gothinkster/realworld) implementation: it is
consumed by **frontends we do not control** (Angular, React, Vue, Svelte…), all of which are written
against one shared API spec. Treat this file as the contract; everything else in `devin_context/` is
an implementation detail that can change.

**Reference frontend** (the client we test against):
[`gothinkster/angular-realworld-example-app`](https://github.com/gothinkster/angular-realworld-example-app)
— when in doubt about a payload shape, check how that client parses it before changing anything.
**Spec of record:** https://realworld-docs.netlify.app/specifications/backend/endpoints/

> Rule of thumb: a change is a breaking change if a frontend would need a code change to keep
> working. Breaking changes need an explicit call-out in the PR description — the frontend does not
> ship on our schedule.

## 1. Base URL

`http://localhost:8080` — **no `/api` prefix**, unlike some RealWorld docs. Never introduce one.

## 2. Root-wrapped payloads

Requests and responses always wrap the resource in a root key. The frontend reads `res.article`,
`res.comments`, `res.user`.

```jsonc
// POST /articles/{slug}/comments   request
{ "comment": { "body": "great post" } }
// 201 response
{ "comment": { "id": "…", "body": "great post", "createdAt": "…", "updatedAt": "…", "author": {…} } }
```

Root keys in use: `user`, `profile`, `article`, `articles` + `articlesCount`, `comment`, `comments`,
`tags`. Collections are wrapped too — never return a bare JSON array.

## 3. Authentication header

`Authorization: Token <jwt>` — the scheme is the literal word `Token`, **not** `Bearer`. Every
RealWorld frontend hard-codes this. `POST /users` and `POST /users/login` return the JWT inside the
`user` object as `user.token`; the frontend stores that and replays it.

## 4. Errors: `422` + `errors` map

The frontend renders a validation error list from exactly this shape:

```json
{ "errors": { "body": ["can't be empty"], "email": ["should be an email"] } }
```

- Keys are **field names**, values are **arrays of strings** (`ErrorResourceSerializer`).
- Validation failures are `422 Unprocessable Entity`, not `400`.
- `401` (no/invalid token) and `403` (authenticated but not the owner) have empty bodies. Do not
  invent a global `{"error": ...}` envelope — it would be silently ignored by the clients.

## 5. Field naming & types

- JSON is `camelCase` (`createdAt`, `favoritesCount`, `tagList`, `following`).
- Timestamps are ISO-8601 with milliseconds in UTC — `2021-01-01T12:00:00.000Z` — enforced globally
  by `JacksonCustomizations.DateTimeSerializer`. Never format a date in a controller or DTO.
- Booleans the frontend toggles on (`favorited`, `following`) are user-relative and must be `false`
  (never `null`, never omitted) for anonymous requests.
- Counters (`favoritesCount`, `articlesCount`) are numbers, always present.
- Articles are addressed by `slug`, comments by `id`; ids are UUID strings. Slugs are derived from
  the title, so a title change changes the URL — return the updated article so the client can
  re-route (`ArticleApi.updateArticle`).
- `author` is a profile object (`ProfileData` serialized as `author` via `@JsonProperty("author")`),
  never a bare username string.

## 6. Anonymous access

`GET /articles/**`, `GET /profiles/**` and `GET /tags` must keep working without a token — the
frontend renders the home feed before login. Personalised flags degrade to `false`.

## 7. CORS

`WebSecurityConfig.corsConfigurationSource()` allows any origin, no credentials, and the headers
`Authorization`, `Cache-Control`, `Content-Type`. Frontends run on a different port in dev; if you
add a header that clients must send, add it there too or preflight will 403.

## 8. Checklist before changing anything client-facing

- [ ] Root key unchanged, or the change is deliberate and documented.
- [ ] Status codes match the table in `02-rest-api-conventions.md`.
- [ ] New fields are additive; nothing renamed or removed without a call-out.
- [ ] Dates go through the global serializer.
- [ ] Anonymous path still works and personalised flags default to `false`.
- [ ] Endpoint behaviour is covered by a `@WebMvcTest` that asserts the JSON shape, not just the status.
