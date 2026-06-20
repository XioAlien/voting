# Vote 投票系统

一个基于 Spring Boot 与 React 的投票系统。当前已包含注册登录、投票创建、邀请码加入、管理员基础能力、dev-token 调试入口，以及统一异常处理与基础安全基线。

## 当前实现范围

- 用户认证：支持用户名或邮箱登录、用户注册、JWT 鉴权
- 投票能力：
  - 查看投票列表
  - 查看投票详情与结果
  - 登录后创建投票
  - 登录后为允许自定义选项的投票添加选项
  - 登录后提交投票
  - 邀请码加入/退出投票
  - 创建者或管理员管理邀请码、重置邀请码、配置成员上限与过期时间
- 管理员能力：
  - 管理员概览看板
  - 用户列表、用户基础信息更新、角色调整
  - 投票规则调整、终止投票
  - 二次确认令牌与审计日志回填
- 开发辅助：
  - `dev/local` 环境可按开关启用 `dev-token`
- 投票模式：
  - `CHOICE`：单选或多选
  - `SLIDER`：按选项评分
- 安全与稳定性：
  - 写接口要求登录
  - `401/403` 返回统一 JSON
  - 业务异常统一走 `ApiException + GlobalExceptionHandler`
  - Redis 不可用时对幂等与部分缓存链路做降级
  - 投票记录具备数据库级防重约束

## 当前未实现

以下能力当前仍未完整落地，不应作为使用预期：

- 更细粒度的独立 RBAC 权限体系
- 应用 Docker 交付与 CI 自动化流水线
- 前端自动化测试

## 技术栈

- 后端：Java 17、Spring Boot 3.2.x、Spring Security、Spring Data JPA、JJWT、MySQL、Redis
- 前端：React 18、TypeScript、Vite、React Router、Ant Design

## 目录结构

```text
.
├─ backend/                 # Spring Boot 后端
├─ frontend/                # React + Vite 前端
├─ .trae/documents/         # 当前维护中的详细文档
```

## 环境要求

- Java 17
- Maven 3.9+
- Node.js 18+ 与 npm
- MySQL 8.x
- Redis
- 可选：Docker Desktop / Docker Engine（如你希望用容器启动依赖）

## 配置说明

- 将 `backend/.env.example` 复制为 `backend/.env`，填写本地占位值后再启动依赖和后端
- `backend/src/main/resources/application.yml` 会自动导入 `backend/.env`，仓库只保留模板与非敏感默认值
- `backend/src/main/resources/application-dev.example.yml`、`application-local.example.yml` 仅作为个人覆写模板；如需额外 profile 覆写，可参考 example 文件自行生成本地私有配置文件，但不要提交
- 前端 `.env` 不是必需项；如需覆盖默认值，可自行创建 `frontend/.env` 配置 `VITE_API_BASE_URL`、`VITE_PROXY_TARGET`、`VITE_PORT`
- 当前依赖服务半容器化约定：
  - 本地后端连接 `localhost`
  - 未来全容器化时只需把 `DB_HOST` 改为 `mysql`、把 `REDIS_HOST` 改为 `redis`

## 启动方式

### 推荐：手动启动

当前已跟踪仓库内容下，建议按以下顺序手动启动：

1. 准备 MySQL 与 Redis，并确保和 `backend/.env` 中的配置一致
2. 将 `backend/.env.example` 复制为 `backend/.env`
3. 如是首次启动或空库环境，可参考 `application-local.example.yml` 自行准备本地 profile 覆写文件
4. 使用 `local` profile 启动后端
5. 安装前端依赖并启动前端开发服务

说明：

- 默认配置中的 `spring.jpa.hibernate.ddl-auto` 为 `validate`，首次启动若没有现成表结构，建议走 `local` profile 下的本地覆盖配置
- 前端默认会通过 Vite 代理把 `/api` 转发到 `http://localhost:8080`

### 后端

