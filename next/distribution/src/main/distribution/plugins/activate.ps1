param([Parameter(Mandatory=$true)][string]$Name)
$ErrorActionPreference = "Stop"
$root = if ($env:WEPUSH_PLUGIN_ROOT) { $env:WEPUSH_PLUGIN_ROOT } else { "$env:ProgramData\WePush Next\agent\plugins" }
$nameOnly = [IO.Path]::GetFileName($Name)
$staged = Join-Path "$root\staging" $nameOnly
$active = Join-Path "$root\active" $nameOnly
if (-not (Test-Path $staged -PathType Leaf)) { throw "Staged plugin not found: $nameOnly" }
New-Item -ItemType Directory -Force -Path "$root\active", "$root\rollback" | Out-Null
$previous = $null
if (Test-Path $active) {
  $previous = "$root\rollback\$nameOnly.$((Get-Date).ToUniversalTime().ToString('yyyyMMddTHHmmssZ'))"
  Move-Item $active $previous
}
Move-Item $staged $active
try {
  Restart-Service WePushNextAgent
  Start-Sleep -Seconds 5
  if ((Get-Service WePushNextAgent).Status -ne "Running") { throw "Agent failed after plugin activation" }
} catch {
  Move-Item $active "$staged.failed" -Force
  if ($previous) { Move-Item $previous $active -Force }
  Restart-Service WePushNextAgent -ErrorAction SilentlyContinue
  throw "Plugin activation failed; previous version was restored: $($_.Exception.Message)"
}
Write-Host "Activated and verified $nameOnly"
