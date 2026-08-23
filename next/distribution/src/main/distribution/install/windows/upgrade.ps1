param([Parameter(Mandatory=$true)][string]$Archive, [Parameter(Mandatory=$true)][string]$ExpectedSha256)
$ErrorActionPreference = "Stop"
$actual = (Get-FileHash $Archive -Algorithm SHA256).Hash.ToLowerInvariant()
if ($actual -ne $ExpectedSha256.ToLowerInvariant()) { throw "Release SHA-256 mismatch" }
& "$PSScriptRoot\backup.ps1"
$temporary = Join-Path $env:TEMP "wepush-next-upgrade-$([Guid]::NewGuid())"
New-Item -ItemType Directory $temporary | Out-Null
try {
  Expand-Archive $Archive $temporary
  $release = Get-ChildItem $temporary -Directory | Select-Object -First 1
  & "$($release.FullName)\install\windows\install.ps1" -Component all
} finally { Remove-Item $temporary -Recurse -Force }
