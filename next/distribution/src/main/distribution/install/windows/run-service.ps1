$ErrorActionPreference = "Stop"
$config = if ($env:WEPUSH_SERVICE_ENV) { $env:WEPUSH_SERVICE_ENV } else { "$env:ProgramData\WePush Next\service.env" }
Get-Content $config | ForEach-Object {
  $line = $_.Trim()
  if ($line -and -not $line.StartsWith("#")) {
    $parts = $line.Split("=", 2)
    if ($parts.Count -eq 2) { [Environment]::SetEnvironmentVariable($parts[0].Trim(), $parts[1], "Process") }
  }
}
$root = (Resolve-Path "$PSScriptRoot\..\..").Path
if (-not $env:WEPUSH_WEB_ROOT -and (Test-Path "$root\web\index.html")) { $env:WEPUSH_WEB_ROOT = "file:$($root.Replace('\', '/'))/web/" }
$java = if ($env:WEPUSH_JAVA) { $env:WEPUSH_JAVA } else { "java.exe" }
$javaOptions = if ($env:WEPUSH_JAVA_OPTS) { $env:WEPUSH_JAVA_OPTS.Split(" ", [System.StringSplitOptions]::RemoveEmptyEntries) } else { @() }
& $java @javaOptions -jar "$root\lib\wepush-next-service.jar"
exit $LASTEXITCODE
