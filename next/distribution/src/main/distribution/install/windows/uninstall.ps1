param([switch]$Purge)
$ErrorActionPreference = "Stop"
$installRoot = if ($env:WEPUSH_INSTALL_ROOT) { $env:WEPUSH_INSTALL_ROOT } else { "$env:ProgramFiles\WePush Next" }
$dataRoot = if ($env:WEPUSH_DATA_ROOT) { $env:WEPUSH_DATA_ROOT } else { "$env:ProgramData\WePush Next" }
$wrapperRoot = "$dataRoot\winsw"
if ($env:WEPUSH_SKIP_SERVICE_CONTROL -ne "true") {
  foreach ($name in @("WePushNextService", "WePushNextAgent")) {
    $exe = "$wrapperRoot\$name.exe"
    if (Test-Path $exe) { & $exe stop 2>$null; & $exe uninstall 2>$null }
  }
}
Remove-Item $installRoot -Recurse -Force -ErrorAction SilentlyContinue
Remove-Item $wrapperRoot -Recurse -Force -ErrorAction SilentlyContinue
if ($Purge) {
  Write-Warning "Purging WePush Next configuration and data"
  Remove-Item $dataRoot -Recurse -Force -ErrorAction SilentlyContinue
} else { Write-Host "Services and binaries removed; configuration and data preserved. Pass -Purge to remove them." }
