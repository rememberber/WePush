param([Parameter(ValueFromRemainingArguments=$true)][string[]]$AgentArguments = @())
$ErrorActionPreference = "Stop"
$config = if ($env:WEPUSH_AGENT_ENV) { $env:WEPUSH_AGENT_ENV } else { "$env:ProgramData\WePush Next\agent.env" }
Get-Content $config | ForEach-Object {
  $line = $_.Trim()
  if ($line -and -not $line.StartsWith("#")) {
    $parts = $line.Split("=", 2)
    if ($parts.Count -eq 2) { [Environment]::SetEnvironmentVariable($parts[0].Trim(), $parts[1], "Process") }
  }
}
$root = (Resolve-Path "$PSScriptRoot\..\..").Path
$java = if ($env:WEPUSH_JAVA) { $env:WEPUSH_JAVA } elseif (Test-Path "$root\runtime\bin\java.exe") { "$root\runtime\bin\java.exe" } else { "java.exe" }
$javaOptions = if ($env:WEPUSH_JAVA_OPTS) { $env:WEPUSH_JAVA_OPTS.Split(" ", [System.StringSplitOptions]::RemoveEmptyEntries) } else { @() }
& $java --enable-native-access=ALL-UNNAMED @javaOptions -jar "$root\lib\wepush-next-agent.jar" @AgentArguments
exit $LASTEXITCODE
