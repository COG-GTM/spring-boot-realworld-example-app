---
name: add-opentelemetry-logging-to-frontend-application
description: "Converted from Devin playbook: Add OpenTelemetry Logging to Frontend Application"
triggers:
  - user
  - model
---

# Add OpenTelemetry Logging to Frontend Application

## When to Use

Use this skill when the workflow from the source Devin playbook applies.

## Source

- Source playbook: Add OpenTelemetry Logging to Frontend Application (playbook-2f30df1ce29340a78c5f194a45976981), macro `!jsotel`

## Procedure

# Playbook: Add OpenTelemetry Logging to Frontend Application

## Overview
Add OpenTelemetry-style tracing and logging to a frontend JavaScript application to enable observability of critical user operations. This playbook covers both vanilla JavaScript applications and applications with build systems (React, Vue, Angular, etc.).

## What's Needed From User
- Repository URL and branch to work on
- (Optional) List of critical operations to instrument - if not provided, these will be automatically identified
- (Optional) Preferred exporter type: console logging (default) or OTLP exporter for production

## Procedure

1. Clone the repository and explore the codebase structure
   - Identify if the project uses a build system (npm/yarn with bundler) or is vanilla JavaScript
   - Locate the main application entry point and JavaScript/TypeScript files
   - Check for existing logging or telemetry implementations

2. Identify critical operations to instrument
   - If the user provided a list of operations, use those
   - If not provided, analyze the codebase to identify critical operations by searching for:
     - **Authentication flows**: Functions/handlers with names containing "login", "logout", "auth", "signIn", "signOut", "register"
     - **Form submissions**: Event listeners for "submit" events, form handler functions
     - **API calls**: `fetch()`, `axios`, `XMLHttpRequest`, or other HTTP client usage
     - **Financial/data operations**: Functions handling transfers, payments, orders, or CRUD operations
     - **User actions**: Click handlers on important buttons (submit, delete, save, update, create)
     - **Navigation/routing**: Route change handlers, page transition functions
   - Prioritize operations that: modify data, involve user authentication, handle sensitive information, or are core to the application's purpose
   - Document the identified operations before proceeding with instrumentation

3. Set up OpenTelemetry based on project type
   - **For projects with build systems (npm/yarn):** Install OpenTelemetry packages via npm:
     ```bash
     npm install @opentelemetry/api @opentelemetry/sdk-trace-web @opentelemetry/resources @opentelemetry/exporter-trace-otlp-http
     ```
   - **For vanilla JavaScript projects:** Implement a lightweight self-contained tracing class that outputs OpenTelemetry-compatible JSON to the console (CDN-based OpenTelemetry bundles often have issues with global variable exposure)

4. Create the tracer initialization code
   - Configure the tracer provider with service name and version
   - Set up the appropriate span exporter (ConsoleSpanExporter for testing, OTLP exporter for production)
   - Initialize the tracer at application startup before any instrumented code runs

5. Instrument critical operations with spans
   - Wrap each critical operation in a span using `tracer.startSpan(operationName)`
   - Add relevant attributes to spans (e.g., user ID, transaction amount, operation status)
   - Set span status to OK or ERROR based on operation outcome
   - Record exceptions when errors occur using `span.recordException(error)`
   - Always call `span.end()` when the operation completes (success or failure)

6. Ensure spans include OpenTelemetry-compliant fields
   - `traceId`: 32-character hex string for distributed tracing correlation
   - `spanId`: 16-character hex string for span identification
   - `name`: Operation name (e.g., "user.login", "transaction.transfer")
   - `startTime` and `endTime`: ISO 8601 timestamps
   - `duration`: Time elapsed in milliseconds
   - `status`: OK, ERROR, or UNSET
   - `attributes`: Key-value pairs with operation-specific data

7. Run the application locally and verify logging works
   - Start the application using the appropriate dev server (e.g., `npm run dev`, `npx serve`) or by opening the HTML file directly
   - Open browser developer tools (F12) and navigate to the Console tab
   - Perform each instrumented operation (login, transactions, form submissions, etc.)
   - Verify that spans appear in the console with green "[OTEL SPAN]" prefix followed by the operation name

8. Validate OpenTelemetry format compliance for each span
   - **Required fields check:** Expand each span log entry and confirm it contains:
     - `traceId`: 32-character hexadecimal string (e.g., "b2820622a1b2c3d4e5f6...")
     - `spanId`: 16-character hexadecimal string (e.g., "9d869b2d1a2b3c4d")
     - `name`: Operation name matching the instrumented function (e.g., "user.login")
     - `startTime`: ISO 8601 timestamp (e.g., "2024-01-15T10:30:00.000Z")
     - `endTime`: ISO 8601 timestamp after startTime
     - `duration`: Positive number in milliseconds (e.g., "15ms")
     - `status`: "OK" for successful operations, "ERROR" for failures
     - `attributes`: Object containing operation-specific key-value pairs
   - **Success path test:** Perform a successful operation and verify status is "OK"
   - **Failure path test:** Trigger an error condition (e.g., invalid login) and verify status is "ERROR" with appropriate error attributes
   - **Attributes validation:** Confirm attributes contain meaningful data (e.g., for login: username; for transfer: amount, sender, receiver)
   - For OTLP exporters, verify spans are being sent to the configured backend by checking the observability platform

9. Commit changes and create a pull request
   - Stage all modified files
   - Write a descriptive commit message explaining the OpenTelemetry instrumentation added
   - Push to a feature branch and create a PR

## Specifications
- All critical operations (user-provided or auto-identified) must have span instrumentation
- Span output must be OpenTelemetry-compliant with traceId, spanId, timestamps, duration, status, and attributes
- Console logging must be visually distinguishable (colored output with "[OTEL SPAN]" prefix)
- The implementation must not break existing application functionality
- For vanilla JS apps, use a self-contained tracing implementation rather than CDN bundles (which have reliability issues)

**Validation:** Run the application locally and perform the following verification steps:
1. Execute each instrumented operation (both success and failure paths)
2. Capture screenshots or screen recordings of the browser console showing the OpenTelemetry spans
3. Verify each span contains: traceId (32 hex chars), spanId (16 hex chars), name, startTime, endTime, duration, status (OK/ERROR), and relevant attributes
4. Confirm success operations show status "OK" and failure operations show status "ERROR" with error details
5. Share the console output evidence with the user to demonstrate the instrumentation is working correctly

## Advice and Pointers
- For vanilla JavaScript applications without a build system, CDN-based OpenTelemetry bundles often fail to expose global variables correctly. Use a lightweight self-contained `ConsoleSpanExporter` class instead that outputs OpenTelemetry-compatible JSON.
- Always initialize the tracer before any instrumented code runs - place initialization at the top of the main script file.
- Use descriptive span names following the pattern `domain.operation` (e.g., "user.login", "transaction.transfer", "ui.update").
- Include both success and failure paths in instrumentation - set status to ERROR and record exceptions when operations fail.
- For async operations, ensure `span.end()` is called in both `.then()` and `.catch()` handlers, or use try/finally blocks.

## Forbidden Actions
- Do not rely solely on CDN-based OpenTelemetry bundles for vanilla JavaScript projects without testing that globals are properly exposed
- Do not skip the verification step - always run the app and confirm spans appear in the console
- Do not leave spans without calling `span.end()` - this causes memory leaks and incomplete telemetry
- Do not instrument trivial operations that would create excessive noise in telemetry data

## Verification

Confirm the generated skill has a clear trigger, required inputs, ordered steps, and no credentials or session-specific assumptions.
