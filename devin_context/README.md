# devin_context/

Machine-readable conventions for this codebase. Devin (and humans) read these **before** writing
code, so that generated changes look like they were written by this team.

## Read order

| File | Use it when |
| --- | --- |
| [`01-architecture.md`](01-architecture.md) | Deciding *where* code goes (layer + package). |
| [`02-rest-api-conventions.md`](02-rest-api-conventions.md) | Adding or changing an HTTP endpoint. |
| [`03-client-contract.md`](03-client-contract.md) | Anything a RealWorld frontend consumes: payload shape, errors, auth, dates, CORS. |
| [`04-graphql-conventions.md`](04-graphql-conventions.md) | Touching the DGS schema, data fetchers or mutations. |
| [`05-persistence-conventions.md`](05-persistence-conventions.md) | Writing MyBatis mappers, SQL, migrations. |
| [`06-testing-and-tooling.md`](06-testing-and-tooling.md) | Writing tests; before opening a PR. |
| [`reference/new-endpoint-walkthrough.md`](reference/new-endpoint-walkthrough.md) | The golden path: one feature, every layer, file by file. |

## Rules of engagement

1. **Conventions beat preference.** If a rule here conflicts with your default style, follow the rule.
2. **Conventions do not beat the RealWorld spec.** The public contract in
   [`03-client-contract.md`](03-client-contract.md) is what every RealWorld frontend depends on;
   never change response shapes to make an implementation more convenient.
3. **Copy the nearest neighbour.** Every rule here points at an existing file that already does it.
   When in doubt, mirror that file rather than inventing a pattern.
4. **If something here is wrong or missing, fix it in the same PR.** These docs are code.

## Non-negotiables (the short version)

- Layer dependencies point inward only: `api`/`graphql` → `application` → `core` ← `infrastructure`.
- Write goes through `core` repositories; reads go through `application` query services. Never mix.
- Every REST body is root-wrapped (`{"article": {...}}`), errors are `422` with
  `{"errors": {"field": ["message"]}}`.
- Auth is `Authorization: Token <jwt>`, resolved via `@AuthenticationPrincipal User user`.
- No new endpoint without a `@WebMvcTest` slice test covering success + auth failure + validation.
- `./gradlew spotlessJavaApply` before every commit; `./gradlew test` before every PR.

## Demo

`demo/RUNBOOK.md` contains the prompts used to show "give Devin your conventions, get conventional
code back". It is documentation, not part of the build.
