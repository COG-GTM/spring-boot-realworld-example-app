# Platform upgrade guardrails (apply to every skill in this plugin)

These rules exist so platform work never disrupts developers who have feature branches in flight.

1. **One upgrade per PR.** Never combine two playbooks in a single branch or PR. The next upgrade
   starts only after the previous PR is merged (or, when the orchestrator says so, stacked on it).
2. **Branch naming.** Platform branches are `platform/<NN>-<stage>` (e.g. `platform/03-jdk-17`).
   Preview/rebase branches for developers are `preview/<feature-slug>-on-<stage>`.
3. **Never touch developer branches.** Do not push to, rebase, or force-push any `feature/*`,
   `bugfix/*`, or `hotfix/*` branch. Offer help by pushing a `preview/*` branch and commenting on
   the developer's PR with exact conflict files and the preview branch name.
4. **Never force-push. Never push to the integration branch (`main` / `gs/main`).** All changes land via PR with the pipeline green.
5. **Validate like the pipeline does.** `mvn -B -ntp clean verify` with the JDK declared in
   `.java-version` must pass before you open a PR. Also boot the app once and hit
   `/actuator/health` when the change touches runtime configuration (framework, auth, JDK).
6. **Stay inside the footprint.** Each skill lists the files it is allowed to change. If the upgrade
   genuinely needs more, say so in the PR description under "Out-of-footprint changes".
7. **Keep the diff reviewable.** Mechanical changes (package renames, codemods) go in their own
   commit, hand-written changes in another, so reviewers can skim the mechanical one.
8. **Commit messages** follow `CLT-<ticket> feature|bug: <summary>`; if no ticket, use
   `PLAT-UPG feature: <summary>`. PR descriptions end with `Devin-Org: engineering`.
9. **Stop on red.** If validation fails and the cause is not something the skill tells you how to
   fix, open the PR as a **draft** with the failure described, and report `status: "blocked"`.
