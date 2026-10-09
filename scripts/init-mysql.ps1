#Requires -Version 5.1
<#
  init-mysql.ps1
  ------------------------------------------------------------------
  Creates the `java_quiz_platform` database and loads the schema
  and the sample data (users, quizzes, questions, options, results).

  Run ONCE, before deploying the WAR. Requires MySQL installed with
  the `mysql` command line client available.

  From the project root:

    # If `mysql` is on your PATH and you will type the password when asked:
    powershell -ExecutionPolicy Bypass -File scripts\init-mysql.ps1

    # If `mysql` is NOT on your PATH (common on Windows):
    powershell -ExecutionPolicy Bypass -File scripts\init-mysql.ps1 `
        -MySqlBin "C:\Program Files\MySQL\MySQL Server 8.0\bin"

    # To avoid the password prompt (scripted / CI):
    powershell -ExecutionPolicy Bypass -File scripts\init-mysql.ps1 `
        -MySqlBin "C:\Program Files\MySQL\MySQL Server 8.0\bin" `
        -User root -Password "yourMysqlPassword"
#>
param(
    [string]$MySqlBin = "",       # folder that contains mysql.exe
    [string]$User     = "root",
    [string]$Password = ""        # leave empty to be prompted
)

$ErrorActionPreference = "Stop"
$projectRoot = Split-Path -Parent $PSScriptRoot

# ---- resolve mysql.exe -------------------------------------------------
if ($MySqlBin) {
    $mysql = Join-Path $MySqlBin "mysql.exe"
    if (-not (Test-Path $mysql)) { throw "mysql.exe not found in: $MySqlBin" }
} else {
    $mysql = (Get-Command mysql -ErrorAction SilentlyContinue).Source
    if (-not $mysql) {
        throw "MySQL CLI 'mysql' not found on PATH. Install MySQL Server or pass -MySqlBin `"C:\Program Files\MySQL\MySQL Server 8.0\bin`""
    }
}
Write-Output "==> Using mysql: $mysql"

# ---- prompt for password if not supplied ------------------------------
if ([string]::IsNullOrEmpty($Password)) {
    $secure = Read-Host "Enter MySQL password for user '$User'" -AsSecureString
    $Password = [Runtime.InteropServices.Marshal]::PtrToStringBSTR(
        [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure))
}

# ---- import schema + sample data --------------------------------------
$schema = Join-Path $projectRoot "database\schema.sql"
$sample = Join-Path $projectRoot "database\sample_data.sql"

Write-Output "==> Creating database + tables from schema.sql ..."
Get-Content $schema -Raw | & $mysql "-u$User" "-p$Password"
if ($LASTEXITCODE -ne 0) { throw "Failed to import schema.sql" }

Write-Output "==> Loading sample data from sample_data.sql ..."
Get-Content $sample -Raw | & $mysql "-u$User" "-p$Password"
if ($LASTEXITCODE -ne 0) { throw "Failed to import sample_data.sql" }

# ---- verify -----------------------------------------------------------
Write-Output ""
Write-Output "==> Verification (row counts in java_quiz_platform):"
& $mysql "-u$User" "-p$Password" -D java_quiz_platform -e @"
SELECT 'users'      AS table_name, COUNT(*) AS rows FROM users
UNION ALL SELECT 'quizzes',  COUNT(*) FROM quizzes
UNION ALL SELECT 'questions',COUNT(*) FROM questions
UNION ALL SELECT 'options',  COUNT(*) FROM options
UNION ALL SELECT 'results',  COUNT(*) FROM results;
"@

Write-Output ""
Write-Output "DONE. Expected: users=3, quizzes=4, questions=33, options=132, results=2"
