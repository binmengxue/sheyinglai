#!/usr/bin/env bash
# 在你自己电脑上执行：本地打包并上传到服务器，然后重启服务
# 用法：bash deploy/deploy.sh   （需要先配置 SSH 免密登录 root@服务器；本机需有 rsync，Windows 用 Git Bash/WSL）
set -euo pipefail
SERVER=${SERVER:-root@106.14.171.234}
ROOT=$(cd "$(dirname "$0")/.." && pwd)

echo ">>> 打包后端（含测试）"
(cd "$ROOT/server" && mvn -q -B package)
echo ">>> 打包前端"
(cd "$ROOT/web" && pnpm install --frozen-lockfile && pnpm build)

echo ">>> 上传后端（先传到临时文件，旧版本保留为 app.jar.bak 以便回滚）"
scp "$ROOT/server/target/app.jar" "$SERVER:/srv/photo/app.jar.new"

echo ">>> 重启后端并检查健康"
ssh "$SERVER" 'bash -s' <<'REMOTE'
set -euo pipefail
cd /srv/photo
[ -f app.jar ] && cp -f app.jar app.jar.bak
mv -f app.jar.new app.jar
chown photo:photo app.jar
systemctl restart photo
for i in $(seq 1 30); do
  if curl -fs -o /dev/null http://127.0.0.1:8080/api/auth/me; then echo "后端已启动"; exit 0; fi
  sleep 2
done
echo "!!! 后端 60 秒内没有启动成功，回滚到上一个版本"
journalctl -u photo -n 30 --no-pager || true
if [ -f app.jar.bak ]; then
  cp -f app.jar.bak app.jar && chown photo:photo app.jar && systemctl restart photo
fi
exit 1
REMOTE

# 后端正常后再更新前端，避免新前端调用旧接口
echo ">>> 上传前端"
rsync -az --delete "$ROOT/web/dist/" "$SERVER:/var/www/photo/"
echo ">>> 完成：https://sheyinglai.top"
