param([ValidateSet("standalone", "service", "agent", "all")][string]$Component = "standalone")
$ErrorActionPreference = "Stop"
if ($Component -eq "standalone") { $Component = "service" }
if ($env:WEPUSH_SKIP_SERVICE_CONTROL -ne "true") {
  $identity = [Security.Principal.WindowsIdentity]::GetCurrent()
  $principal = New-Object Security.Principal.WindowsPrincipal($identity)
  if (-not $principal.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)) { throw "Run PowerShell as Administrator" }
}
$source = (Resolve-Path "$PSScriptRoot\..\..").Path
$version = Split-Path $source -Leaf
$installRoot = if ($env:WEPUSH_INSTALL_ROOT) { $env:WEPUSH_INSTALL_ROOT } else { "$env:ProgramFiles\WePush Next" }
$release = "$installRoot\releases\$version"
$dataRoot = if ($env:WEPUSH_DATA_ROOT) { $env:WEPUSH_DATA_ROOT } else { "$env:ProgramData\WePush Next" }
$wrapperRoot = "$dataRoot\winsw"
$java = if ($env:WEPUSH_JAVA) { $env:WEPUSH_JAVA } elseif (Test-Path "$source\runtime\bin\java.exe") { "$source\runtime\bin\java.exe" } else { "java.exe" }
$javaVersion = (& $java -version 2>&1 | Select-Object -First 1).ToString()
if ($javaVersion -notmatch 'version "(\d+)') { throw "Java 21+ is required" }
if ([int]$Matches[1] -lt 21) { throw "Java 21+ is required" }
New-Item -ItemType Directory -Force -Path "$installRoot\releases", $dataRoot, "$dataRoot\service", "$dataRoot\agent", "$dataRoot\agent\plugins\active", "$dataRoot\logs", $wrapperRoot | Out-Null
if ($env:WEPUSH_SKIP_SERVICE_CONTROL -ne "true") {
  & icacls.exe $dataRoot /inheritance:r /grant:r '*S-1-5-18:(OI)(CI)F' '*S-1-5-32-544:(OI)(CI)F' '*S-1-5-19:(OI)(CI)M' | Out-Null
  if ($LASTEXITCODE -ne 0) { throw "Unable to secure WePush Next ProgramData ACL" }
}
if (-not (Test-Path $release)) { Copy-Item $source $release -Recurse }
$current = "$installRoot\current"
if (Test-Path $current) {
  $item = Get-Item $current -Force
  if (-not $item.LinkType) { throw "Refusing to replace non-link path: $current" }
  Remove-Item $current -Force
}
New-Item -ItemType Junction -Path $current -Target $release | Out-Null
$serviceConfig = "$dataRoot\service.env"
if (-not (Test-Path $serviceConfig)) {
@"
WEPUSH_MODE=standalone
WEPUSH_BIND_ADDRESS=127.0.0.1
WEPUSH_PORT=18990
WEPUSH_DATABASE_KIND=sqlite
WEPUSH_DATABASE_PATH=$dataRoot\service\wepush-next.db
WEPUSH_ARTIFACT_KIND=local
WEPUSH_ARTIFACT_ROOT=$dataRoot\service\artifacts
WEPUSH_MASTER_KEY_PATH=$dataRoot\service\secrets\master-key.json
WEPUSH_AGENT_CA_KEY_PATH=$dataRoot\service\agent-ca\ca-key.pem
WEPUSH_AGENT_CA_CERTIFICATE_PATH=$dataRoot\service\agent-ca\ca-certificate.pem
WEPUSH_JAVA_OPTS=-XX:MaxRAMPercentage=75
"@ | Set-Content -Encoding UTF8 $serviceConfig
}
$agentConfig = "$dataRoot\agent.env"
if (-not (Test-Path $agentConfig)) {
@"
WEPUSH_AGENT_ID=agent-local
WEPUSH_SERVICE_HOST=127.0.0.1
WEPUSH_SERVICE_HTTP_PORT=18990
WEPUSH_SERVICE_BASE_URL=http://127.0.0.1:18990
WEPUSH_AGENT_GRPC_PORT=19090
WEPUSH_AGENT_GRPC_PLAINTEXT=true
WEPUSH_AGENT_IDENTITY_PATH=$dataRoot\agent\identity.json
WEPUSH_AGENT_STATE_PATH=$dataRoot\agent\journal.json
WEPUSH_AGENT_EVENT_OUTBOX_PATH=$dataRoot\agent\event-outbox
WEPUSH_AGENT_COMPLETION_OUTBOX_PATH=$dataRoot\agent\completion-outbox
WEPUSH_PLUGIN_ACTIVE_PATH=$dataRoot\agent\plugins\active
WEPUSH_PLUGIN_DEVELOPER_MODE=false
WEPUSH_JAVA_OPTS=-XX:MaxRAMPercentage=75
"@ | Set-Content -Encoding UTF8 $agentConfig
}
$bundledWinsw = "$source\install\windows\WinSW-x64.exe"
if (-not (Test-Path $bundledWinsw -PathType Leaf)) { throw "Offline WinSW wrapper is missing from the release" }
$expectedLength = 18243033
$expectedSha256 = "05b82d46ad331cc16bdc00de5c6332c1ef818df8ceefcd49c726553209b3a0da"
if ((Get-Item $bundledWinsw).Length -ne $expectedLength) { throw "Bundled WinSW file length mismatch" }
if ((Get-FileHash $bundledWinsw -Algorithm SHA256).Hash.ToLowerInvariant() -ne $expectedSha256) { throw "Bundled WinSW SHA-256 mismatch" }
if ($env:WEPUSH_SKIP_SERVICE_CONTROL -ne "true") {
  $winsw = "$wrapperRoot\WinSW-x64.exe"
  Copy-Item $bundledWinsw $winsw -Force
  foreach ($unit in @("Service", "Agent")) {
    if ($Component -eq "all" -or $Component -eq $unit.ToLowerInvariant()) {
      $name = "WePushNext$unit"
      $exe = "$wrapperRoot\$name.exe"
      Copy-Item $winsw $exe -Force
      Copy-Item "$source\install\windows\$name.xml" "$wrapperRoot\$name.xml" -Force
      & $exe stop 2>$null
      & $exe uninstall 2>$null
      & $exe install
      & $exe start
    }
  }
}
Write-Host "Installed WePush Next $version ($Component)"
