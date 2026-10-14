#!/usr/bin/env bash
# 服务器首次初始化（Ubuntu 22.04/24.04），用 root 执行一次：bash server-init.sh
set -euo pipefail

apt update
apt install -y openjdk-17-jre-headless nginx mysql-server rsync

# 运行用户和目录
id photo &>/dev/null || useradd -r -s /usr/sbin/nologin photo
mkdir -p /srv/photo/uploads /srv/photo/logs /var/www/photo
chown -R photo:photo /srv/photo
chmod 755 /srv/photo /srv/photo/uploads   # 让 Nginx 能读取图片

# 数据库和账号：密码必须通过环境变量传入，并和 photo.env 里一致
#   DB_PASS='一个强密码' bash server-init.sh
DB_PASS=${DB_PASS:?请先设置 DB_PASS，例如 DB_PASS='一个强密码' bash server-init.sh}
mysql <<SQL
CREATE DATABASE IF NOT EXISTS photo DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS 'photo'@'localhost' IDENTIFIED BY '${DB_PASS}';
GRANT ALL PRIVILEGES ON photo.* TO 'photo'@'localhost';
FLUSH PRIVILEGES;
SQL

# Nginx 和后端服务
cp nginx-photo.conf /etc/nginx/sites-available/photo
ln -sf /etc/nginx/sites-available/photo /etc/nginx/sites-enabled/photo
rm -f /etc/nginx/sites-enabled/default
nginx -t
systemctl reload nginx

cp photo.service /etc/systemd/system/photo.service
systemctl daemon-reload
systemctl enable photo

[ -f /srv/photo/photo.env ] || { cp photo.env.example /srv/photo/photo.env; chmod 600 /srv/photo/photo.env; chown photo:photo /srv/photo/photo.env; }
echo "初始化完成。请编辑 /srv/photo/photo.env，然后上传 app.jar 和前端 dist。"