```powershell
cd backend
$env:SPRING_PROFILES_ACTIVE="local"
mvn spring-boot:run
```

或先打包再运行：

```powershell
cd backend
mvn -DskipTests package
java -jar .\target\backend-0.0.1-SNAPSHOT.jar --spring.datasource.password=你的数据库密码 --jwt.secret=长度足够的本地密钥
```

### 前端

```powershell
cd frontend
npm install
npm run dev
```

默认访问地址：

- 前端：`http://localhost:5173/`
- 后端：`http://localhost:8080/`

## 前端路由

- `/`：投票列表首页
- `/vote/:id`：投票详情与结果页
- `/login`：登录页
- `/register`：注册页
- `/admin`：管理与创建中心，已集成管理员概览、用户管理和创建投票入口

## 主要接口

### 认证

- `POST /api/auth/register`
- `POST /api/auth/login`
- `GET|POST /api/auth/dev-token`（仅 `dev/local` 且显式启用时可用）

### 投票

- `GET /api/votes`
- `GET /api/votes/{id}/results`
- `POST /api/votes`
- `POST /api/votes/{voteId}/join`
- `POST /api/votes/join`
- `POST /api/votes/{voteId}/leave`
- `DELETE /api/votes/{voteId}/leave`
- `GET /api/votes/{voteId}/invite`
- `PUT /api/votes/{voteId}/invite/settings`
- `PATCH /api/votes/{voteId}/invite/settings`
- `POST /api/votes/{voteId}/invite/reset`
- `POST /api/votes/{voteId}/options`
- `POST /api/votes/{voteId}/vote`

### 管理员

- `GET /api/admin/dashboard`
- `POST /api/admin/confirm`
- `POST /api/admin/confirmations`
- `GET /api/admin/users`
- `PUT /api/admin/users/{userId}`
- `PATCH /api/admin/users/{userId}`
- `PUT /api/admin/users/{userId}/role`
- `PATCH /api/admin/users/{userId}/role`
- `PUT /api/admin/users/{userId}/status`
- `PATCH /api/admin/users/{userId}/status`
- `DELETE /api/admin/users`
- `PUT /api/admin/votes/{voteId}/rule`
- `PATCH /api/admin/votes/{voteId}/rule`
- `POST /api/admin/votes/{voteId}/terminate`

更完整的请求与响应示例见 [.trae/documents/api_contract.md](.trae/documents/api_contract.md)。

## 当前鉴权规则

- 放行：
  - `POST /api/auth/login`
  - `POST /api/auth/register`
  - `GET|POST /api/auth/dev-token`
  - `OPTIONS /**`
  - `GET /api/votes/**`（邀请码投票会按成员关系返回 `403`）
- 需要登录：
  - 其他所有写接口
- 需要管理员权限：
  - `/api/admin/**`
- 受环境与开关双重限制：
  - `/api/auth/dev-token`

安全语义：

- `400`：登录凭证错误、参数错误、校验失败
- `401`：未登录、token 无效、token 过期
- `403`：已登录但无权限
- `404`：资源不存在
- `409`：业务状态冲突

## 质量检查

### 后端

```powershell
cd backend
mvn test
mvn -DskipTests package
```

### 前端

```powershell
cd frontend
npm run check
npm run lint
npm run build
```

说明：

- 当前前端没有 `npm test` 脚本，也没有落地自动化测试文件

## 文档索引

- 文档总览：[.trae/documents/README.md](.trae/documents/README.md)
- API 契约：[.trae/documents/api_contract.md](.trae/documents/api_contract.md)
- 运维手册：[.trae/documents/ops_runbook.md](.trae/documents/ops_runbook.md)
- 安全基线：[.trae/documents/security_baseline.md](.trae/documents/security_baseline.md)
- 测试策略：[.trae/documents/testing_strategy.md](.trae/documents/testing_strategy.md)
- 架构说明：[.trae/documents/vote_tech_arch.md](.trae/documents/vote_tech_arch.md)


