# Rebuild demo database for property-workorder (WARNING: drops all data in prop_wo)
param(
    [string]$User = "root",
    [Parameter(Mandatory = $true)][string]$Password,
    [string]$HostName = "127.0.0.1",
    [int]$Port = 3306
)

$ErrorActionPreference = "Stop"
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
Push-Location $scriptDir

Write-Host "==> Rebuilding database prop_wo at $HostName`:$Port" -ForegroundColor Yellow

& mysql -h $HostName -P $Port -u $User "-p$Password" --default-character-set=utf8mb4 -e "source init.sql"
if ($LASTEXITCODE -ne 0) {
    Pop-Location
    throw "init.sql failed, check MySQL connection and root password"
}

& mysql -h $HostName -P $Port -u $User "-p$Password" --default-character-set=utf8mb4 -e "source data.sql"
if ($LASTEXITCODE -ne 0) {
    Pop-Location
    throw "data.sql failed"
}

$count = & mysql -h $HostName -P $Port -u $User "-p$Password" -N -e "SELECT COUNT(1) FROM prop_wo.work_order"
Pop-Location

Write-Host "==> Done. Demo work orders: $count" -ForegroundColor Green
Write-Host "==> Restart backend to regenerate QR codes and demo watermark photos." -ForegroundColor Green
