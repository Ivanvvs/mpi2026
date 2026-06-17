# Local Run

Run commands from the project root.

## Requirements

```text
Java 21+
PostgreSQL 16+
Node.js 20+
make
```

## Windows

Prepare PostgreSQL:

```powershell
make setup-db
```

This creates or updates a local `.env` file with Spring datasource credentials. The file is ignored by git.

Terminal 1, backend:

```powershell
make backend
```

Terminal 2, frontend:

```powershell
make frontend
```

If `make` is not installed, use:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\setup-db.ps1 -PostgresPassword 2518
.\mvnw.cmd spring-boot:run
```

```powershell
cd frontend-vue
npm install
npm run dev -- --host 127.0.0.1
```

## macOS

Install tools:

```bash
brew install postgresql@16 node make
brew services start postgresql@16
export PATH="/opt/homebrew/opt/postgresql@16/bin:$PATH"
chmod +x ./mvnw ./scripts/setup-db.sh
```

Prepare PostgreSQL:

```bash
make setup-db
```

This creates or updates a local `.env` file with Spring datasource credentials. The file is ignored by git.

Terminal 1, backend:

```bash
make backend
```

Terminal 2, frontend:

```bash
make frontend
```

## Open

```text
http://127.0.0.1:5173/
```

## Demo Users

```text
admin / 1234
student / 1234
curator / 1234
examiner / 1234
```

## Ports

```text
Backend:  http://localhost:8081
Frontend: http://127.0.0.1:5173
Postgres: localhost:5432
```

## Environment

The backend reads database credentials from a local `.env` file in the project root.

To get these environment variables automatically, run:

```powershell
make setup-db
```

This command creates the PostgreSQL database/user and writes `.env` automatically.

After it finishes, `.env` will contain:

```properties
spring.datasource.username=ksu
spring.datasource.password=<generated-local-password>
```

Then start the backend:

```powershell
make backend
```

If `make` is not installed on Windows, run:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\setup-db.ps1 -PostgresPassword xxxx
.\mvnw.cmd spring-boot:run
```

If you want fixed local study credentials instead of a generated password, run:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\setup-db.ps1 -PostgresPassword xxxx -AppPassword ksu
```

Then `.env` will contain:

```properties
spring.datasource.username=ksu
spring.datasource.password=ksu
```

`.env.example` is only a template that shows which keys are needed. `.env` is ignored by git and should stay local.

`jwt.secret` is optional for local runs. If it is not set, the backend generates a temporary key on startup.
