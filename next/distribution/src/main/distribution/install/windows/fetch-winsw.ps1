param([string]$Destination = "$PSScriptRoot\WinSW-x64.exe")
$ErrorActionPreference = "Stop"
$version = "2.12.0"
$expectedLength = 18243033
$expectedSha256 = "05b82d46ad331cc16bdc00de5c6332c1ef818df8ceefcd49c726553209b3a0da"
$uri = "https://github.com/winsw/winsw/releases/download/v$version/WinSW-x64.exe"
$temporary = "$Destination.download"
Invoke-WebRequest -UseBasicParsing -Uri $uri -OutFile $temporary
if ((Get-Item $temporary).Length -ne $expectedLength) { Remove-Item $temporary -Force; throw "WinSW file length mismatch" }
$actual = (Get-FileHash $temporary -Algorithm SHA256).Hash.ToLowerInvariant()
if ($actual -ne $expectedSha256) { Remove-Item $temporary -Force; throw "WinSW SHA-256 mismatch" }
Move-Item $temporary $Destination -Force
Write-Host "Fetched verified WinSW $version to $Destination"
