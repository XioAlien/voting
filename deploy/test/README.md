# 测试环境部署模板

本目录用于承载“先测试后生产”阶段的部署模板，不保存任何真实生产值。

当前同时提供两条测试环境路径：

- Docker 一键启动
  - 适合测试机 `clone` 后快速拉起完整链路
- 主机手工部署模板
  - 适合后续演练 `systemd + Nginx + Jar` 的真实主机部署

当前文件：

- `docker-compose.yml`
  - Docker 一键启动编排，包含 frontend、backend、mysql、redis 四个服务
- `backend.env.example`
  - 后端测试环境 `.env` 模板
- `vote-backend-test.service`
  - `systemd` 服务模板
- `nginx.vote-test.conf`
  - Nginx 站点模板

## Docker 一键启动

这套编排现在不再内置任何测试默认口令。启动前需要先显式传入关键变量，再在仓库根目录执行：

```bash
export MYSQL_PASSWORD='your-test-db-password'
export MYSQL_ROOT_PASSWORD='your-test-root-password'
export JWT_SECRET='your-test-jwt-secret'
export BOOTSTRAP_ADMIN_PASSWORD='your-test-admin-password'
docker compose -f deploy/test/docker-compose.yml up -d --build
```

当前约定：

- 前端访问地址：`http://localhost:8088`
- 后端通过前端同源 `/api` 转发访问
- MySQL、Redis 使用容器内部网络互联
- Flyway 会在后端启动时自动初始化测试数据库
- 测试管理员账号信息默认仍为：
  - 用户名：`test-admin`
  - 邮箱：`test-admin@example.com`
- 管理员密码必须由 `BOOTSTRAP_ADMIN_PASSWORD` 显式传入

必填环境变量：

- `MYSQL_PASSWORD`
- `MYSQL_ROOT_PASSWORD`
- `JWT_SECRET`
- `BOOTSTRAP_ADMIN_PASSWORD`

停止命令：

```bash
docker compose -f deploy/test/docker-compose.yml down
```

连同数据卷一起清理：

```bash
docker compose -f deploy/test/docker-compose.yml down -v
```

如需覆盖端口或管理员信息，可在执行前通过 shell 环境变量注入，例如：

```bash
APP_PORT=8090 BOOTSTRAP_ADMIN_USERNAME='staging-admin' BOOTSTRAP_ADMIN_EMAIL='staging-admin@example.com' docker compose -f deploy/test/docker-compose.yml up -d --build
```

## 主机手工部署模板

以下内容用于后续主机部署预演：

- `backend.env.example`
  - 后端测试环境 `.env` 模板
- `vote-backend-test.service`
  - `systemd` 服务模板
- `nginx.vote-test.conf`
  - Nginx 站点模板

## 建议目录

```text
/srv/vote-test/
├── backend/
│   ├── app.jar
│   ├── .env
│   └── logs/
└── frontend/
    └── dist/
```

## 使用方式

1. 复制 `backend.env.example` 为测试环境实际 `.env`
2. 按测试机实际数据库、Redis、域名替换占位值
3. 将 `vote-backend-test.service` 放到 `/etc/systemd/system/`
4. 将 `nginx.vote-test.conf` 放到 Nginx 站点目录并启用
5. 首次验证通过后，立即将 `BOOTSTRAP_ADMIN_ENABLED` 改回 `false`

## 注意

- 本目录不用于存放真实生产口令
- `deploy/test/docker-compose.yml` 中的关键口令现已改为必填变量，未传入时 compose 会直接报错退出
- 生产环境应在测试环境预演完成后另行定稿
- 测试环境建议仍使用 `SPRING_PROFILES_ACTIVE=prod`，这样启动保护和生产路径更接近
- 当前后端仍复用 `8080`，若测试机已有其他服务占用该端口，需要同步调整 Nginx 反代和后端启动参数
- Docker 一键启动默认暴露 `8088`，是为了避免与宿主机现有 `80/8080` 冲突
- 当前仓库只完成了 compose 配置校验；镜像构建与整链路启动仍需在 Docker daemon 可用的机器上补最后一轮实机验证
