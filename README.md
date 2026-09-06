# Codexa

A technical coding-problem practice platform: admins author Python problems with test cases,
users solve them in an in-browser editor, and submissions run against the test cases via the
JDoodle execution API.

- **Backend** — Spring Boot 3.4 / Java 24 / PostgreSQL, JWT authentication (`./src`)
- **Frontend** — React 19 + React Router 7 + Monaco editor (`./my-app`)

## Prerequisites

- JDK 24
- PostgreSQL with a `codeplatform` database
- Node.js 20+
- A [JDoodle](https://www.jdoodle.com/compiler-api) client id and secret (free tier is enough)

## Configuration

No credentials live in the repository. Copy `.env.example` to `.env` and fill it in; the backend
reads `.env` directly at startup (via `spring.config.import`), so no shell export step is needed
whether you start it from Maven, the IDE, or `java -jar`.

```bash
cp .env.example .env
# edit .env
```

| Variable | Purpose |
| --- | --- |
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | PostgreSQL connection |
| `JWT_SECRET` | Token signing key — **at least 32 characters** (`openssl rand -base64 48`) |
| `JWT_EXPIRATION_MINUTES` | Token lifetime, default 1440 |
| `ADMIN_USERNAMES` | Comma-separated usernames granted `ADMIN` at registration |
| `CORS_ALLOWED_ORIGINS` | Origins allowed to call the API, default `http://localhost:3000` |
| `JDOODLE_CLIENT_ID`, `JDOODLE_CLIENT_SECRET` | Code execution API credentials |

Every variable has a development default except the JDoodle credentials; without those the
`/api/execute` endpoint returns `503`. Put your own username in `ADMIN_USERNAMES` **before**
registering it — the role is assigned at registration time.

## Running

```bash
# Backend on :8080
./mvnw spring-boot:run

# Frontend on :3000 (proxies /api to :8080)
cd my-app && npm install && npm start
```

For a deployment where the API is on another origin, set `REACT_APP_API_BASE_URL` when building
the frontend.

## Tests

```bash
./mvnw test              # backend
cd my-app && npm test    # frontend
```

## API

| Method | Path | Access |
| --- | --- | --- |
| `POST` | `/api/auth/register` | public |
| `POST` | `/api/auth/login` | public — returns `{ token, username, role }` |
| `GET` | `/api/auth/me` | authenticated |
| `GET` | `/api/problems`, `/api/problems/{id}` | public |
| `POST`/`PUT`/`DELETE` | `/api/problems/**` | `ADMIN` |
| `*` | `/api/testcases/**` | `ADMIN` |
| `*` | `/api/users/**` | `ADMIN` |
| `POST` | `/api/execute` | authenticated — runs the code, saves it, and updates progress |
| `GET` | `/api/submissions/mine` | authenticated — the caller's saved work and solved status per problem |
| `GET` | `/api/submissions/problem/{id}` | authenticated — the caller's saved code for one problem (404 if none) |
| `PUT` | `/api/submissions/problem/{id}` | authenticated — autosave `{ "code": "..." }` without running |

Authenticate with `Authorization: Bearer <token>`. Missing credentials return `401`; insufficient
role returns `403`.

## How execution works

A problem stores a `functionSignature` (`def add(a, b):`) and test cases whose `input` is the
comma-separated Python argument list (`2,3`) and whose `expected` is the printed result (`5`).

`PythonHarness` assembles a script from the signature, the submitted body and the test cases,
wrapping each call in a boundary marker:

```python
def add(a, b):
    return a + b

__CASES__ = [
    [2,3],
]

for __args in __CASES__:
    print("__CASE_BOUNDARY_9f3a__")
    try:
        print(add(*__args))
    except Exception as __exc:
        print("__CASE_ERROR_9f3a__" + type(__exc).__name__ + ": " + str(__exc))
```

Results are split on the marker rather than matched line-by-line, so a case that returns a
multi-line value does not shift every later result. Arguments are splatted from a list so a single
list argument (`[1,2,3]`) stays distinct from two scalars (`1,2`).

Submissions must `return` their answer; a body that calls `print` is rejected before the execution
API is called, so an invalid submission costs no quota.

## Saved code and progress

Each (user, problem) pair has one `Submission` row. The editor autosaves to it ~1s after you stop
typing, and every run overwrites it with the code that ran plus the pass count. `solved` flips to
true the first time every test case passes and stays true afterwards, so the problem list shows a
tick even if you later break the solution while experimenting.
