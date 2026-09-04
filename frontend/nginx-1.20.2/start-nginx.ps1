param(
    [string]$NginxExe = ''
)

$ErrorActionPreference = 'Stop'
$nginxHome = (Resolve-Path (Join-Path $PSScriptRoot '.')).Path
$configPath = Join-Path $nginxHome 'conf/nginx.conf'

if (-not $NginxExe) {
    $NginxExe = Join-Path $nginxHome 'nginx.exe'
}

if (-not $NginxExe -or -not (Test-Path $NginxExe)) {
    throw '未找到 frontend/nginx-1.20.2/nginx.exe。请将 Nginx 解压到该目录。'
}

if (-not (Test-Path (Join-Path (Split-Path $nginxHome -Parent) 'dist'))) {
    throw '未找到 frontend/dist。请先在 frontend 目录执行 npm run build。'
}

& $NginxExe -p "$nginxHome/" -c 'conf/nginx.conf' -t
if ($LASTEXITCODE -ne 0) {
    throw 'Nginx 配置校验失败，未启动服务。'
}

$nginxProcess = Start-Process -FilePath $NginxExe `
    -ArgumentList @('-p', "$nginxHome/", '-c', 'conf/nginx.conf') `
    -WorkingDirectory $nginxHome `
    -WindowStyle Hidden `
    -PassThru

Start-Sleep -Milliseconds 300
if ($nginxProcess.HasExited) {
    throw "Nginx 启动失败，退出码：$($nginxProcess.ExitCode)"
}

Write-Host "Nginx 已启动： http://localhost:8088/"
Write-Host "配置文件： $configPath"
