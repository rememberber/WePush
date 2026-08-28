param([Parameter(Mandatory=$true)][string]$Name)
$ErrorActionPreference = "Stop"
$root = if ($env:WEPUSH_PLUGIN_ROOT) { $env:WEPUSH_PLUGIN_ROOT } else { "$env:ProgramData\WePush Next\agent\plugins" }
$nameOnly = [IO.Path]::GetFileName($Name)
$previous = Get-ChildItem "$root\rollback" -File -Filter "$nameOnly.*" -ErrorAction SilentlyContinue |
  Sort-Object Name | Select-Object -Last 1
if (-not $previous) { throw "No rollback version found for $nameOnly" }
New-Item -ItemType Directory -Force -Path "$root\active", "$root\rollback" | Out-Null
$active = "$root\active\$nameOnly"
$current = $null
if (Test-Path $active) {
  $current = "$root\rollback\$nameOnly.$((Get-Date).ToUniversalTime().ToString('yyyyMMddTHHmmssZ')).replaced"
  Move-Item $active $current
}
Move-Item $previous.FullName $active
try {
  if ($env:WEPUSH_SKIP_SERVICE_CONTROL -ne "true") {
    Restart-Service WePushNextAgent
    Start-Sleep -Seconds 5
    if ((Get-Service WePushNextAgent).Status -ne "Running") { throw "Agent failed after plugin rollback" }
  }
} catch {
  Move-Item $active $previous.FullName -Force
  if ($current) { Move-Item $current $active -Force }
  if ($env:WEPUSH_SKIP_SERVICE_CONTROL -ne "true") { Restart-Service WePushNextAgent -ErrorAction SilentlyContinue }
  throw "Plugin rollback failed; active version was recovered: $($_.Exception.Message)"
}
Write-Host "Rolled back and verified $nameOnly"
