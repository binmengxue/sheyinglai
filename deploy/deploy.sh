#!/usr/bin/env bash
# 在你自己电脑上执行：本地打包并上传到服务器，然后重启服务
# 用法：bash deploy/deploy.sh   （需要先配置 SSH 免密登录 root@服务器；本机需有 rsync，Windows 用 Git Bash/WSL）
set -euo pipefail
SERVER=${SERVER:-root@106.14.171.234}
ROOT=$(cd "$(dirname "$0")/.." && pwd)

echo ">>> 打包后端"
(cd "$ROOT/server" && mvn -q -B package -DskipTests)
echo ">>> 打包前端"
(cd "$ROOT/web" && npm install && npm run build)

echo ">>> 上传"
scp "$ROOT/server/target/app.jar" "$SERVER:/srv/photo/app.jar"
rsync -az --delete "$ROOT/web/dist/" "$SERVER:/var/www/photo/"

echo ">>> 重启后端"
ssh "$SERVER" "chown photo:photo /srv/photo/app.jar && systemctl restart photo && sleep 8 && systemctl --no-pager status photo | head -5"
echo ">>> 完成：http://106.14.171.234"
