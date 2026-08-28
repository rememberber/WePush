param([string]$Destination = "")
$ErrorActionPreference = "Stop"
$dataRoot = if ($env:WEPUSH_DATA_ROOT) { $env:WEPUSH_DATA_ROOT } else { "$env:ProgramData\WePush Next" }
$installRoot = if ($env:WEPUSH_INSTALL_ROOT) { $env:WEPUSH_INSTALL_ROOT } else { "$env:ProgramFiles\WePush Next" }
if (-not $Destination) { $Destination = if ($env:WEPUSH_BACKUP_ROOT) { $env:WEPUSH_BACKUP_ROOT } else { "$env:ProgramData\WePush Next Backups" } }
if (-not (Test-Path $dataRoot -PathType Container)) { throw "WePush data directory is missing" }
New-Item -ItemType Directory -Force -Path $Destination | Out-Null
$resolvedDataRoot = [IO.Path]::GetFullPath($dataRoot).TrimEnd('\')
$resolvedDestination = [IO.Path]::GetFullPath((Resolve-Path $Destination).Path).TrimEnd('\')
if ($resolvedDestination.Equals($resolvedDataRoot, [StringComparison]::OrdinalIgnoreCase) -or
    $resolvedDestination.StartsWith("$resolvedDataRoot\", [StringComparison]::OrdinalIgnoreCase)) {
  throw "Backup destination must not be inside the data directory"
}
$running = @()
if ($env:WEPUSH_SKIP_SERVICE_CONTROL -ne "true") {
  foreach ($name in @("WePushNextService", "WePushNextAgent")) {
    $service = Get-Service $name -ErrorAction SilentlyContinue
    if ($service -and $service.Status -eq "Running") { Stop-Service $name; $running += $name }
  }
}
$temporary = Join-Path $env:TEMP "wepush-next-backup-$([Guid]::NewGuid())"
try {
  $payload = "$temporary\payload"
  New-Item -ItemType Directory -Force -Path "$payload\data" | Out-Null
  Get-ChildItem $dataRoot -Force | Copy-Item -Destination "$payload\data" -Recurse -Force
  $version = "unknown"
  $current = "$installRoot\current"
  if (Test-Path $current) { $version = Split-Path (Get-Item $current -Force).Target -Leaf }
  $stamp = (Get-Date).ToUniversalTime().ToString("yyyyMMddTHHmmssZ")
  @(
    "format=wepush-next-backup-v1",
    "platform=windows",
    "productVersion=$version",
    "createdAt=$stamp",
    "contents=config,database,master-key,artifacts,agent-identity,journal,outbox,plugins"
  ) | Set-Content -Encoding ascii "$temporary\BACKUP-MANIFEST"
  $checksums = Get-ChildItem $payload -File -Recurse | Sort-Object FullName | ForEach-Object {
    $relative = [IO.Path]::GetRelativePath($temporary, $_.FullName).Replace("\", "/")
    "$((Get-FileHash $_.FullName -Algorithm SHA256).Hash.ToLowerInvariant())  $relative"
  }
  $checksums | Set-Content -Encoding ascii "$temporary\SHA256SUMS"
  $archive = Join-Path $Destination "wepush-next-$stamp.zip"
  Compress-Archive -Path "$temporary\*" -DestinationPath $archive -CompressionLevel Optimal
  & "$PSScriptRoot\restore.ps1" -ValidateOnly -Archive $archive
  Write-Output $archive
} finally {
  Remove-Item $temporary -Recurse -Force -ErrorAction SilentlyContinue
  foreach ($name in $running) { Start-Service $name }
}
