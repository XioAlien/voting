# 安全基线

本文档仅记录当前代码中已经落地的安全能力，以及仍未完成的安全项。

## 1. 已落地的安全基线

### 1.1 敏感配置外置

当前敏感信息已从主配置文件中外置为环境变量：

- `DB_PASSWORD`
- `JWT_SECRET`
- `REDIS_PASSWORD`

见 [application.yml](file:///d:/code/java/voting/backend/src/main/resources/application.yml) 与 [backend/.env.example](file:///d:/code/java/voting/backend/.env.example)。

### 1.2 鉴权规则

当前后端安全规则：

- 放行 `POST /api/auth/login`
- 放行 `POST /api/auth/register`
- 放行 `GET|POST /api/auth/dev-token`（仅 `dev/local` 且显式开启时可用）
- 放行 `GET /api/votes/**`
- 放行 `OPTIONS /**`
- `/api/admin/**` 要求管理员权限
- 其余接口要求认证

实现位置：

- [SecurityConfig](file:///d:/code/java/voting/backend/src/main/java/com/vote/backend/security/SecurityConfig.java)

### 1.3 JWT 认证

当前使用 Bearer Token 认证：

- 认证成功后将用户信息写入 `SecurityContext`
- token 非法与过期会区分文案

当前 `401` 语义：

- 未登录：`未登录`
- token 非法：`登录状态无效，请重新登录`
- token 过期：`登录状态已过期，请重新登录`

实现位置：

- [JwtAuthenticationFilter](file:///d:/code/java/voting/backend/src/main/java/com/vote/backend/security/JwtAuthenticationFilter.java)
- [RestAuthenticationEntryPoint](file:///d:/code/java/voting/backend/src/main/java/com/vote/backend/security/RestAuthenticationEntryPoint.java)

### 1.4 统一异常处理

当前后端已统一异常出口：

- 参数校验失败：`400`
- 非法 JSON：`400`
- 未认证：`401`
- 无权限：`403`
- 资源不存在：`404`
- 业务冲突：`409`
- 未处理异常：`500`

实现位置：

- [ApiException](file:///d:/code/java/voting/backend/src/main/java/com/vote/backend/controller/ApiException.java)
- [GlobalExceptionHandler](file:///d:/code/java/voting/backend/src/main/java/com/vote/backend/controller/GlobalExceptionHandler.java)

### 1.5 生产配置基线

当前默认生产导向配置包括：

- `ddl-auto: validate`
- `open-in-view: false`
- 默认关闭 SQL 输出
- 强制 UTF-8 编码
- CORS 白名单配置

### 1.6 幂等与防重

当前已完成：

- Redis 幂等键失败时降级
- 投票失败后清理幂等键，避免误封
- 数据库级重复投票记录约束兜底

### 1.7 管理员与二次确认

当前已完成：

- `/api/admin/**` 管理员接口权限控制
- 管理员确认令牌签发与校验
- `dev-token` 的环境和开关双重限制

### 1.8 邀请码访问控制

当前已完成：

- 投票邀请码加入与退出链路
- 邀请码设置、重置与成员限制能力
- 管理相关接口依赖登录身份和业务校验

## 2. 当前未落地的安全能力

以下能力当前代码中没有实现，不应在其他文档中描述为已完成：

- refresh token 轮换与撤销
- 默认管理员首次登录改密
- CI 安全门禁与敏感信息扫描
- 更完整的部署基线与密钥托管方案

## 3. 当前风险与建议

### P1

- 前端 token 当前只写入 `localStorage`
  - 风险：浏览器本地脚本环境被污染时，token 暴露面更大
  - 当前状态：已实现，但尚未收敛为更严格策略

- 当前没有统一业务错误码字段
  - 风险：前后端在部分场景仍需要依赖消息文案判断
  - 当前状态：已统一状态码，但未统一 `code`

### P2

- 没有 CI 安全门禁
  - 风险：后续改动可能重新引入明文配置或安全回退

- 没有 Docker / 部署基线
  - 风险：部署环境差异可能导致安全配置不一致

## 4. 验收基线

当前建议至少验证以下安全场景：

- 非法 JSON 登录：`400`
- 空字段登录：`400`
- 错误密码登录：`400`
- 未登录访问写接口：`401`
- 伪造 token 访问写接口：`401`
- 过期 token 访问写接口：`401`
- 无权限场景：`403`

## 5. 维护原则

- 任何未在代码中实现的能力，不写入“已落地”章节
- 新增安全特性时，应同步更新：
  - `README.md`
  - `api_contract.md`
  - 本文档
