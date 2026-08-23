param([ValidateSet("service", "agent", "all")][string]$Component = "all")
$ErrorActionPreference = "Stop"
$identity = [Security.Principal.WindowsIdentity]::GetCurrent()
$principal = New-Object Security.Principal.WindowsPrincipal($identity)
if (-not $principal.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)) { throw "Run PowerShell as Administrator" }
$source = (Resolve-Path "$PSScriptRoot\..\..").Path
$version = Split-Path $source -Leaf
$installRoot = "$env:ProgramFiles\WePush Next"
$release = "$installRoot\releases\$version"
$dataRoot = "$env:ProgramData\WePush Next"
$wrapperRoot = "$dataRoot\winsw"
$javaVersion = (& java.exe -version 2>&1 | Select-Object -First 1).ToString()
if ($javaVersion -notmatch 'version "(\d+)') { throw "Java 21+ is required" }
if ([int]$Matches[1] -lt 21) { throw "Java 21+ is required" }
New-Item -ItemType Directory -Force -Path "$installRoot\releases", $dataRoot, "$dataRoot\service", "$dataRoot\agent", "$dataRoot\agent\plugins\active", "$dataRoot\logs", $wrapperRoot | Out-Null
& icacls.exe $dataRoot /inheritance:r /grant:r '*S-1-5-18:(OI)(CI)F' '*S-1-5-32-544:(OI)(CI)F' '*S-1-5-19:(OI)(CI)M' | Out-Null
if ($LASTEXITCODE -ne 0) { throw "Unable to secure WePush Next ProgramData ACL" }
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
$winsw = "$wrapperRoot\WinSW-x64.exe"
if (-not (Test-Path $winsw)) { & "$source\install\windows\fetch-winsw.ps1" -Destination $winsw }
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
Write-Host "Installed WePush Next $version ($Component)"
