# 摄影来 · 约拍摄影师平台

Vue 3 + Element Plus 前端（手机版式 H5），Spring Boot 3 后端。多摄影师入驻的约拍平台，只做拍照：

- **用户**：手机号 + 密码注册，注册即可接单（不审核，管理员事后处置）
- **首页**：摄影师列表，按类型筛选，按综合 / 距离 / 评分 / 价格排序
- **摄影师主页**：资料、排名、收费详情、客片（按类型分组）、评价、关注
- **档期与预约**：月历显示可约时段，客户提交预约，摄影师确认 / 拒绝 / 完成；**不在线付款**，确认后客户可以看到摄影师联系方式，线下沟通付款
- **评价**：完成后客户打分评价，自动更新评分
- **经营数据**：曝光、主页浏览、咨询量（昨日环比 + 30 天），订单均价、成交率、接单率、回复时长
- **喜欢客片、我的点评、管理后台**（封禁账号、隐藏摄影师、删除违规客片和评价）

> ⚠️ **备案提示**：多用户入驻、发布报价、撮合预约已超出「个人摄影作品展示」的个人备案范围，上线前请确认需要换成企业主体备案（可能还涉及 ICP 许可证）。`/agreement`、`/privacy` 里的《用户协议》《隐私政策》只是草稿，上线前要换成正式文本。

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

表结构由 **Flyway** 管理（`server/src/main/resources/db/migration/V*.sql`），启动时自动执行新版本；以后改表请新增 `V3__xxx.sql`，不要改已执行过的文件。

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

打开 <http://localhost:5173>（手机版式，PC 上用浏览器 DevTools 切到手机尺寸看效果更准）。

- 管理员：手机号 `13800000000` / 密码 `admin123`（只在本地有效，启动时自动创建）。升级前的旧相册和照片会自动归到管理员名下。
- 普通用户：在「我」→ 注册。注册后到「我要接单」填价格，就会出现在首页；到「我的客片」上传作品。
- 本地后端不在 8080 时：`API_TARGET=http://localhost:18080 pnpm dev`。

> 国内 npm / Maven 下载慢的话，可以把 npm 源设成 `https://registry.npmmirror.com`，Maven 在 `~/.m2/settings.xml` 配置阿里云镜像 `https://maven.aliyun.com/repository/public`。

## 二、接口一览

所有接口前缀 `/api`，返回 `{ code, msg, data }`，`code = 0` 为成功。需要登录的接口带请求头 `Authorization: Bearer <token>`。

完整参数见 Swagger 文档。「本人」指只能操作自己的数据（管理员不受限）。

