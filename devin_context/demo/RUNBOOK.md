# Demo runbook — "here are our conventions, follow them"

**Story:** a team's conventions live in the repo (`devin_context/`, design-system docs, a reference
repo). You point Devin at them in one sentence; the output looks like your codebase, not like
generic Spring Boot from a model's training set.

**Length:** 10–12 minutes live (or pre-run the session and review the PR — see *Pre-baked mode*).
**Repo:** `COG-GTM/spring-boot-realworld-example-app`.

---

## 0. Setup (before the call)

- Have `devin_context/README.md` and `devin_context/03-client-contract.md` open in a browser tab.
- Optional but strong: pre-run **Prompt B** so a finished PR is ready to review if the live session
  runs long.

---

## 1. Show the context pack (2 min)

Open `devin_context/` and make three points:

1. **It's in the repo, not in a wiki.** Versioned, reviewed, diffable — the same PR that changes a
   convention updates the doc.
2. **It's layered.** Architecture → REST conventions → the *client contract* → persistence → tests.
   `03-client-contract.md` is the interesting one: this backend serves frontends the team doesn't
   own (the reference client is `gothinkster/angular-realworld-example-app`), so the payload shape,
   the `Authorization: Token <jwt>` scheme, the `422 {"errors": {...}}` format and the ISO-8601
   dates are hard constraints. That doc *is* "follow our frontend conventions".
3. **Every rule points at a file that already does it** — the conventions are extracted from the
   codebase, so Devin can verify them instead of trusting prose.

Also show `AGENTS.md` at the repo root: three lines that make Devin pick this up automatically on
every session, without anyone pasting it into the prompt.

---

## 2. The prompt (30 sec)

Paste this into a fresh Devin session on the repo:

> **Prompt B — with conventions**
>
> Our frontend team needs comment editing. Add `PUT /articles/{slug}/comments/{id}` to the RealWorld
> API. Read `devin_context/` first and follow it exactly — especially the client contract in
> `devin_context/03-client-contract.md` and the endpoint checklist in
> `devin_context/reference/new-endpoint-walkthrough.md`. In the PR description, list each convention
> you followed and where it is defined.

That is the whole demo: one sentence of task, one sentence of "follow our conventions".

---

## 3. Narrate while it works (5–7 min)

Point out that Devin reads the context pack *before* touching code, then watch for these tells in
the diff — each one is a decision a context-free model gets wrong:

| Convention | What a generic implementation does instead |
| --- | --- |
| Response is root-wrapped `{"comment": {...}}` via the controller's private `commentResponse(...)` helper | returns the bare DTO |
| Validation failure is `422` with `{"errors": {"body": ["can't be empty"]}}` | `400` with Spring's default error body |
| `@AuthenticationPrincipal User user` + `AuthorizationService.canWriteComment` → `403` | reads the `Authorization` header manually, inlines `equals` checks |
| Body mutated by `Comment.update(...)` on the entity, persisted via `CommentRepository.save` | adds a setter, or calls the MyBatis mapper straight from the controller |
| Response re-read through `CommentQueryService` so the client gets `CommentData` with `author` | serializes the `core` entity, leaking `userId` and dropping `author` |
| New SQL in `resources/mapper/CommentMapper.xml` with `#{}` params | `@Update("...")` annotation with string concatenation |
| `@WebMvcTest` slice test asserting `comment.body`, plus 403/422 cases | one happy-path test, or none |
| GraphQL mutation added for parity | REST only, adapters silently drift |

---

## 4. Land the point (2 min)

Open the PR. The description enumerates the conventions followed, with file references — that is the
reviewable artifact. Then say the quiet part:

- The conventions are **inputs**, not a code review afterthought. Nobody had to leave the "wrap the
  response in a root key" comment on this PR.
- This scales: the same pack drives every session on this repo, and new rules are added the moment a
  review comment repeats itself.
- Swap `devin_context/` for a design-system doc, a Figma spec, or a reference repo and the mechanic
  is identical — for a frontend team it's "use our `<Button>` primitives, our tokens, our test
  conventions" instead of "root-wrap the payload".

---

## Optional beats

**A. The control (adds ~5 min, very persuasive).** Run the same task in a second session *without*
the context pack:

> **Prompt A — no conventions**
>
> Add an endpoint to edit a comment on an article in this Spring Boot API.

Diff the two PRs side by side. Prompt A produces working, idiomatic-Spring code that is wrong for
*this* codebase (bare DTO, `400`s, entity in the response, no GraphQL parity). Same model, same
repo — the delta is the context.

**B. Conventions as a living artifact.** Add a rule to `devin_context/02-rest-api-conventions.md`
mid-demo ("all list endpoints must support `?limit=&offset=`"), then ask Devin for a list endpoint
and watch the new rule show up in the output.

**C. Contract enforcement.** Ask: *"Would this change break the Angular RealWorld frontend?"* Devin
answers against `03-client-contract.md` and the reference client rather than guessing.

---

## Pre-baked mode

If you want zero live risk: run Prompt B beforehand, then in the meeting show `devin_context/`, read
the prompt out loud, and review the resulting PR. The talk track above works unchanged.

A pre-run of Prompt B already exists — **PR #866**, `PUT /articles/{slug}/comments/{id}` — and can be
used as the review artifact as-is. Its description enumerates the conventions followed, and the diff
hits every row of the table in step 3 (root-wrapped response, `422` error map, `AuthorizationService`
check, entity-owned `Comment.update`, re-read through `CommentQueryService`, XML mapper SQL,
GraphQL parity, five API tests).
