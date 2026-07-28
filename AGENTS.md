# AGENTS.md

Read [`devin_context/`](devin_context/README.md) before writing code in this repo, and follow it.

- Start with [`devin_context/README.md`](devin_context/README.md) (index + non-negotiables).
- Anything client-facing is governed by
  [`devin_context/03-client-contract.md`](devin_context/03-client-contract.md) — this backend serves
  RealWorld frontends we do not own, so payload shapes, status codes, the `Authorization: Token`
  scheme and date formats are hard constraints.
- Before opening a PR: `./gradlew spotlessJavaApply && ./gradlew test`, and work through the
  checklist in
  [`devin_context/reference/new-endpoint-walkthrough.md`](devin_context/reference/new-endpoint-walkthrough.md).
