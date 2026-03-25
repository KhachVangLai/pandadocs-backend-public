param()

$ErrorActionPreference = "Stop"

if (-not (Get-Command git -ErrorAction SilentlyContinue)) {
    throw "git is required to run this check."
}

if (-not (Get-Command rg -ErrorAction SilentlyContinue)) {
    throw "rg (ripgrep) is required to run this check."
}

$repoRoot = Split-Path -Parent $PSScriptRoot
Set-Location $repoRoot

$trackedFiles = @(git ls-files 2>$null)
if ($LASTEXITCODE -ne 0) {
    throw "git ls-files failed. If Git reports dubious ownership, run: git config --global --add safe.directory D:/FPTU/Panda_Clone_Real/pandadocs-backend-public"
}

if ($trackedFiles.Count -eq 0) {
    throw "No tracked files found. Run this inside the public git repo."
}

$forbiddenFiles = @(
    "^\\.env$",
    "^\\.env\\.",
    "^secrets/",
    "firebase-service-account.*\\.json$",
    "application\\.properties\\.backup$",
    "pom\\.xml\\.backup$",
    "\\.(pem|p12|jks|keystore)$"
)

$contentPatterns = @(
    "AIza[0-9A-Za-z_-]{20,}",
    "GOCSPX-[0-9A-Za-z_-]{10,}",
    "AKIA[0-9A-Z]{16}",
    "-----BEGIN [A-Z ]*PRIVATE KEY-----",
    "eyJhbGciOiJIUzI1Ni"
)

$findings = @()

foreach ($file in $trackedFiles) {
    foreach ($pattern in $forbiddenFiles) {
        if ($file -match $pattern) {
            $findings += "Forbidden tracked file: $file"
        }
    }
}

foreach ($pattern in $contentPatterns) {
    $matches = & rg --line-number --with-filename --glob "!*.md" --glob "!*.example" --glob "!*.txt" --glob "!docs/**" -e $pattern -- $trackedFiles 2>$null
    if ($LASTEXITCODE -eq 0 -and $matches) {
        $findings += $matches
    }
}

if ($findings.Count -gt 0) {
    Write-Host "Public release check failed. Review these findings:" -ForegroundColor Red
    $findings | Sort-Object -Unique | ForEach-Object { Write-Host " - $_" }
    exit 1
}

Write-Host "Public release check passed." -ForegroundColor Green
Write-Host "No forbidden tracked files or high-signal secret patterns were found."
