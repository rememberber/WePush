param(
  [Parameter(Mandatory=$true)][string]$Archive,
  [string]$ExpectedSha256 = "",
  [switch]$ValidateOnly
)
$ErrorActionPreference = "Stop"
if (-not (Test-Path $Archive -PathType Leaf)) { throw "Backup archive not found" }
if ($ExpectedSha256) {
  $actual = (Get-FileHash $Archive -Algorithm SHA256).Hash.ToLowerInvariant()
  if ($actual -ne $ExpectedSha256.ToLowerInvariant()) { throw "Backup SHA-256 mismatch" }
}
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [IO.Compression.ZipFile]::OpenRead((Resolve-Path $Archive))
$archiveFiles = [Collections.Generic.HashSet[string]]::new([StringComparer]::Ordinal)
try {
  foreach ($entry in $zip.Entries) {
    $name = $entry.FullName.Replace("\", "/")
    if ([IO.Path]::IsPathRooted($name) -or $name.Contains(":") -or $name -match '(^|/)\.\.(/|$)') {
      throw "Unsafe backup entry: $name"
    }
    if ($entry.Name -and -not $archiveFiles.Add($name)) { throw "Duplicate backup entry: $name" }
  }
} finally { $zip.Dispose() }
$temporary = Join-Path $env:TEMP "wepush-next-restore-$([Guid]::NewGuid())"
New-Item -ItemType Directory $temporary | Out-Null
try {
  Expand-Archive $Archive $temporary
  $manifest = Get-Content "$temporary\BACKUP-MANIFEST"
  if ($manifest -notcontains "format=wepush-next-backup-v1") { throw "Unsupported backup format" }
  if ($manifest -notcontains "platform=windows") { throw "Backup platform mismatch" }
  $manifestedFiles = [Collections.Generic.HashSet[string]]::new([StringComparer]::Ordinal)
  $temporaryRoot = [IO.Path]::GetFullPath($temporary).TrimEnd('\')
  foreach ($line in Get-Content "$temporary\SHA256SUMS") {
    $line = $line.TrimStart([char]0xfeff)
    if ($line.Length -lt 67 -or $line.Substring(64, 2) -ne "  ") { throw "Invalid backup checksum manifest line" }
    $expectedDigest = $line.Substring(0, 64)
    $relative = $line.Substring(66)
    if ($expectedDigest -notmatch '^[0-9a-f]{64}$' -or
        -not $relative.StartsWith("payload/data/", [StringComparison]::Ordinal)) {
      throw "Invalid backup checksum manifest line: $line"
    }
    if ($relative.Contains("\") -or $relative -match '(^|/)\.\.(/|$)' -or -not $manifestedFiles.Add($relative)) {
      throw "Unsafe or duplicate backup checksum path: $relative"
    }
    $path = Join-Path $temporary $relative.Replace('/', '\')
    $resolvedPath = [IO.Path]::GetFullPath($path)
    if (-not $resolvedPath.StartsWith("$temporaryRoot\", [StringComparison]::Ordinal)) { throw "Unsafe backup checksum path: $relative" }
    if (-not (Test-Path $resolvedPath -PathType Leaf)) { throw "Backup file is missing: $relative" }
    if ((Get-FileHash $path -Algorithm SHA256).Hash.ToLowerInvariant() -ne $expectedDigest) {
      throw "Backup file checksum differs: $relative"
    }
  }
  if (-not (Test-Path "$temporary\payload\data" -PathType Container)) { throw "Backup payload is incomplete" }
  $actualFiles = Get-ChildItem "$temporary\payload" -File -Recurse | ForEach-Object {
    $_.FullName.Substring($temporary.Length + 1).Replace("\", "/")
  }
  if (Compare-Object @($manifestedFiles) $actualFiles -CaseSensitive) { throw "Backup payload file set differs from SHA256SUMS" }
  $expectedArchiveFiles = [Collections.Generic.HashSet[string]]::new($manifestedFiles, [StringComparer]::Ordinal)
  [void]$expectedArchiveFiles.Add("BACKUP-MANIFEST")
  [void]$expectedArchiveFiles.Add("SHA256SUMS")
  if (Compare-Object @($expectedArchiveFiles) @($archiveFiles) -CaseSensitive) { throw "Backup archive contains an unlisted file" }
  if ($ValidateOnly) { Write-Host "Backup validated: manifest, paths and all file checksums are valid"; return }

  $dataRoot = if ($env:WEPUSH_DATA_ROOT) { $env:WEPUSH_DATA_ROOT } else { "$env:ProgramData\WePush Next" }
  $backupRoot = if ($env:WEPUSH_BACKUP_ROOT) { $env:WEPUSH_BACKUP_ROOT } else { "$env:ProgramData\WePush Next Backups" }
  $resolvedDataRoot = [IO.Path]::GetFullPath($dataRoot).TrimEnd('\')
  $volumeRoot = [IO.Path]::GetPathRoot($resolvedDataRoot).TrimEnd('\')
  if ($resolvedDataRoot.Equals($volumeRoot, [StringComparison]::OrdinalIgnoreCase)) { throw "Refusing unsafe data root" }
  New-Item -ItemType Directory -Force -Path $backupRoot | Out-Null
  $recovery = Join-Path $backupRoot "pre-restore-$((Get-Date).ToUniversalTime().ToString('yyyyMMddTHHmmssZ'))-$PID"
  $resolvedRecovery = [IO.Path]::GetFullPath($recovery)
  if ($resolvedRecovery.StartsWith("$resolvedDataRoot\", [StringComparison]::OrdinalIgnoreCase)) { throw "Backup root must not be inside the restored path" }
  New-Item -ItemType Directory -Force -Path $recovery | Out-Null
  $running = @()
  if ($env:WEPUSH_SKIP_SERVICE_CONTROL -ne "true") {
    foreach ($name in @("WePushNextService", "WePushNextAgent")) {
      $service = Get-Service $name -ErrorAction SilentlyContinue
      if ($service -and $service.Status -eq "Running") { Stop-Service $name; $running += $name }
    }
  }
  $stage = "$dataRoot.restore.$PID"
  Remove-Item $stage -Recurse -Force -ErrorAction SilentlyContinue
  Copy-Item "$temporary\payload\data" $stage -Recurse
  if (Test-Path $dataRoot) { Move-Item $dataRoot "$recovery\data" }
  try {
    Move-Item $stage $dataRoot
    if ($env:WEPUSH_SKIP_SERVICE_CONTROL -ne "true") { foreach ($name in $running) { Start-Service $name } }
    & "$PSScriptRoot\..\verify-install.ps1" -ExpectedVersion $env:WEPUSH_EXPECTED_VERSION
  } catch {
    if ($env:WEPUSH_SKIP_SERVICE_CONTROL -ne "true") {
      foreach ($name in @("WePushNextAgent", "WePushNextService")) { Stop-Service $name -ErrorAction SilentlyContinue }
    }
    Remove-Item $dataRoot -Recurse -Force -ErrorAction SilentlyContinue
    if (Test-Path "$recovery\data") { Move-Item "$recovery\data" $dataRoot }
    if ($env:WEPUSH_SKIP_SERVICE_CONTROL -ne "true") { foreach ($name in $running) { Start-Service $name -ErrorAction SilentlyContinue } }
    throw "Restored data failed verification; original data was recovered: $($_.Exception.Message)"
  }
  Write-Host "Backup restored and verified; pre-restore data retained at $recovery"
} finally { Remove-Item $temporary -Recurse -Force -ErrorAction SilentlyContinue }
