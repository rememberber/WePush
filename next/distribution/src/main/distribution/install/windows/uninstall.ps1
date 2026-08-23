param([switch]$Purge)
$ErrorActionPreference = "Stop"
$wrapperRoot = "$env:ProgramData\WePush Next\winsw"
foreach ($name in @("WePushNextService", "WePushNextAgent")) {
  $exe = "$wrapperRoot\$name.exe"
  if (Test-Path $exe) { & $exe stop 2>$null; & $exe uninstall 2>$null }
}
Remove-Item "$env:ProgramFiles\WePush Next" -Recurse -Force -ErrorAction SilentlyContinue
Remove-Item $wrapperRoot -Recurse -Force -ErrorAction SilentlyContinue
if ($Purge) {
  Write-Warning "Purging WePush Next configuration and data"
  Remove-Item "$env:ProgramData\WePush Next" -Recurse -Force -ErrorAction SilentlyContinue
} else { Write-Host "Services and binaries removed; configuration and data preserved. Pass -Purge to remove them." }
