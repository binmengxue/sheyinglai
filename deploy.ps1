# 一键发布到 https://sheyinglai.top
# 放在项目根目录 E:\liuxueqin\sheyinglai\ 下，用法：
#   .\deploy.ps1            前后端都发布
#   .\deploy.ps1 -Web       只发布前端（只改了 web 目录时用，最快）
#   .\deploy.ps1 -Server    只发布后端（只改了 server 目录时用）
param([switch]$Web, [switch]$Server)

$Remote = "root@106.14.171.234"
$Root = $PSScriptRoot
if (-not $Web -and -not $Server) { $Web = $true; $Server = $true }

function Check($step) {
    if ($LASTEXITCODE -ne 0) { Write-Host "[失败] $step，已停止发布" -ForegroundColor Red; exit 1 }
}

if ($Server) {
    Write-Host ">>> 打包后端..." -ForegroundColor Cyan
    Push-Location "$Root\server"; mvn -q clean package -DskipTests; $code = $LASTEXITCODE; Pop-Location
    $LASTEXITCODE = $code; Check "后端打包"
}
if ($Web) {
    Write-Host ">>> 打包前端..." -ForegroundColor Cyan
    Push-Location "$Root\web"; pnpm build; $code = $LASTEXITCODE; Pop-Location
    $LASTEXITCODE = $code; Check "前端打包"
}

if ($Server) {
    Write-Host ">>> 上传后端..." -ForegroundColor Cyan
    scp -q "$Root\server\target\app.jar" "${Remote}:/srv/photo/app.jar"; Check "上传 app.jar"
    ssh $Remote "chown photo:photo /srv/photo/app.jar && systemctl restart photo"; Check "重启后端"
}
if ($Web) {
    Write-Host ">>> 上传前端..." -ForegroundColor Cyan
    ssh $Remote "rm -rf /tmp/dist"; Check "清理临时目录"
    scp -q -r "$Root\web\dist" "${Remote}:/tmp/"; Check "上传 dist"
    ssh $Remote "rm -rf /var/www/photo/* && cp -r /tmp/dist/. /var/www/photo/ && rm -rf /tmp/dist"; Check "替换前端文件"
}

Write-Host ""
Write-Host "发布完成：https://sheyinglai.top" -ForegroundColor Green
if ($Server) { Write-Host "后端需要约 30 秒启动，稍等再刷新页面" -ForegroundColor Yellow }