| 方法 | 路径 | 说明 | 需登录 |
|---|---|---|---|
| POST | /auth/register | 手机号 + 密码注册（需 agree=true），返回 token | |
| POST | /auth/login | 手机号 + 密码登录，返回 token | |
| GET | /auth/me | 当前用户、摄影师资料、是否管理员 | |
| POST | /auth/logout | 退出 | |
| PUT | /users/me | 修改昵称、性别、城市、简介、微信号 | ✅ |
| PUT | /users/me/password | 修改密码 | ✅ |
| POST | /users/me/avatar | 上传头像 | ✅ |
| GET | /photographers?style=&sort=&lat=&lng=&page= | 首页摄影师列表，sort：default / distance / rating / price | |
| GET | /photographers/{id} | 摄影师主页（track=false 不计浏览量） | |
| PUT | /photographers/me | 修改接单设置（价格、精修、设备、爽约规则、档期规则、位置） | ✅ |
| GET | /photographers/{id}/schedule?month=2026-10 | 档期：每天状态和可约时段 | |
| PUT | /photographers/me/off | 设置 / 取消某天休息 | ✅ |
| GET | /photographers/{id}/reviews | 评价列表 | |
| POST / DELETE | /photographers/{id}/follow | 关注 / 取消关注 | ✅ |
| GET | /albums?userId=&style= | 某摄影师的客片 | |
| GET | /albums/mine | 我的客片（含不公开） | ✅ |
| GET | /albums/{id} | 客片详情 | |
| POST / PUT / DELETE | /albums、/albums/{id} | 新建 / 修改 / 删除客片（本人） | ✅ |
| PUT | /albums/{id}/cover/{photoId} | 设置封面（本人） | ✅ |
| GET | /photos?albumId=&page=&size= | 某组客片的照片 | |
| POST | /photos | 上传一张照片（multipart：albumId、file，本人） | ✅ |
| DELETE | /photos/{id} | 删除照片（本人） | ✅ |
| POST | /bookings | 发起预约 | ✅ |
| GET | /bookings?role=customer\|photographer&status= | 我约的 / 约我的 | ✅ |
| POST | /bookings/{id}/confirm、reject、complete | 摄影师确认 / 拒绝 / 标记完成 | ✅ |
| POST | /bookings/{id}/cancel | 任一方取消 | ✅ |
| POST | /bookings/{id}/review | 客户评价已完成的预约 | ✅ |
| GET | /me/likes、POST / DELETE /me/likes/{albumId} | 喜欢的客片 | ✅ |
| GET | /me/reviews | 我写的点评 | ✅ |
| GET | /me/stats | 经营数据 | ✅ |
| GET | /admin/users?keyword= | 用户列表（管理员） | ✅ |
| PUT | /admin/users/{id}/banned?value= | 封禁 / 解封（管理员） | ✅ |
| PUT | /admin/photographers/{id}/visible?value= | 从首页隐藏 / 恢复（管理员） | ✅ |
| DELETE | /admin/reviews/{id} | 删除评价（管理员） | ✅ |
| GET | /files/** | 本地存储模式下的图片文件 | |

预约状态：`PENDING 待确认 → CONFIRMED 已确认 → COMPLETED 已完成`，另有 `REJECTED 已拒绝`、`CANCELLED 已取消`。只有已确认的预约会占用档期。

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
vim /srv/photo/photo.env      # 填 DB_PASSWORD（和上面一致）、ADMIN_PHONE、ADMIN_PASSWORD
```

脚本会安装 JDK 17、Nginx、MySQL，创建数据库和 `photo` 用户，配置好 Nginx 和 systemd 服务。

### 3. 发布

在你自己电脑的项目根目录执行（先配置好 SSH 免密登录）：

```bash
bash deploy/deploy.sh
```

脚本会本地打包前后端，上传到服务器并重启服务。完成后访问 <http://106.14.171.234>。

**从作品集版本升级到约拍平台版本（只需做一次）**：

1. 先备份数据库：`mysqldump -u root photo > /root/photo-backup-$(date +%F).sql`（测试环境同理备份 `photo_dev`）。
2. 修改 `/srv/photo/photo.env`：把 `ADMIN_USERNAME=admin` 换成 `ADMIN_PHONE=你的手机号`（管理员改为手机号登录）。**不加这一项后端会启动失败。**
3. 正常发布。启动时 Flyway 会把现有表记为基线 V1，只执行 V2（新建用户、预约等表，给相册和照片加列）；`AdminInitializer` 会创建管理员账号，并把旧相册和照片归到管理员名下。
4. 建议先在测试环境走一遍，确认旧数据正常再发线上。

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

3. 修改 `web/src/views/HomeView.vue` 底部的备案号（备案通过后必须在网站底部展示，并链接到工信部网站），重新发布。

> 首页「距离最近」依赖浏览器定位，浏览器只在 HTTPS 下允许定位；HTTP 访问时列表照常显示，只是没有距离。

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

不需要启动本地后端。管理员账号为测试环境 `photo.env` 里配置的 `ADMIN_PHONE` / `ADMIN_PASSWORD`，也可以直接注册新账号。

## 四、说明

- **管理员账号**：由 `photo.env` 的 `ADMIN_PHONE` / `ADMIN_PASSWORD` 决定，每次启动自动创建或同步（改密码只需改配置并重启）。普通用户存在数据库里，密码 BCrypt 加密。
- **隐私**：手机号不公开；摄影师确认预约后，客户才能看到摄影师的微信号（没填则显示手机号）；客户填的联系方式只有该摄影师能看到；摄影师的定位坐标不返回给前端，只用来算距离。
- **备案**：见文首提示，多用户约拍平台需要按实际运营主体备案。
- **安全**：上线前一定修改 `ADMIN_PASSWORD`；`photo.env` 不要提交到 Git（`.gitignore` 已排除）。
