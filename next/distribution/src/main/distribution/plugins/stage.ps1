param([Parameter(Mandatory=$true)][string]$Package)
$ErrorActionPreference = "Stop"
if (-not (Test-Path $Package -PathType Leaf)) { throw "Plugin package not found" }
if ([IO.Path]::GetExtension($Package) -ne ".zip") { throw "Plugin package must end in .zip" }
$root = if ($env:WEPUSH_PLUGIN_ROOT) { $env:WEPUSH_PLUGIN_ROOT } else { "$env:ProgramData\WePush Next\agent\plugins" }
$staging = Join-Path $root "staging"
New-Item -ItemType Directory -Force -Path $staging | Out-Null
$target = Join-Path $staging ([IO.Path]::GetFileName($Package))
Copy-Item $Package "$target.tmp" -Force
Move-Item "$target.tmp" $target -Force
Get-FileHash $target -Algorithm SHA256
