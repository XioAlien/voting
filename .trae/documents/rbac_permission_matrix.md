# RBAC 权限矩阵（归档）

本文件描述的是历史规划中的管理员权限模型，不代表当前代码现状。

## 当前状态

当前代码中：

- 已存在 `ADMIN` 专用接口
- 已存在 `/api/admin/**`
- 已存在二次确认令牌
- 但尚未形成独立、细粒度的 RBAC 体系与正式权限矩阵文档

因此，本文件不能作为当前权限设计说明。

## 当前实际权限边界

当前仅存在以下安全边界：

- `POST /api/auth/login` 与 `POST /api/auth/register` 允许匿名访问
- `GET|POST /api/auth/dev-token` 在 `dev/local` 且显式开启时允许访问
- `GET /api/votes/**` 允许匿名访问
- `/api/admin/**` 要求管理员权限
- 其他写接口要求登录

更准确的当前语义请查看：

- [README.md](file:///d:/code/java/voting/README.md)
- [api_contract.md](file:///d:/code/java/voting/.trae/documents/api_contract.md)
- [security_baseline.md](file:///d:/code/java/voting/.trae/documents/security_baseline.md)
