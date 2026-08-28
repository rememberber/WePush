param([ValidateSet("standalone", "service", "agent", "all")][string]$Component = "standalone")
$ErrorActionPreference = "Stop"
& "$PSScriptRoot\windows\install.ps1" -Component $Component
if ($Component -in @("standalone", "service", "all")) {
  & "$PSScriptRoot\verify-install.ps1" -ExpectedVersion (Split-Path (Split-Path $PSScriptRoot -Parent) -Leaf)
}
