param([string]$ExpectedVersion = "")
$ErrorActionPreference = "Stop"
if ($env:WEPUSH_SKIP_HEALTH_CHECK -eq "true") {
  Write-Host "Installation health check skipped by explicit test configuration"
  exit 0
}
$baseUrl = if ($env:WEPUSH_LOCAL_SERVICE_URL) { $env:WEPUSH_LOCAL_SERVICE_URL.TrimEnd("/") } else { "http://127.0.0.1:18990" }
$attempts = if ($env:WEPUSH_HEALTH_ATTEMPTS) { [int]$env:WEPUSH_HEALTH_ATTEMPTS } else { 60 }
for ($attempt = 0; $attempt -lt $attempts; $attempt++) {
  try {
    $installation = Invoke-RestMethod -Uri "$baseUrl/actuator/health/installation" -TimeoutSec 5
    $details = $installation.components.wePushInstallation.details
    if ($installation.status -eq "UP" -and $details.databaseVersion -and $details.dryRun -eq "DRY_RUN") {
      $system = Invoke-RestMethod -Uri "$baseUrl/api/v1/system/info" -TimeoutSec 5
      if ($ExpectedVersion -and $system.version -ne $ExpectedVersion) {
        throw "Service version $($system.version) does not match $ExpectedVersion"
      }
      Write-Host "Installation verified: readiness, database migration and Provider Dry Run are healthy"
      exit 0
    }
  } catch {
    if ($attempt -eq $attempts - 1) { throw }
  }
  Start-Sleep -Seconds 1
}
throw "Installation did not become healthy at $baseUrl"
