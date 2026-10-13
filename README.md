# Dynamic Form Builder (Backend)

REST API for building forms without writing code. An admin designs a form (fields, sections, validation rules, show/hide conditions), publishes it as a version, and anyone with the form's link can fill it in. Every published version is kept, and each submission stays tied to the version it was filled in on.

This repository is the backend only. The frontend is a separate project that talks to this API.

## Tech stack

| | |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 4.1.1 (Web MVC, Security, Data JPA, Validation) |
| Database | PostgreSQL |
| Migrations | Flyway |
| Auth | JWT bearer tokens |
| Build | Maven (wrapper included) |

## Features

- **Forms**: create, list, search, update, delete, archive, clone.
- **Components**: `TEXT`, `TEXTAREA`, `NUMBER`, `DECIMAL`, `EMAIL`, `PHONE`, `DROPDOWN`, `RADIO`, `CHECKBOX`, `MULTISELECT`, `TOGGLE`, `DATE`, `DATETIME`, `FILE_UPLOAD`, plus `SECTION` and `HEADING` for layout. Components can be reordered and moved between sections.
- **Validation rules**: `MIN_LENGTH`, `MAX_LENGTH`, `REGEX`, `MIN`, `MAX`, `DATE_MIN`, `DATE_MAX`.
- **Conditions**: show/hide, enable/disable, or make a field required/optional, based on other fields' answers.
- **Versioning**: publishing freezes the form into a snapshot. A published form is locked; changes need a new draft version. Older versions stay readable.
- **Public submissions**: start, upload files, submit, and optionally rate. Answers are validated on the server against the version the person started on.
- **File uploads**: jpg, png, webp, pdf, up to 10 MB, stored on local disk. The content is checked against the file's signature, not just its extension.
- **Rating**: an optional per-form step after submit (0.5 to 5), with average and count in the submission list.
- **History and audit**: submission list and detail per form, and an audit log of who changed what.

## Prerequisites

| Tool | Version | Notes |
|---|---|---|
| JDK | **21** | Newer JDKs can fail to compile the project (Lombok). `java -version` must say 21 |
| PostgreSQL | 14 or newer | Running on `localhost:5432` |
| Maven | not needed | Use the included wrapper: `./mvnw` (Windows: `mvnw.cmd`) |

## Local setup

### 1. Clone

```
git clone https://github.com/Rawat-Aashish/dyn-renderer-be.git
cd dyn-renderer-be
```

### 2. Create the database

```
psql -U postgres -h localhost -c "CREATE DATABASE dynamic_form_builder"
```

Only the empty database is needed. The tables are created by Flyway when the app starts.

### 3. Create the local config file

```
cp application-local.properties.example application-local.properties
```

(Windows: `copy application-local.properties.example application-local.properties`)

Open `application-local.properties` and fill in the values described in the next section. The file is git-ignored; never commit it.

### 4. Run

```
./mvnw spring-boot:run
```

The API is served at `http://localhost:8080`. Run the command from the project root, otherwise the local config file is not found.

### 5. Log in

With `app.seed.enabled=true`, the first start creates an admin user:

| Email | Password |
|---|---|
| `admin@gmail.com` | `password` |

```
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@gmail.com","password":"password"}'
```

Send the returned token as `Authorization: Bearer <token>` on every admin endpoint.

## Configuration

Settings are read from two files:

- `src/main/resources/application.properties`: committed, non-secret defaults.
- `application-local.properties` in the project root: git-ignored, holds secrets and machine-specific values, and overrides the committed file.

Any setting can also be given as an environment variable instead of in the file (the name in the second column).

### Required

| Property | Environment variable | Description |
|---|---|---|
| `spring.datasource.password` | `SPRING_DATASOURCE_PASSWORD` | Password of the PostgreSQL user |
| `app.jwt.secret` | `APP_JWT_SECRET` | Key used to sign login tokens. At least 32 characters; the app refuses to start without it. Generate one with `openssl rand -base64 48` |

### Optional

| Property | Environment variable | Default | Description |
|---|---|---|---|
| `spring.datasource.url` | `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/dynamic_form_builder` | Database connection URL |
| `spring.datasource.username` | `SPRING_DATASOURCE_USERNAME` | `postgres` | Database user |
| `app.cors.allowed-origins` | `APP_CORS_ALLOWED_ORIGINS` | empty (no browser origin allowed) | Comma separated origins of the frontend, e.g. `http://localhost:3000` |
| `app.seed.enabled` | `APP_SEED_ENABLED` | `false` | `true` creates the `admin@gmail.com` test user on startup. Local development only |
| `app.jwt.expiration-minutes` | `APP_JWT_EXPIRATION_MINUTES` | `60` | How long a login token is valid |
| `app.upload.dir` | `APP_UPLOAD_DIR` | `uploads` | Folder for uploaded files, relative to where the app is started. An absolute path also works |
| `server.port` | `SERVER_PORT` | `8080` | Port the API listens on |

## Database migrations

