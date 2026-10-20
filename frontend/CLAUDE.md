# 校园优购 · 前端（Vue 3 桌面网页版）

黑马点评定制项目「校园优购」的桌面网页前端。旧 H5 站点在 `../nginx-1.18.0/html/hmdp/`，**不要修改或删除**，仅作参考。

## 运行

```bash
npm install
npm run dev      # http://localhost:5173，/api 代理到 http://localhost:8081
npm run build    # 产出 dist/，nginx 指向这里
```

开发前需要：MySQL、WSL 里的 Redis/RabbitMQ、Spring Boot（8081）。AI 聊天还需要 WSL 里 FastAPI（8000）。

## 技术栈

Vue 3 `<script setup>` + Vite + Vue Router 4 + Pinia + Element Plus + axios + SCSS。
**纯 JavaScript，不用 TypeScript。**

## 目录约定

```
public/imgs/          图片资源，后端上传目录也指向这里
src/api/              每个后端模块一个文件，只做请求封装和参数整形
src/components/       跨页面复用组件（PascalCase.vue）
src/composables/      useInfiniteScroll / useImageCompress
src/router/index.js   路由 + 鉴权守卫 + document.title
src/stores/auth.js    登录态（token + 用户信息）
src/styles/           tokens.scss（设计变量）/ base.scss / element.scss
src/utils/format.js   金额、时间、图片串等格式化
src/views/            页面组件，一页一个文件，后缀 View.vue
```

## 接口与鉴权

- 后端无 CORS，开发走 Vite 代理：`/api` → `http://localhost:8081`，并去掉 `/api` 前缀。
- 鉴权头是 `authorization: <token>`，**没有 Bearer 前缀**；token 存 `localStorage["campus_token"]`。
- 后端统一返回 `{ success, errorMsg, data }`。`request.js` 已解包 `data`；业务失败时 HTTP 仍是 200，靠 `success:false` 判断并弹 `ElMessage.error`。
- 401 由响应拦截器处理：清 token → 跳 `/login?redirect=<当前路径>`。需要在 401 时静默的请求传 `{ silent: true }`。
- 关注接口是 **POST**：`POST /follow/:id/:isfollow`、`POST /follow/or/not/:id`，不要用旧站的 PUT/GET。
- 金额单位是**分**，展示一律用 `formatPrice()`。
- 图片字段是逗号分隔字符串，用 `splitImages()` 拆。

## 样式约定

- 颜色只用 `tokens.scss` 里的变量（奶油底色 + 珊瑚橙/黄油/薄荷/薰衣草），不要写死新色值。
- 页面根元素 `.page`，内容容器 `.container`（1200px）；卡片 `.card`。
- 大圆角、软阴影、hover 轻微上浮；避免纯黑纯白。
- 响应式断点：1000px（两栏变单栏）、640px（单列）。不要出现横向滚动。
- Element Plus 主题在 `element.scss` 覆盖，组件内不要再改主色。

## 已修复的旧站问题（不要改回去）

- 关注接口改 POST，修复 405。
- 金额按分换算；旧实现有格式错误。
- 上传用标准 FormData，不手写 multipart boundary；删除图片时去掉 `/imgs` 前缀。
- 分页用 `useInfiniteScroll` 的 loading 保护，修复并发重复请求和失败继续翻页。
- 不实现后端没有的功能（密码登录、评论、资料保存），用「暂未开放」明确说明，不做假交互。
