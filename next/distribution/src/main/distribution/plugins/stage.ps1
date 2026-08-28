param([Parameter(Mandatory=$true)][string]$Package)
$ErrorActionPreference = "Stop"
if (-not (Test-Path $Package -PathType Leaf)) { throw "Plugin package not found" }
if ([IO.Path]::GetExtension($Package) -ne ".zip") { throw "Plugin package must end in .zip" }
$verificationOutput = & "$PSScriptRoot\..\install\windows\run-agent.ps1" -AgentArguments @("--verify-plugin", (Resolve-Path $Package).Path)
if ($LASTEXITCODE -ne 0) { throw "Provider plugin signature or content verification failed" }
$verification = $verificationOutput | ConvertFrom-Json
if (-not $verification.valid -or $verification.canonicalName -notmatch '^[A-Za-z0-9._-]+\.zip$') { throw "Verified plugin did not return a canonical name" }
$root = if ($env:WEPUSH_PLUGIN_ROOT) { $env:WEPUSH_PLUGIN_ROOT } else { "$env:ProgramData\WePush Next\agent\plugins" }
$staging = Join-Path $root "staging"
New-Item -ItemType Directory -Force -Path $staging | Out-Null
$target = Join-Path $staging $verification.canonicalName
Copy-Item $Package "$target.tmp" -Force
Move-Item "$target.tmp" $target -Force
Get-FileHash $target -Algorithm SHA256
Write-Output ($verification | ConvertTo-Json -Compress)
Write-Host "Verified signature and staged $($verification.canonicalName)"
