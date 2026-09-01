param(
    [string]$NginxExe = ''
)

$ErrorActionPreference = 'Stop'
$nginxHome = (Resolve-Path (Join-Path $PSScriptRoot '.')).Path

if (-not $NginxExe) {
    $candidates = @(
        (Join-Path $nginxHome 'nginx.exe'),
        'D:\Resume-Projects\Sky-Delivery\frontend\nginx-1.20.2\nginx.exe'
    )

    $NginxExe = $candidates | Where-Object { Test-Path $_ } | Select-Object -First 1
}

if (-not $NginxExe -or -not (Test-Path $NginxExe)) {
    throw '未找到 nginx.exe。请使用与启动时相同的 -NginxExe 路径。'
}

& $NginxExe -p "$nginxHome/" -c 'conf/nginx.conf' -s quit
if ($LASTEXITCODE -ne 0) {
    throw "Nginx 停止失败，退出码：$LASTEXITCODE"
}

Write-Host 'Nginx 已停止。'
