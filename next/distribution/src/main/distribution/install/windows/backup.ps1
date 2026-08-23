param([string]$Destination = "$env:ProgramData\WePush Next\backups")
$ErrorActionPreference = "Stop"
New-Item -ItemType Directory -Force -Path $Destination | Out-Null
$running = @()
foreach ($name in @("WePushNextService", "WePushNextAgent")) {
  $service = Get-Service $name -ErrorAction SilentlyContinue
  if ($service -and $service.Status -eq "Running") { Stop-Service $name; $running += $name }
}
try {
  $stamp = (Get-Date).ToUniversalTime().ToString("yyyyMMddTHHmmssZ")
  $archive = Join-Path $Destination "wepush-next-$stamp.zip"
  Compress-Archive -Path "$env:ProgramData\WePush Next\service.env", "$env:ProgramData\WePush Next\agent.env", "$env:ProgramData\WePush Next\service", "$env:ProgramData\WePush Next\agent" -DestinationPath $archive
  Write-Host $archive
} finally { foreach ($name in $running) { Start-Service $name } }
