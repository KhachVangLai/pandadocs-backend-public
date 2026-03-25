param()

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
$localConfigExample = Join-Path $repoRoot "src/main/resources/application-local.example.properties"
$localConfig = Join-Path $repoRoot "src/main/resources/application-local.properties"
$secretsDir = Join-Path $repoRoot "secrets"

if (-not (Test-Path $secretsDir)) {
    New-Item -ItemType Directory -Path $secretsDir | Out-Null
    Write-Host "Created local-only secrets directory: $secretsDir"
} else {
    Write-Host "Local-only secrets directory already exists: $secretsDir"
}

if (-not (Test-Path $localConfig)) {
    Copy-Item $localConfigExample $localConfig
    Write-Host "Created local config file: $localConfig"
} else {
    Write-Host "Local config file already exists: $localConfig"
}

Write-Host ""
Write-Host "Next steps:"
Write-Host "1. Put your Firebase service account JSON at: $secretsDir\\firebase-service-account.json"
Write-Host "2. Edit: $localConfig"
Write-Host "3. Start the app with:"
Write-Host "   .\\mvnw.cmd spring-boot:run `"-Dspring-boot.run.profiles=local`""
