$ErrorActionPreference = "Stop"
$sourceRoot = (Resolve-Path "$PSScriptRoot\..").Path
$tools = "$sourceRoot\main\distribution\install\windows"
$temporary = Join-Path $env:TEMP "wepush-next-operations-$([Guid]::NewGuid())"
try {
  $dataRoot = "$temporary\data"
  # Windows keeps service.env and agent.env at the ProgramData data root.
  $configRoot = $dataRoot
  $installRoot = "$temporary\install"
  $backupRoot = "$temporary\backups"
  $expectedRoot = "$temporary\expected"
  New-Item -ItemType Directory -Force -Path $configRoot, "$dataRoot\service\secrets", "$dataRoot\service\artifacts\nested",
    "$dataRoot\agent\plugins\active", "$installRoot\releases\0.1.0-old", $backupRoot, $expectedRoot | Out-Null
  "WEPUSH_MODE=standalone" | Set-Content "$configRoot\service.env"
  "WEPUSH_PLUGIN_TRUSTED_KEYS=test-key" | Set-Content "$configRoot\agent.env"
  "sqlite-database" | Set-Content "$dataRoot\service\wepush-next.db"
  "master-key" | Set-Content "$dataRoot\service\secrets\master-key.json"
  "artifact" | Set-Content "$dataRoot\service\artifacts\nested\audience.csv"
  "agent-identity" | Set-Content "$dataRoot\agent\identity.json"
  "agent-journal" | Set-Content "$dataRoot\agent\journal.json"
  "event-outbox" | Set-Content "$dataRoot\agent\event-outbox"
  "completion-outbox" | Set-Content "$dataRoot\agent\completion-outbox"
  "signed-plugin" | Set-Content "$dataRoot\agent\plugins\active\provider.zip"
  New-Item -ItemType Junction -Path "$installRoot\current" -Target "$installRoot\releases\0.1.0-old" | Out-Null
  Copy-Item $configRoot "$expectedRoot\config" -Recurse
  Copy-Item $dataRoot "$expectedRoot\data" -Recurse

  $env:WEPUSH_SKIP_SERVICE_CONTROL = "true"
  $env:WEPUSH_SKIP_HEALTH_CHECK = "true"
  $env:WEPUSH_CONFIG_ROOT = $configRoot
  $env:WEPUSH_DATA_ROOT = $dataRoot
  $env:WEPUSH_INSTALL_ROOT = $installRoot
  $env:WEPUSH_BACKUP_ROOT = $backupRoot

  $archive = (& "$tools\backup.ps1" -Destination $backupRoot | Select-Object -Last 1).ToString()
  & "$tools\restore.ps1" -ValidateOnly -Archive $archive
  "corrupted-current-data" | Set-Content "$dataRoot\service\wepush-next.db"
  Remove-Item "$dataRoot\agent\identity.json", "$configRoot\agent.env"
  & "$tools\restore.ps1" -Archive $archive
  $expectedHashes = Get-ChildItem $expectedRoot -File -Recurse | ForEach-Object {
    $relative = [IO.Path]::GetRelativePath($expectedRoot, $_.FullName)
    "$relative=$((Get-FileHash $_.FullName -Algorithm SHA256).Hash)"
  } | Sort-Object
  $actualHashes = @(
    Get-ChildItem $configRoot -File -Recurse | ForEach-Object {
      "config\$([IO.Path]::GetRelativePath($configRoot, $_.FullName))=$((Get-FileHash $_.FullName -Algorithm SHA256).Hash)"
    }
    Get-ChildItem $dataRoot -File -Recurse | ForEach-Object {
      "data\$([IO.Path]::GetRelativePath($dataRoot, $_.FullName))=$((Get-FileHash $_.FullName -Algorithm SHA256).Hash)"
    }
  ) | Sort-Object
  if (Compare-Object $expectedHashes $actualHashes) { throw "Restored backup differs from expected content" }

  $corruptRoot = "$temporary\corrupt"
  Expand-Archive $archive $corruptRoot
  Add-Content "$corruptRoot\payload\data\service\wepush-next.db" "tampered"
  $corruptArchive = "$temporary\corrupt.zip"
  Compress-Archive "$corruptRoot\*" $corruptArchive
  $corruptionAccepted = $false
  try { & "$tools\restore.ps1" -ValidateOnly -Archive $corruptArchive; $corruptionAccepted = $true } catch { }
  if ($corruptionAccepted) { throw "Corrupted backup unexpectedly validated" }

  $extraRoot = "$temporary\extra"
  Expand-Archive $archive $extraRoot
  "unlisted" | Set-Content "$extraRoot\payload\data\unlisted.txt"
  $extraArchive = "$temporary\extra.zip"
  Compress-Archive "$extraRoot\*" $extraArchive
  $extraAccepted = $false
  try { & "$tools\restore.ps1" -ValidateOnly -Archive $extraArchive; $extraAccepted = $true } catch { }
  if ($extraAccepted) { throw "Backup with an unlisted payload unexpectedly validated" }

  $upgradeRoot = "$temporary\upgrade\wepush-next-0.1.0-test"
  New-Item -ItemType Directory -Force -Path "$upgradeRoot\install\windows" | Out-Null
  @'
param([string]$Component = "standalone")
$ErrorActionPreference = "Stop"
New-Item -ItemType Directory -Force -Path "$env:WEPUSH_INSTALL_ROOT\releases\0.1.0-test" | Out-Null
Remove-Item "$env:WEPUSH_INSTALL_ROOT\current" -Force
New-Item -ItemType Junction -Path "$env:WEPUSH_INSTALL_ROOT\current" -Target "$env:WEPUSH_INSTALL_ROOT\releases\0.1.0-test" | Out-Null
"upgrade-mutated-data" | Set-Content "$env:WEPUSH_DATA_ROOT\service\wepush-next.db"
'@ | Set-Content "$upgradeRoot\install\windows\install.ps1"
  'throw "forced installation health failure"' | Set-Content "$upgradeRoot\install\verify-install.ps1"
  $upgradeArchive = "$temporary\upgrade.zip"
  Compress-Archive $upgradeRoot $upgradeArchive
  $upgradeSha = (Get-FileHash $upgradeArchive -Algorithm SHA256).Hash
  $upgradeAccepted = $false
  try { & "$tools\upgrade.ps1" -Archive $upgradeArchive -ExpectedSha256 $upgradeSha; $upgradeAccepted = $true } catch { }
  if ($upgradeAccepted) { throw "Forced failed upgrade unexpectedly succeeded" }
  $target = (Get-Item "$installRoot\current" -Force).Target
  if ([IO.Path]::GetFullPath($target) -ne [IO.Path]::GetFullPath("$installRoot\releases\0.1.0-old")) {
    throw "Failed upgrade did not restore the previous release"
  }
  if ((Get-Content "$dataRoot\service\wepush-next.db" -Raw).Trim() -ne "sqlite-database") {
    throw "Failed upgrade did not restore the previous database"
  }
  Write-Host "Backup validation, full restore and failed-upgrade rollback passed on windows"
} finally {
  Remove-Item $temporary -Recurse -Force -ErrorAction SilentlyContinue
}
