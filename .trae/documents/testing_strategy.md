# 测试策略与质量门禁

本文档描述当前仓库已经具备的测试能力，以及建议补齐的方向，不把未落地测试写成现状。

## 1. 当前测试现状

### 1.1 后端

当前仓库中已有 6 个后端测试文件、32 个 `@Test`，主要包括：

- `BootstrapAdminInitializerTest`
- `DevTokenBlockFilterTest`
- `SensitiveConfigStartupValidatorTest`
- `VoteServiceTest`
- `AdminInviteServicesTest`
- `RestAuthenticationEntryPointTest`

已覆盖的重点包括：

- Bootstrap Admin 初始化与敏感配置校验
- `dev-token` 在开发/生产环境下的开关限制
- 投票多选与数据库级防重约束
- Redis 幂等降级与部分缓存回退行为
- 邀请码管理、确认令牌与管理员相关链路
- `401` 入口文案区分

### 1.2 前端

当前前端没有自动化测试文件，也没有 `npm test` 脚本。

### 1.3 脚本

当前仓库中存在脚本相关说明，但 `scripts/` 目录已被定义为本地临时脚本目录，不作为 Git 跟踪内容的一部分，因此不将脚本测试列为当前稳定门禁。

## 2. 当前可执行的质量检查

### 2.1 后端

```powershell
cd backend
mvn test
```

```powershell
cd backend
mvn -DskipTests package
```

### 2.2 前端

```powershell
cd frontend
npm run check
```

```powershell
cd frontend
npm run lint
```

```powershell
cd frontend
npm run build
```

## 3. 当前建议回归场景

### 3.1 认证

- 注册成功
- 重复注册返回 `409`
- 登录成功
- 错误密码返回 `400`
- 非法 JSON 返回 `400`
- 未登录访问写接口返回 `401`
- 非法 token 返回 `401`
- 过期 token 返回 `401`

### 3.2 投票

- 创建 `CHOICE` 投票
- 创建 `SLIDER` 投票
- 允许一次提交多个不同选项
- 同一请求重复选项返回 `400`
- 重复投票返回 `409`
- 已结束投票返回 `409`
- Redis 不可用时主流程仍可继续

## 4. 当前未落地项

以下内容目前仍是建议，不是现状：

- 前端单元测试与页面行为测试
- Web 层控制器测试体系
- CI 自动执行门禁
- 覆盖率阈值检查
- 端到端测试

## 5. 后续建议

- 补前端测试框架与 `npm test`
- 为关键控制器补 Web 层测试
- 将现有命令接入 CI
- 按模块逐步引入覆盖率统计

