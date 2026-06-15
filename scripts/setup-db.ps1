param(
    [string]$PostgresUser = "postgres",
    [string]$PostgresPassword = "",
    [string]$AppDb = "examdb",
    [string]$AppUser = "ksu",
    [string]$AppPassword = "ksu"
)

$ErrorActionPreference = "Stop"

$pg18 = "C:\Program Files\PostgreSQL\18\bin"
$pg17 = "C:\Program Files\PostgreSQL\17\bin"
$pg16 = "C:\Program Files\PostgreSQL\16\bin"

foreach ($path in @($pg18, $pg17, $pg16)) {
    if (Test-Path $path) {
        $env:Path = "$path;$env:Path"
    }
}

if (-not (Get-Command psql -ErrorAction SilentlyContinue)) {
    throw "psql is not available. Add PostgreSQL bin directory to PATH."
}

if ($PostgresPassword) {
    $env:PGPASSWORD = $PostgresPassword
}

psql -U $PostgresUser -d postgres -v ON_ERROR_STOP=1 -c "DO `$`$ BEGIN IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = '$AppUser') THEN CREATE ROLE $AppUser LOGIN PASSWORD '$AppPassword'; ELSE ALTER ROLE $AppUser WITH LOGIN PASSWORD '$AppPassword'; END IF; END `$`$;"

$exists = psql -U $PostgresUser -d postgres -tAc "SELECT 1 FROM pg_database WHERE datname='$AppDb';"
if (-not $exists) {
    createdb -U $PostgresUser -O $AppUser $AppDb
}

psql -U $PostgresUser -d postgres -v ON_ERROR_STOP=1 -c "GRANT ALL PRIVILEGES ON DATABASE $AppDb TO $AppUser;"

$env:PGPASSWORD = $AppPassword
psql -U $AppUser -d $AppDb -v ON_ERROR_STOP=1 -c "SELECT current_database(), current_user;"
