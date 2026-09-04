# Reproducible Release Validation

The release runner provisions an isolated MySQL schema, applies migrations V2 through V5 twice, checks the resulting indexes, and runs the release-only external integration tests against that schema and a dedicated Redis namespace. It removes the temporary schema in a `finally` block and refuses to create or drop a name that does not match the validation prefix.

## Prerequisites

- MySQL 8.x with a user allowed to create and drop schemas, and the `mysql` client on `PATH`.
- Redis Stack with RediSearch enabled, and the application dependencies available to Maven.
- PowerShell 7 and Maven 3.9 or compatible executables.

The database user must be able to create and drop only the temporary validation schemas needed for this run. Do not point the runner at a production schema.

## Environment

`RELEASE_VALIDATION_DB_PASSWORD` is required. Optional database variables are `RELEASE_VALIDATION_DB_HOST`, `RELEASE_VALIDATION_DB_PORT`, `RELEASE_VALIDATION_DB_USER`, `RELEASE_VALIDATION_MYSQL_BIN`, and `RELEASE_VALIDATION_MAVEN_BIN`. Redis defaults to localhost and can be changed with `RELEASE_VALIDATION_REDIS_HOST` and `RELEASE_VALIDATION_REDIS_PORT`.

The runner passes `RELEASE_VALIDATION_JDBC_URL`, database credentials, and Redis coordinates to the external Maven process. Passwords remain environment values and are never written to evidence files or command arguments.

## Invocation

From the `ai-medical-care` project root:

```powershell
$env:RELEASE_VALIDATION_DB_PASSWORD = '<password>'
pwsh -File .\scripts\release-validation.ps1
```

The command prints only the evidence-file path. Each run writes a timestamped Markdown record under `docs/verification/runs/`, including status, migration pass count, cleanup status, index verification, and external-test profile. Those generated records are ignored by Git because they contain environment-specific run metadata and should not become production documentation.

The runner uses only names beginning with `ai_medical_care_release_validation_`, validates the complete generated name immediately before create and drop, and never alters application data outside that namespace.
