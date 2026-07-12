# 运维手册

本文档只记录当前代码实际可执行的本地运行、打包与验证流程。若目标是正式部署上线，请先阅读 `changelogs/SUMMARY.md`，再进入相关部署卡片查看方案、检查单与回滚预案。

## 1. 环境要求

- Java 17
- Maven 3.9+
- Node.js 与 npm
- MySQL 8.x
- Redis

## 2. 当前配置基线

后端核心配置来自 [application.yml]：

- 端口：`8080`
- 字符集：强制 `UTF-8`
- JPA：默认 `ddl-auto: validate`
- Flyway：启动时自动执行 `db/migration` 初始化脚本
- `open-in-view: false`
- JWT 通过环境变量 `JWT_SECRET` 注入
- Redis 通过 `REDIS_*` 环境变量配置
- `application.yml` 的基础默认 CORS 仅允许：
  - `http://localhost:5173`
  - `http://127.0.0.1:5173`
- 按仓库推荐方式复制 `backend/.env.example` 后，本地常见配置还会额外放开：
  - `http://localhost:*`
  - `http://127.0.0.1:*`

开发示例配置文件：

- `backend/src/main/resources/application-local.example.yml`


推荐的本地准备：

- 将 `backend/.env.example` 复制为 `backend/.env`
- 默认保持 `JPA_DDL_AUTO=validate`，首次启动或空库环境由 Flyway 初始化库表
- 若你确实需要临时覆写 JPA 建表策略，再将 `backend/src/main/resources/application-local.example.yml` 复制为本地 `application-local.yml`

## 3. 启动方式

### 3.1 后端直接运行

前置条件：

- MySQL 已可连通
- `backend/.env` 已准备完成
- 数据库账号具备执行 Flyway 初始化迁移所需权限

注意：

- 应用启动时会先执行 Flyway 迁移，再由 Hibernate 以 `validate` 校验表结构
- 仅设置 `SPRING_PROFILES_ACTIVE=local` 并不会自动修改建表策略
- 只有当本地实际存在 `application-local.yml`，且其中覆盖了 `spring.jpa.hibernate.ddl-auto`，例如 `update`，才会改变默认的校验策略

```powershell
cd backend
$env:SPRING_PROFILES_ACTIVE="local"
$env:DB_PASSWORD="你的数据库密码"
$env:JWT_SECRET="长度足够的本地密钥"
mvn spring-boot:run
```

### 3.2 后端打包运行

```powershell
cd backend
mvn -DskipTests package
java -jar .\target\backend-0.0.1-SNAPSHOT.jar --spring.datasource.password=你的数据库密码 --jwt.secret=长度足够的本地密钥
```

### 3.3 前端开发启动

```powershell
cd frontend
npm install
npm run dev
```

### 3.4 Redis 启动

当前仓库不提供固定的本地 `redis/` 运行目录。

请自行准备 Redis 服务，并确保：

- `REDIS_HOST`
- `REDIS_PORT`
- `REDIS_PASSWORD`

与后端配置保持一致。

## 4. 访问地址

- 前端：`http://localhost:5173/`
- 后端：`http://localhost:8080/`

## 5. 验证命令

### 5.1 后端

```powershell
cd backend
mvn test
mvn -DskipTests package
```

### 5.2 前端

```powershell
cd frontend
npm run check
npm run lint
npm run build
```

说明：

- 当前前端没有 `npm test`
- 当前仓库没有 CI 配置，这些命令需要本地手工执行

## 6. 接口冒烟建议

建议至少验证以下场景：

- `POST /api/auth/register`：正常注册
- `POST /api/auth/login`：正常登录
- `POST /api/auth/login` 非法 JSON：返回 `400`
- `POST /api/votes` 未登录：返回 `401`
- `POST /api/votes` 已登录：创建成功
- `POST /api/votes/{voteId}/vote`：
  - 正常投票成功
  - 重复投票返回 `409`

## 7. 常见问题

### 7.1 前端请求失败

检查项：

- 前端是否运行在 `5173`
- 后端是否运行在 `8080`
- `CORS_ALLOWED_ORIGINS` 是否与前端地址一致

### 7.2 后端启动失败

检查项：

- `DB_PASSWORD` 是否设置
- `JWT_SECRET` 是否设置
- MySQL 是否可连通
- 若出现表不存在或 Hibernate `validate` 失败，检查 Flyway 迁移是否执行成功，以及数据库账号是否具备建表权限

### 7.3 Redis 不可用

当前行为：

- Redis 不可用时，投票幂等相关逻辑会降级
- 主业务流程尽量不中断，但仍建议在开发和生产环境中保持 Redis 可用

## 8. 当前未纳入本文档的内容

以下内容不在本文档展开，或仍未在仓库中形成现成流程：

- 正式部署上线步骤、上线检查单与回滚预案（统一维护在 `changelogs/`）
- Docker 部署
- CI 流水线
- Kubernetes 正式部署方案
- 管理后台运维流程
