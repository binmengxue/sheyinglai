# 摄影来 · 个人摄影作品集

Vue 3 + Element Plus 前端，Spring Boot 3 后端。功能：管理员登录、相册增删改、批量上传照片（并发队列 + 进度）、瀑布流浏览、大图预览、设封面。访客只能浏览，适合个人主体备案。

```
sheyinglai/
├── web/                 # 前端：Vue 3 + Vite + TS + Element Plus + Pinia
├── server/              # 后端：Spring Boot 3 + MyBatis-Plus + Sa-Token
├── deploy/              # 服务器部署：Nginx、systemd、初始化和发布脚本
└── docker-compose.yml   # 本地开发用 MySQL（可选）
```

## 一、本地运行

**环境**：JDK 17+、Maven 3.8+、Node 18+。IDE 推荐 IntelliJ IDEA（后端）+ VS Code（前端，装 Vue - Official 插件）。

### 1. 启动后端

```bash
cd server
mvn spring-boot:run
```

默认使用 **h2 profile**：不用装数据库，数据存在 `server/data/`，图片存在 `server/uploads/`，适合先跑起来看效果。

想用 MySQL 时：

```bash
docker compose up -d                         # 在项目根目录，启动本地 MySQL（root / 123456）
cd server && mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

启动后访问接口文档：<http://localhost:8080/api/swagger-ui.html>

### 2. 启动前端

```bash
cd web
npm install
npm run dev
```

打开 <http://localhost:5173>，点右上角「登录」，默认账号 `admin` / `admin123`（只在本地有效）。登录后到「相册」新建相册，进入相册上传照片。

> 国内 npm / Maven 下载慢的话，可以把 npm 源设成 `https://registry.npmmirror.com`，Maven 在 `~/.m2/settings.xml` 配置阿里云镜像 `https://maven.aliyun.com/repository/public`。

## 二、接口一览

所有接口前缀 `/api`，返回 `{ code, msg, data }`，`code = 0` 为成功。需要登录的接口带请求头 `Authorization: Bearer <token>`。

| 方法 | 路径 | 说明 | 需登录 |
|---|---|---|---|
| POST | /auth/login | 登录，返回 token | |
| GET | /auth/me | 当前登录状态 | |
| POST | /auth/logout | 退出 | |
| GET | /albums | 相册列表（未登录只返回公开相册） | |
| GET | /albums/{id} | 相册详情 | |
| POST | /albums | 新建相册 | ✅ |
| PUT | /albums/{id} | 修改相册 | ✅ |
| DELETE | /albums/{id} | 删除相册及其照片 | ✅ |
| PUT | /albums/{id}/cover/{photoId} | 设置封面 | ✅ |
| GET | /photos?albumId=&page=&size= | 照片分页；不传 albumId 为首页最新作品 | |
| POST | /photos | 上传一张照片（multipart：albumId、file） | ✅ |
| DELETE | /photos/{id} | 删除照片 | ✅ |
| GET | /files/** | 本地存储模式下的图片文件 | |

以后做小程序时，可以直接复用这套接口。

## 三、部署到服务器（Ubuntu，106.14.171.234）

### 1. 阿里云防火墙

轻量服务器控制台 → 防火墙：放行 **22、80、443**。**8080、3306 不要放行**。

### 2. 初始化服务器（只做一次）

把 `deploy/` 目录传到服务器，然后执行：

```bash
scp -r deploy root@106.14.171.234:/root/
ssh root@106.14.171.234
cd /root/deploy && DB_PASS='你的数据库密码' bash server-init.sh
vim /srv/photo/photo.env      # 填 DB_PASSWORD（和上面一致）、ADMIN_PASSWORD
```

脚本会安装 JDK 17、Nginx、MySQL，创建数据库和 `photo` 用户，配置好 Nginx 和 systemd 服务。

### 3. 发布

在你自己电脑的项目根目录执行（先配置好 SSH 免密登录）：

```bash
bash deploy/deploy.sh
```

脚本会本地打包前后端，上传到服务器并重启服务。完成后访问 <http://106.14.171.234>。

常用命令：

```bash
systemctl status photo          # 后端运行状态
journalctl -u photo -f          # 实时日志
tail -f /srv/photo/logs/app.log # 应用日志
```

### 4. 备案通过后：域名 + HTTPS

1. 轻量服务器控制台 → 域名 → 申请免费证书，下载 **Nginx** 格式，上传到服务器 `/etc/nginx/cert/`。
2. 在 `nginx-photo.conf` 里加一个 443 的 server（复制 80 的内容，加上下面几行），80 端口改成跳转到 https：

```nginx
listen 443 ssl;
ssl_certificate     /etc/nginx/cert/sheyinglai.top.pem;
ssl_certificate_key /etc/nginx/cert/sheyinglai.top.key;
```

3. 修改 `web/src/App.vue` 底部的备案号（备案通过后必须在网站底部展示，并链接到工信部网站），重新发布。

### 5. 切换到阿里云 OSS（图片多了以后）

1. 在 OSS 控制台创建 Bucket（地域选华东 2 上海，和服务器一致），读写权限设为「公共读」，开通图片处理。
2. 在 RAM 控制台创建一个只有该 Bucket 读写权限的 AccessKey。
3. 修改 `/srv/photo/photo.env`：`STORAGE_TYPE=oss`，填写 `OSS_*` 配置，然后执行 `systemctl restart photo`。

切换后新上传的照片存 OSS，旧照片仍在服务器本地且能正常访问；但在后台删除旧照片时只删数据库记录，本地文件需要手动清理。建议图片还少的时候就尽早切换。

### 6. 测试环境（https://sheyinglai.top/api-dev）

本地默认（`pnpm dev` + h2）的数据和图片都存在本机。想让本地操作直接落到测试环境：

**服务器一次性搭建**（和线上同一台机器，独立进程/数据库/图片目录）：

1. `mkdir -p /srv/photo-dev/{uploads,logs} && chown -R photo:photo /srv/photo-dev`，建库 `photo_dev` 并给 `photo` 用户授权。
2. 复制 `deploy/photo-dev.env.example` 为 `/srv/photo-dev/photo.env`（`chmod 600`），`deploy/photo-dev.service` 放到 `/etc/systemd/system/`。
3. 更新 Nginx 配置（已含 `/api-dev/` 转发）：`nginx -t && systemctl reload nginx`。
4. 打包后端 `mvn -q clean package -DskipTests`，上传 `server/target/app.jar` 到 `/srv/photo-dev/app.jar`，`systemctl enable --now photo-dev`。

**本地使用**：

```bash
cd web
pnpm dev:test     # 前端请求走 /api-dev，经 Vite 代理到测试环境；上传的图片存测试环境，不再写本地 server/uploads
```

不需要启动本地后端。登录账号为测试环境 `photo.env` 里配置的管理员。

## 四、说明

- **管理员账号**：个人站点只有一个管理员，账号密码在 `photo.env` 里配置，不存数据库。
- **个人备案**：网站内容要和备案时填的「个人摄影作品展示」一致，不要加对外招募、收费等功能。
- **安全**：上线前一定修改 `ADMIN_PASSWORD`；`photo.env` 不要提交到 Git（`.gitignore` 已排除）。
