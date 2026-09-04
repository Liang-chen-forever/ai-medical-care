param(
    [string]$NginxExe = ''
)

$ErrorActionPreference = 'Stop'
$nginxHome = (Resolve-Path (Join-Path $PSScriptRoot '.')).Path

if (-not $NginxExe) {
    $NginxExe = Join-Path $nginxHome 'nginx.exe'
}

if (-not $NginxExe -or -not (Test-Path $NginxExe)) {
    throw '未找到 frontend/nginx-1.20.2/nginx.exe。'
}

& $NginxExe -p "$nginxHome/" -c 'conf/nginx.conf' -s quit
if ($LASTEXITCODE -ne 0) {
    throw "Nginx 停止失败，退出码：$LASTEXITCODE"
}

Write-Host 'Nginx 已停止。'
