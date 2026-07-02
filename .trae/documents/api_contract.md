# API 契约文档

本文档描述当前已跟踪后端源码与配置中可确认、可供联调的接口；如与历史规划冲突，以源码为准。

## 1. 统一响应结构

所有控制器接口统一返回：

```json
{
  "success": true,
  "message": "操作成功",
  "data": {}
}
```

字段说明：

- `success`：是否成功
- `message`：提示信息
- `data`：业务数据，可能为对象、数组或 `null`

当前实现中没有统一 `code` 和 `requestId` 字段。

## 2. 鉴权规则

当前安全配置以显式放行为准：

- `POST /api/auth/login`：允许匿名访问
- `POST /api/auth/register`：允许匿名访问
- `GET|POST /api/auth/dev-token`：允许匿名访问，但仅 `dev/local` 且显式开启时可用
- `GET /api/votes/**`：允许匿名访问
- `OPTIONS /**`：允许匿名访问
- `/api/admin/**`：要求管理员权限
- 其他写接口：要求携带 `Authorization: Bearer <token>`

当访问受保护接口时：

- 未携带 token：返回 `401`，消息 `未登录`
- token 非法：返回 `401`，消息 `登录状态无效，请重新登录`
- token 过期：返回 `401`，消息 `登录状态已过期，请重新登录`

## 3. 认证接口

### 3.1 注册

- `POST /api/auth/register`

请求体：

```json
{
  "username": "user1",
  "email": "user1@example.com",
  "password": "Password123"
}
```

成功响应示例：

```json
{
  "success": true,
  "message": "注册成功",
  "data": {
    "token": "jwt-token",
    "role": "USER",
    "requirePasswordChange": false
  }
}
```

常见失败：

- 用户名已存在：`409`
- 邮箱已存在：`409`
- 参数校验失败：`400`

### 3.2 登录

- `POST /api/auth/login`

请求体：

```json
{
  "usernameOrEmail": "user1",
  "password": "Password123"
}
```

成功响应示例：

```json
{
  "success": true,
  "message": "登录成功",
  "data": {
    "token": "jwt-token",
    "role": "USER",
    "requirePasswordChange": false
  }
}
```

常见失败：

- 用户名或密码错误：`400`
- 参数校验失败：`400`
- 非法 JSON：`400`

### 3.3 Dev Token

- `GET /api/auth/dev-token`
- `POST /api/auth/dev-token`

说明：

- 仅用于 `dev/local` 调试
- 还需要显式开启 `DEV_TOKEN_ENABLED=true`
- 该接口已存在于已跟踪代码中，实际可用性仍取决于本地 profile 与环境变量配置
- 不应作为生产或联调环境的常规登录方式

## 4. 投票接口

### 4.1 获取投票列表

- `GET /api/votes`

说明：

- 可匿名访问
- 返回投票摘要列表

### 4.2 获取投票详情与结果

- `GET /api/votes/{id}/results`

说明：

- 可匿名访问
- 投票不存在时返回 `404`

### 4.3 创建投票

- `POST /api/votes`
- 需要登录

请求体示例：

```json
{
  "title": "晚餐选择",
  "description": "今晚吃什么",
  "type": "CHOICE",
  "minChoices": 1,
  "maxChoices": 2,
  "forceAllOptions": false,
  "allowCustomOptions": true,
  "options": [
    { "text": "火锅", "maxScore": 100 },
    { "text": "烧烤", "maxScore": 100 }
  ]
}
```

### 4.4 添加自定义选项

- `POST /api/votes/{voteId}/options`
- 需要登录

请求体示例：

```json
{
  "text": "自定义选项",
  "maxScore": 100
}
```

### 4.5 邀请码加入与退出

- `POST /api/votes/{voteId}/join`
- `POST /api/votes/join-by-invite`：仅凭邀请码加入投票
- `POST /api/votes/{voteId}/leave`
- `DELETE /api/votes/{voteId}/leave`

说明：

- `POST /api/votes/{voteId}/join` 用于已知目标投票时加入
- `POST /api/votes/join-by-invite` 用于仅凭邀请码定位并加入投票
- 退出接口同时支持 `POST` 和 `DELETE`

### 4.6 邀请码管理

- `GET /api/votes/{voteId}/invite`
- `POST /api/votes/{voteId}/invite/reset`
- `PATCH /api/votes/{voteId}/invite/settings`
- `PUT /api/votes/{voteId}/invite/settings`

说明：

- 仅创建者或管理员可见/可操作
- `PUT` 与 `PATCH` 当前都可用

### 4.7 提交投票

- `POST /api/votes/{voteId}/vote`
- 需要登录

`CHOICE` 模式请求体示例：

```json
{
  "optionIds": [1, 2]
}
```

`SLIDER` 模式请求体示例：

```json
{
  "optionScores": {
    "1": 80,
    "2": 95
  }
}
```

当前行为说明：

- 同一投票允许一次提交多个不同选项
- 同一请求内不允许重复选择同一选项
- 数据库层对重复投票记录有唯一性约束兜底
- Redis 幂等键失败时会降级，不阻断主流程

## 5. 管理员接口

### 5.1 访问校验、概览与确认令牌

- `GET /api/admin/access`
- `GET /api/admin/dashboard`
- `POST /api/admin/confirm`
- `POST /api/admin/confirmations`：兼容别名

说明：

- `GET /api/admin/access` 是轻量权限探针，仅用于判断当前登录用户是否具备管理员访问权限。
- 该接口适合前端菜单显示、路由守卫、后台入口可见性判断等场景。
- `GET /api/admin/dashboard` 用于真正加载管理员后台统计数据，不再兼做权限探测。
- 上述接口都要求管理员权限；未登录或无权限时返回 `401 / 403`。

`GET /api/admin/access` 成功响应示例：

```json
{
  "success": true,
  "message": "获取成功",
  "data": {
    "allowed": true
  }
}
```

确认令牌成功响应示例：

```json
{
  "success": true,
  "message": "确认令牌签发成功",
  "data": {
    "token": "confirm-token"
  }
}
```

### 5.2 用户管理

- `GET /api/admin/users`
- `PATCH /api/admin/users/{userId}`
- `PUT /api/admin/users/{userId}`
- `PATCH /api/admin/users/{userId}/role`
- `PUT /api/admin/users/{userId}/role`
- `PATCH /api/admin/users/{userId}/status`
- `PUT /api/admin/users/{userId}/status`
- `DELETE /api/admin/users`

### 5.3 投票管理

- `PATCH /api/admin/votes/{voteId}/rule`
- `PUT /api/admin/votes/{voteId}/rule`
- `POST /api/admin/votes/{voteId}/terminate`

## 6. 当前未实现接口

以下接口在当前已跟踪后端源码中未稳定实现：

- `POST /api/auth/change-initial-password`
- `DELETE /api/votes/{voteId}`
- refresh token 相关接口

如果前端或外部集成依赖这些接口，需要先补实现，不能以本文件为依据直接调用。
