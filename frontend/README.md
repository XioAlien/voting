# Frontend 模块说明

当前前端是一个基于 React 18、TypeScript、Vite 和 Ant Design 的单页应用，对应当前后端已实现的认证、邀请码和管理员能力。

## 当前页面

- `/`
  - 投票列表首页
- `/vote/:id`
  - 投票详情、邀请码加入与邀请码管理页
- `/login`
  - 登录页
- `/register`
  - 注册页
- `/admin`
  - 管理与创建中心；已登录用户可创建投票，管理员额外看到概览和用户管理

## 当前功能

- 调用后端真实注册接口
- 调用后端真实登录接口
- 登录成功后将 JWT 写入 `localStorage`
- 请求拦截器自动附带 Bearer Token
- 支持查看投票列表、投票详情和结果
- 支持邀请码加入投票与邀请码受限投票访问
- 支持创建者或管理员管理邀请码设置与重置邀请码
- 支持管理员概览和用户管理面板
- 支持创建 `CHOICE` 与 `SLIDER` 投票
- 支持在允许的情况下添加自定义选项
- 支持提交选择型或评分型投票

## 当前未实现

- 前端自动化测试
- bootstrap-admin 首次改密引导

## 启动方式

推荐直接单独启动前端开发服务：

```powershell
cd frontend
npm install
npm run dev
```

默认访问地址：

- `http://localhost:5173/`

## 环境变量

- 前端 `.env` 不是必需项；如需覆盖默认值，可自行创建 `frontend/.env`
- 默认不设置 `VITE_API_BASE_URL` 时，前端继续通过 Vite 代理把 `/api` 转发到 `VITE_PROXY_TARGET`
- 默认 `VITE_PROXY_TARGET=http://localhost:8080`
- 如需直连其他后端地址，可在 `frontend/.env` 中设置 `VITE_API_BASE_URL=http://127.0.0.1:8080`
- `VITE_PORT` 默认是 `5173`

## 可用命令

```powershell
npm run check
```

```powershell
npm run lint
```

```powershell
npm run build
```

说明：

- 当前没有 `npm test`

## 与后端的对接约定

- 登录、注册、创建投票、添加选项、提交投票都使用后端真实接口
- 邀请码加入、邀请码管理、管理员能力都使用后端真实接口
- JWT 从 `localStorage` 读取
- 受保护接口通过 `Authorization: Bearer <token>` 访问
- `src/api/http.ts` 默认保留相对路径请求，只有显式设置 `VITE_API_BASE_URL` 时才改为自定义后端基地址

## 关键文件

- `src/App.tsx`
  - 路由定义
- `src/api/http.ts`
  - axios 实例与 token 注入
- `src/pages/Login.tsx`
  - 登录页
- `src/pages/Register.tsx`
  - 注册页
- `src/pages/Home.tsx`
  - 首页
- `src/pages/VoteDetail.tsx`
  - 投票详情页
- `src/pages/Admin.tsx`
  - 管理与创建中心页面
