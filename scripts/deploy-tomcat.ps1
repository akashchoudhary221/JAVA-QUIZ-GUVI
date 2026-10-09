#Requires -Version 5.1
<#
  deploy-tomcat.ps1
  ------------------------------------------------------------------
  Builds the production WAR (configured for MySQL) and deploys it to
  your Apache Tomcat installation.

  STEP 1  Edit src\main\resources\config.properties and set your MySQL
          password (db.password=...). This file is baked into the WAR.

  STEP 2  From the project root run:

    powershell -ExecutionPolicy Bypass -File scripts\deploy-tomcat.ps1 `
        -TomcatHome "C:\Program Files\Apache Software Foundation\Tomcat 9.0"

  STEP 3  Start Tomcat and open:
        http://localhost:8080/java-quiz-platform
#>
param(
    [Parameter(Mandatory=$true)]
    [string]$TomcatHome,
    [switch]$SkipBuild          # skip `mvn package` if WAR already built
)

$ErrorActionPreference = "Stop"
$projectRoot = Split-Path -Parent $PSScriptRoot
Set-Location $projectRoot

# ---- build the WAR -----------------------------------------------------
if (-not $SkipBuild) {
    Write-Output "==> Building production WAR (mvn clean package -DskipTests) ..."
    & mvn -q clean package -DskipTests
    if ($LASTEXITCODE -ne 0) { throw "Maven build FAILED" }
}

$war = Join-Path $projectRoot "target\java-quiz-platform.war"
if (-not (Test-Path $war)) { throw "WAR not found at: $war" }

$webapps = Join-Path $TomcatHome "webapps"
if (-not (Test-Path $webapps)) { throw "Tomcat webapps folder not found: $webapps" }

# ---- stop Tomcat if it is currently running ----------------------------
$shutdown = Join-Path $TomcatHome "bin\shutdown.bat"
if (Test-Path $shutdown) {
    Write-Output "==> Stopping any running Tomcat ..."
    & cmd /c "`"$shutdown`"" 2>&1 | Out-Null
    Start-Sleep -Seconds 3
}

# ---- deploy ------------------------------------------------------------
Write-Output "==> Deploying WAR to $webapps ..."
Copy-Item $war (Join-Path $webapps "java-quiz-platform.war") -Force

# Remove the old exploded folder so Tomcat unpacks a fresh copy
$exploded = Join-Path $webapps "java-quiz-platform"
if (Test-Path $exploded) { Remove-Item $exploded -Recurse -Force }

# ---- next steps --------------------------------------------------------
Write-Output ""
Write-Output "======================================================="
Write-Output " DEPLOYED SUCCESSFULLY"
Write-Output "======================================================="
Write-Output " 1) Start Tomcat :"
Write-Output "      & `"$TomcatHome\bin\startup.bat`""
Write-Output " 2) Open browser :"
Write-Output "      http://localhost:8080/java-quiz-platform"
Write-Output ""
Write-Output " Default logins:"
Write-Output "   Admin      : admin@quiz.com   / admin123"
Write-Output "   Creator    : creator@quiz.com / creator123"
Write-Output "   Participant: student@quiz.com / student123"
Write-Output "======================================================="
