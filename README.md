# 校园优购 · Campus Yougou

面向星城大学的校园生活服务平台：逛店铺、抢优惠券、发探店动态、关注同学，还有一个能聊天、能看照片认店的 AI 助手。

后端在其基础上补齐了 AI 助手、动态图片上传、关注流等能力，前端重写为 **Vue 3 桌面网页版**。

## 功能

- **店铺**：按分类浏览、关键字搜索、按校区距离由近到远展示（Redis GEO），店铺详情与优惠券
- **优惠券秒杀**：Redis + Lua 预扣库存，异步创建订单，一人一单校验
- **校园动态**：发布带图动态、点赞、关注、关注流 Feed（RabbitMQ 异步推送，游标分页）
- **用户**：手机号验证码登录、个人主页、他人主页、共同关注
- **AI 助手**：多轮对话（携带最近 6 条历史）、上传照片识别店铺并推荐，由独立的 FastAPI 服务提供模型能力
- **图片上传**：动态配图上传/删除，上传目录统一到前端静态资源目录

## 技术栈

| 层 | 技术 |
|---|---|
| 后端 | Java 8、Spring Boot 2.3、MyBatis-Plus 3.4、Redis（Lettuce + Redisson）、RabbitMQ、MySQL 8 |
| 前端 | Vue 3 `<script setup>`、Vite 6、Vue Router 4、Pinia、Element Plus、axios、SCSS |
| AI | 独立 FastAPI 服务（默认 `localhost:8000`），后端通过 RestTemplate 转发 |
| 部署 | 内置 nginx（`nginx-1.18.0/`），静态资源 + `/api` 反向代理 |

## 目录结构

```
hm-dianping/
├── src/main/java/com/hmdp/        # 后端源码
│   ├── controller/                # 接口层
│   ├── service/impl/              # 业务实现（秒杀、Feed、关注、AI 转发等）
│   ├── config/                    # Redisson、MyBatis-Plus、拦截器配置
│   └── utils/                     # 登录拦截、Redis key、常量
├── src/main/resources/
│   ├── application.yaml           # 数据库 / Redis / RabbitMQ 配置
│   ├── seckill.lua  unlock.lua    # 秒杀与分布式锁脚本
│   └── db/hmdp.sql                # 建库建表 + 初始数据
├── frontend/                      # Vue 3 前端（Vue 3 桌面版）
│   ├── src/api/                   # 按后端模块拆分的请求封装
│   ├── src/views/                 # 10 个页面
│   ├── src/components/            # 通用组件
│   ├── public/imgs/               # 图片资源，也是后端上传目录
│   └── CLAUDE.md                  # 前端目录 / 接口 / 样式约定
├── nginx-1.18.0/                  # 本地部署用 nginx
└── hm-dianping-openapi.json       # 接口文档（可导入 Apifox / Postman）
```

前端旧版 H5 页面保留在 `nginx-1.18.0/html/hmdp/`，仅作参考，不再维护。

## 快速开始

### 环境要求

- JDK 8、Maven 3.6+
- MySQL 8、Redis 6+、RabbitMQ 3.x
- Node.js 18+（Vite 6）

### 1. 初始化数据库

```bash
mysql -u root -p -e "CREATE DATABASE hmdp DEFAULT CHARSET utf8mb4;"
mysql -u root -p hmdp < src/main/resources/db/hmdp.sql
```

### 2. 修改配置

- `src/main/resources/application.yaml`：MySQL、Redis、RabbitMQ 的地址和密码
- `src/main/java/com/hmdp/config/RedissonConfig.java`：Redisson 目前单独硬编码了 Redis 地址和密码，修改 Redis 时两处都要改
- `src/main/java/com/hmdp/utils/SystemConstants.java`：`IMAGE_UPLOAD_DIR` 是上传目录的绝对路径，默认指向 `frontend/public/imgs/`，换机器需要同步修改

### 3. 启动后端

```bash
mvn spring-boot:run
```

服务监听 `8081`。

### 4. 启动前端

```bash
cd frontend
npm install
npm run dev
```

访问 `http://localhost:5173`。开发环境由 Vite 把 `/api` 代理到 `http://localhost:8081`，无需处理 CORS。

### 5. 登录

只支持「手机号 + 验证码」。验证码不会真的发短信，而是打印在后端控制台日志里，搜索「验证码」即可看到。新手机号会自动注册。

### 6. AI 助手（可选）

AI 聊天依赖单独的 FastAPI 服务，默认地址见 `SmartChatController`：

- `POST http://localhost:8000/chat`：文本对话
- `POST http://localhost:8000/chat-image`：图片识别

未启动时前端仍可使用其他功能，AI 页面会提示服务不可用。该服务使用什么模型、API Key 如何配置，见对应 Python 项目。

### 7. 生产构建与 nginx

```bash
cd frontend && npm run build     # 产出 frontend/dist
cd ../nginx-1.18.0 && ./nginx.exe
```

访问 `http://localhost:8080`。nginx 已配置：

- `/` → `../frontend/dist`，`try_files` 支持前端路由刷新
- `/imgs/` → `../frontend/public/imgs/`
- `/api` → `http://127.0.0.1:8081`（去掉 `/api` 前缀）

## 接口约定

- 统一返回 `{ "success": boolean, "errorMsg": string, "data": any }`；业务失败时 HTTP 状态仍是 200，以 `success` 为准
- 登录后请求头携带 `authorization: <token>`，**没有 Bearer 前缀**；token 由后端写入 Redis 并设置有效期
- 未登录或 token 过期返回 HTTP 401
- 金额字段（`payValue`、`actualValue`）单位是**分**
- 图片字段是逗号分隔的相对路径，例如 `/imgs/blogs/xxx.jpg`

## 已知限制

- 密码登录未实现，只有验证码登录
- 资料编辑页为只读展示，后端没有资料更新接口
- 店铺列表只支持按距离排序；旧版「人气 / 评分」排序按钮后端并未实现，前端已移除
- 评论功能只有展示占位，没有评论接口
- 上传目录是 Windows 绝对路径，部署到其他系统需要调整

## 说明

- 店铺、动态、用户等初始数据来自课程素材，仅用于演示
- 前端图片资源合计约 8 MB，随仓库一起提交，保证克隆后开箱即用
