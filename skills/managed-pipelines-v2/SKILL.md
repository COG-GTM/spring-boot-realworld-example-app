---
name: managed-pipelines-v2
description: Migrate a service from GS Managed Pipelines v1 (Jenkinsfile + @Library('gs-managed-pipelines@v1')) to Managed Pipelines v2 (declarative .gs/pipeline.yml with PR-time CLM and build gates). Use when a repo still has a v1 Jenkinsfile.
---

# Managed Pipelines v2 migration

v2 replaces the scripted `Jenkinsfile` with a declarative manifest at `.gs/pipeline.yml`. v2 runs
the CLM policy gate **at PR time**, so the repo must already be CLM-clean (run `clm-scan-fix`
first) or every PR will be red.

## Footprint
`Jenkinsfile` (deleted), `.gs/pipeline.yml` (new), `.github/workflows/ci.yml` (mirror of the build
gate), `README.md` pipeline section, `.github/CODEOWNERS` (path rename only).

## Steps
1. Read the existing `Jenkinsfile`. Capture: `appId`, `team`, `jdk`, `buildTool`, `goals`,
   deploy environments/strategy, notification channel.
2. Create branch `platform/02-managed-pipelines-v2`.
3. Write `.gs/pipeline.yml`:
   ```yaml
   apiVersion: gs.pipelines/v2
   kind: ServicePipeline
   metadata:
     appId: <appId>
     team: <team>
   build:
     tool: maven
     jdk: <jdk from Jenkinsfile, e.g. "11"; keep as-is, JDK bumps are a separate playbook>
     goals: clean verify
     cache: maven
   gates:
     pullRequest: [build, unit-tests, clm-policy]
     main: [build, unit-tests, clm-policy, sbom]
   deploy:
     environments: <from Jenkinsfile>
     strategy: <from Jenkinsfile>
   notifications:
     slack: <channel>
   ```
4. Delete `Jenkinsfile`. Update `CODEOWNERS` so the `/Jenkinsfile` owners now own `/.gs/pipeline.yml`.
5. Keep `.github/workflows/ci.yml` as the GitHub mirror of the `pullRequest` gate; make sure it
   reads the JDK from `.java-version` (`java-version-file`) rather than hard-coding it.
6. Validate: `python3 -c "import yaml,sys; yaml.safe_load(open('.gs/pipeline.yml'))"` (or any
   YAML parser) and `mvn -B -ntp clean verify`.
7. Update the README pipeline row to "GS Managed Pipelines **v2** (`.gs/pipeline.yml`)".
8. Commit `PLAT-UPG feature: migrate to managed pipelines v2` and open PR
   `[platform 2/N] Managed Pipelines v2`.

## Done when
`.gs/pipeline.yml` exists and parses, `Jenkinsfile` is gone, CI mirror reads `.java-version`,
build is green, PR open.