Migrations are plain SQL files in `src/main/resources/db/migration`, named `V<number>__<description>.sql`. Flyway applies the pending ones on every start, so there is no separate migrate command.

To change the schema, add a new file with the next number. Never edit a migration that has already been applied. Hibernate runs with `ddl-auto=validate`: it creates nothing and stops the app if an entity does not match the tables.

## Tests and build

```
./mvnw test                                  # all tests
./mvnw test -Dtest=ClassName#methodName      # one test
./mvnw clean package                         # build the jar into target/ (runs the tests)
```

The tests start the whole application, so they need PostgreSQL running and `application-local.properties` in place.

## API overview

Every JSON response has the same shape:

```json
{ "success": true, "message": "Form created successfully", "data": { } }
```

Validation failures (400) add an `errors` object of `{ "field": "message" }`.

### Public (no token)

| Method | Path | Purpose |
|---|---|---|
| POST | `/api/auth/login` | Log in, returns a token |
| GET | `/api/forms/{formCode}/published` | The live version of a form |
| POST | `/api/forms/{formCode}/submissions/start` | Start filling in a form, returns a submission token |
| POST | `/api/forms/{formCode}/submissions/files` | Upload a file for a `FILE_UPLOAD` field (multipart: `token`, `fieldCode`, `file`) |
| POST | `/api/forms/{formCode}/submissions` | Submit the answers |
| POST | `/api/forms/{formCode}/submissions/rating` | Rate the form after submitting, if the form asks for it |

### Admin (`Authorization: Bearer <token>`)

| Method | Path | Purpose |
|---|---|---|
| POST | `/api/forms` | Create a form |
| GET | `/api/forms` | List forms (`page`, `size`, `archived`, `status`, `search`) |
| GET | `/api/forms/{id}` | One form with its components and conditions |
| PUT | `/api/forms/{id}` | Update form details |
| DELETE | `/api/forms/{id}` | Delete a form that was never published |
| POST | `/api/forms/{id}/archive` | Archive a form |
| POST | `/api/forms/{id}/clone` | Copy a form into a new draft |
| POST | `/api/forms/{formId}/components` | Add a component |
| PUT | `/api/forms/{formId}/components/{componentId}` | Update a component |
| PUT | `/api/forms/{formId}/components/reorder` | Reorder components |
| DELETE | `/api/forms/{formId}/components/{componentId}` | Delete a component |
| POST | `/api/components/{componentId}/validations` | Add a validation rule |
| PUT | `/api/components/{componentId}/validations/{ruleId}` | Update a validation rule |
| DELETE | `/api/components/{componentId}/validations/{ruleId}` | Delete a validation rule |
| POST | `/api/forms/{formId}/conditions` | Add a condition |
| PUT | `/api/forms/{formId}/conditions/{conditionId}` | Update a condition |
| DELETE | `/api/forms/{formId}/conditions/{conditionId}` | Delete a condition |
| GET | `/api/forms/{formId}/versions` | List versions |
| GET | `/api/forms/{formId}/versions/{version}` | One version with its snapshot |
| POST | `/api/forms/{formId}/versions` | Create the next draft version |
| POST | `/api/forms/{formId}/versions/{version}/publish` | Publish a draft version |
| GET | `/api/forms/{formCode}/submissions` | List a form's submissions |
| GET | `/api/submissions/{submissionId}` | One submission |
| GET | `/api/submissions/{submissionId}/files/{fileId}` | Download an uploaded file |
| GET | `/api/forms/{formId}/audit` | Audit log of a form |

### Typical flow

1. Log in.
2. Create a form, add components, validation rules and conditions.
3. Publish version 1. The form is now public under its `code` and locked for editing.
4. A visitor loads the published form, starts a submission, uploads any files, submits, and optionally rates.
5. The admin reads the submissions and the audit log.
6. To change the form, create a new version, edit, and publish it. People who started on the old version can still submit against it.

## Project structure

```
src/main/java/com/dynamicformbuilder/
  auth/          login
  security/      JWT, roles, user lookup
  config/        security, CORS, app properties
  form/          forms
  component/     form fields and sections
  validation/    validation rules
  condition/     conditions
  formversion/   versions, publishing, snapshots
  submission/    public form, submissions, file uploads, rating
  audit/         audit log
  common/        response envelope, exceptions, global exception handler
src/main/resources/
  application.properties
  db/migration/  Flyway SQL migrations
```

Each feature follows the same layers: Controller → Service → Repository → Entity, with request and response DTOs. Entities are never returned from the API. Errors are thrown by services and turned into responses by one `GlobalExceptionHandler`.

## Things to know

- **Uploaded files live on local disk** in `app.upload.dir`. The database holds only the path and metadata, so back up the folder together with the database. Running more than one copy of the app needs a shared folder.
- **Files of submissions that were started but never submitted are not cleaned up.**
- **The seeded test user has a known password.** Keep `app.seed.enabled` off anywhere other than local development.
- **A form's `code` never changes** and stays reserved even after the form is archived.
