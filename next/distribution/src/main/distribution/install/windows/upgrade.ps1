param([Parameter(Mandatory=$true)][string]$Archive, [Parameter(Mandatory=$true)][string]$ExpectedSha256)
$ErrorActionPreference = "Stop"
$actual = (Get-FileHash $Archive -Algorithm SHA256).Hash.ToLowerInvariant()
if ($actual -ne $ExpectedSha256.ToLowerInvariant()) { throw "Release SHA-256 mismatch" }
$installRoot = if ($env:WEPUSH_INSTALL_ROOT) { $env:WEPUSH_INSTALL_ROOT } else { "$env:ProgramFiles\WePush Next" }
$current = "$installRoot\current"
$previous = if (Test-Path $current) { (Get-Item $current -Force).Target } else { $null }
$backup = (& "$PSScriptRoot\backup.ps1" | Select-Object -Last 1).ToString()
$temporary = Join-Path $env:TEMP "wepush-next-upgrade-$([Guid]::NewGuid())"
New-Item -ItemType Directory $temporary | Out-Null
try {
  Expand-Archive $Archive $temporary
  $release = Get-ChildItem $temporary -Directory | Select-Object -First 1
  $version = $release.Name -replace '^wepush-next-', ''
  try {
    & "$($release.FullName)\install\windows\install.ps1" -Component standalone
    & "$($release.FullName)\install\verify-install.ps1" -ExpectedVersion $version
    Write-Host "Upgrade to $version verified; backup retained at $backup"
  } catch {
    if ($previous) {
      Remove-Item $current -Force -ErrorAction SilentlyContinue
      New-Item -ItemType Junction -Path $current -Target $previous | Out-Null
    }
    $env:WEPUSH_EXPECTED_VERSION = ""
    & "$PSScriptRoot\restore.ps1" -Archive $backup
    throw "Upgrade verification failed; previous release and data were restored"
  }
} finally { Remove-Item $temporary -Recurse -Force -ErrorAction SilentlyContinue }
