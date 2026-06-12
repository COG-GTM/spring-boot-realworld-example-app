---
name: remove-legacy-asp-net-mvc-bundling-system-web-optimization
description: "Converted from Devin playbook: Remove Legacy ASP.NET MVC Bundling (System.Web.Optimization)"
triggers:
  - user
  - model
---

# Remove Legacy ASP.NET MVC Bundling (System.Web.Optimization)

## When to Use

Use this skill when the workflow from the source Devin playbook applies.

## Source

- Source playbook: Remove Legacy ASP.NET MVC Bundling (System.Web.Optimization) (playbook-8378cf352ed64c189145bb969b7d9d82)

## Procedure

# Remove Legacy ASP.NET MVC Bundling (`System.Web.Optimization`)

Remove the legacy `System.Web.Optimization` bundling infrastructure from an ASP.NET MVC 5 (or similar .NET Framework) project when a modern frontend build tool (Angular CLI, webpack, Vite, etc.) already handles all asset bundling.

---

## Prerequisites

1. **Confirm modern bundling is in place** — the project must already use a frontend build tool (e.g. Angular CLI outputting to a `dist/` or `Content/app/browser/` folder) and the Razor views must already reference these assets via direct `<link>` / `<script>` tags rather than `@Styles.Render()` or `@Scripts.Render()` helpers.
2. **Confirm `BundleConfig.RegisterBundles` is empty or unused** — if bundles are still actively registered and referenced, those assets must first be migrated to the modern build tool.
3. **Clone the repository and create a feature branch.**

---

## Step 1: Audit Current Usage

Search the entire codebase for references to the bundling system. All of these should return zero matches (or only the files you're about to remove) before proceeding:

```bash
grep -rn --include='*.cs' --include='*.cshtml' --include='*.config' --include='*.csproj' \
  -E 'System\.Web\.Optimization|BundleConfig|BundleTable|@Styles\.Render|@Scripts\.Render|WebGrease|Antlr3' .
```

If any Razor views still use `@Styles.Render("~/bundles/...")` or `@Scripts.Render("~/bundles/...")`, replace them with direct `<link>` / `<script>` tags pointing to the modern build output **before** continuing.

---

## Step 2: Delete `BundleConfig.cs`

Delete the bundling configuration file (typically at `App_Start/BundleConfig.cs`).

---

## Step 3: Update `Global.asax.cs`

- Remove `using System.Web.Optimization;`
- Remove the `BundleConfig.RegisterBundles(BundleTable.Bundles);` call from `Application_Start()`

The remaining `Application_Start()` should typically contain only:
```csharp
AreaRegistration.RegisterAllAreas();
// FilterConfig, RouteConfig, etc.
RouteConfig.RegisterRoutes(RouteTable.Routes);
```

---

## Step 4: Remove NuGet Packages from `packages.config`

Remove these packages (and any other bundling-related packages):

| Package | Notes |
|---------|-------|
| `Microsoft.AspNet.Web.Optimization` | The bundling/minification library |
| `WebGrease` | CSS/JS optimization dependency |
| `Antlr` (3.x) | Parser dependency of WebGrease |

---

## Step 5: Clean Up `.csproj`

Remove the following from the project file:

- **Assembly references** for `System.Web.Optimization`, `WebGrease`, and `Antlr3.Runtime`
- **Compile include** for `App_Start\BundleConfig.cs`
- **Properties** like `<WebGreaseLibPath>` if present
- Any `<HintPath>` entries pointing to the removed package assemblies

---

## Step 6: Clean Up `Web.config`

Remove assembly binding redirects for the removed packages:

```xml
<!-- Remove these <dependentAssembly> blocks -->
<dependentAssembly>
  <assemblyIdentity name="WebGrease" ... />
  <bindingRedirect ... />
</dependentAssembly>
<dependentAssembly>
  <assemblyIdentity name="Antlr3.Runtime" ... />
  <bindingRedirect ... />
</dependentAssembly>
```

Also check `Views/web.config` — if it registers the `System.Web.Optimization` namespace, remove that entry.

---

## Step 7: Verify No Remaining References

Re-run the audit from Step 1:

```bash
grep -rn --include='*.cs' --include='*.cshtml' --include='*.config' --include='*.csproj' \
  -E 'System\.Web\.Optimization|BundleConfig|BundleTable|@Styles\.Render|@Scripts\.Render|WebGrease|Antlr' .
```

This must return **zero matches**.

---

## Step 8: Build & Test

1. **Build the .NET solution** to confirm it compiles without the removed assemblies:
   ```bash
   # Windows
   msbuild MySolution.sln
   # Linux (Mono)
   xbuild MySolution.sln
   ```

2. **Build the frontend** to confirm modern bundling still works:
   ```bash
   # Example for Angular CLI
   npx ng build
   ```

3. **Run the app** and verify the landing page loads correctly — all CSS and JS should be served via the direct `<link>` / `<script>` tags.

4. **Check the browser console** for any 404s or loading errors related to missing assets.

---

## Step 9: Commit & Create PR

Commit with a clear message:
```
refactor: remove System.Web.Optimization bundling infrastructure
```

PR description should include:
- Summary of what was removed and why (dead code since modern bundling took over)
- List of files changed
- Confirmation that no remaining references exist
- Build and runtime verification results

---

## Checklist

- [ ] `BundleConfig.cs` deleted
- [ ] `Global.asax.cs` cleaned (no `System.Web.Optimization` using or `RegisterBundles` call)
- [ ] `packages.config` cleaned (removed `Microsoft.AspNet.Web.Optimization`, `WebGrease`, `Antlr`)
- [ ] `.csproj` cleaned (removed references, compile includes, properties)
- [ ] `Web.config` cleaned (removed binding redirects)
- [ ] `Views/web.config` checked (no `System.Web.Optimization` namespace)
- [ ] Razor views confirmed using direct `<link>`/`<script>` tags (no `@Styles.Render`/`@Scripts.Render`)
- [ ] Grep returns zero matches for all bundling-related terms
- [ ] Solution builds successfully
- [ ] Frontend builds successfully
- [ ] App runs and loads correctly in browser
- [ ] No console errors for missing assets

## Verification

Confirm the generated skill has a clear trigger, required inputs, ordered steps, and no credentials or session-specific assumptions.
