# 多平台完善方案（渐进式）

## Context

当前项目仅有一套 Vue 3 + Vite PC Web 前端，缺少移动端适配和微信小程序。用户需要完善三个平台：PC Web、移动端、微信小程序，以提升简历项目的完整性和技术广度。

采用**渐进式**策略，分三个阶段交付，每个阶段产出可独立演示的成果。

---

## 总体架构

```
                    Spring Boot 后端 (8080)
                    /        |         \
                   /         |          \
          Vue 3 Web    uni-app 小程序   PWA (移动端)
          (PC+移动)    (微信小程序)     (可安装到手机)
```

- **Phase 1**: 现有 Vue 3 项目改造为响应式 + PWA，一套代码覆盖 PC 和移动端 Web
- **Phase 2**: 新建 uni-app 项目，编译到微信小程序，复用后端 API
- **Phase 3**: 移动端 App（uni-app 编译或 PWA 打包）

---

## Phase 1: PC Web 响应式 + PWA 移动端（当前阶段）

### 1.1 响应式布局改造

**目标文件**: `frontend/src/assets/style.css`, `frontend/src/App.vue`, 所有 View 组件

- 全局 CSS 变量系统，统一色彩、间距、圆角、阴影
- 移动端 375px 断点适配：导航栏改为底部 TabBar，表单全宽，卡片堆叠
- 平板 768px 断点适配：双列布局
- PC 1024px+ 断点：保持现有布局
- 字体大小响应式缩放（`clamp()` 函数）
- 触摸友好的按钮最小尺寸 44x44px

### 1.2 PWA 支持

**新文件**: `frontend/public/manifest.json`, `frontend/public/sw.js`, `frontend/src/utils/pwa.js`

- 添加 Web App Manifest（图标、名称、主题色、全屏模式）
- 注册 Service Worker 实现离线缓存和快速加载
- 添加"添加到主屏幕"提示（移动端浏览器）
- 离线时显示自定义 fallback 页面

### 1.3 移动端交互优化

**目标文件**: `frontend/src/App.vue`, `frontend/src/router/index.js`

- 移动端底部 TabBar 导航（4个标签：AI问诊、科室医生、预约挂号、我的）
- 页面切换动画（滑动过渡）
- 下拉刷新（ChatView 消息列表）
- 骨架屏（Skeleton）加载状态：首次加载时显示占位动画
- Toast 全局通知组件（成功/错误/警告）
- 全局错误边界（ErrorBoundary 组件），捕获未处理的 Vue 错误

### 1.4 用户引导

**新文件**: `frontend/src/components/OnboardingGuide.vue`

- 首次访问时显示 3-4 步功能引导（swipe 卡片形式）
- 存储引导状态到 localStorage，仅首次显示
- 关键按钮添加 tooltip 提示

### 1.5 性能优化

- 路由懒加载（动态 import）
- 图片/图标使用 SVG sprite 减少请求
- CSS 动画使用 `transform` 和 `opacity`（GPU 加速）
- 聊天消息列表使用虚拟滚动（长列表优化）

---

## Phase 2: 微信小程序（后续阶段）

### 2.1 uni-app 项目搭建

**新目录**: `miniprogram/`（独立 uni-app 项目）

- 使用 uni-app CLI（Vue 3 版本）创建项目
- 配置 `manifest.json`（微信小程序 AppID、权限）
- 配置 `pages.json`（页面路由、TabBar、窗口样式）
- 复用后端 API：uni-app 使用 `uni.request` 替代 Axios

### 2.2 页面实现

- 首页：AI 问诊对话（SSE 流式响应适配 uni-app）
- 科室医生列表页
- 预约挂号页
- 我的预约页
- 登录/注册页
- 个人中心页

### 2.3 微信特有功能

- 微信一键登录（`wx.login` + 后端换取 openid）
- 微信手机号授权
- 微信支付（预约挂号付费）
- 订阅消息（预约提醒）
- 分享给微信好友

---

## Phase 3: 移动端 App（后续阶段）

- 从 Phase 2 的 uni-app 项目编译到 Android/iOS
- 或使用 Phase 1 的 PWA 通过 TWA（Trusted Web Activity）打包为 APK
- 添加原生功能：推送通知、生物识别登录

---

## 当前执行：Phase 1 文件清单

| 类别 | 文件 | 操作 |
|------|------|------|
| 全局样式 | `frontend/src/assets/style.css` | 重写：响应式 CSS 变量 + 媒体查询 |
| 根组件 | `frontend/src/App.vue` | 修改：移动端底部 TabBar、页面切换动画 |
| 路由 | `frontend/src/router/index.js` | 修改：路由懒加载 |
| 布局 | `frontend/src/views/ChatView.vue` | 修改：移动端布局适配 |
| 布局 | `frontend/src/views/DepartmentView.vue` | 修改：移动端卡片布局 |
| 布局 | `frontend/src/views/AppointmentView.vue` | 修改：移动端表单适配 |
| 布局 | `frontend/src/views/MyAppointments.vue` | 修改：移动端列表适配 |
| 布局 | `frontend/src/views/LoginView.vue` | 修改：移动端全宽表单 |
| 布局 | `frontend/src/views/RegisterView.vue` | 修改：移动端全宽表单 |
| 新组件 | `frontend/src/components/Toast.vue` | 新建：全局 Toast 通知 |
| 新组件 | `frontend/src/components/Skeleton.vue` | 新建：骨架屏加载组件 |
| 新组件 | `frontend/src/components/ErrorBoundary.vue` | 新建：错误边界组件 |
| 新组件 | `frontend/src/components/OnboardingGuide.vue` | 新建：首次引导组件 |
| PWA | `frontend/public/manifest.json` | 新建：PWA 清单 |
| PWA | `frontend/public/sw.js` | 新建：Service Worker |
| PWA | `frontend/src/utils/pwa.js` | 新建：PWA 注册逻辑 |
| 入口 | `frontend/index.html` | 修改：添加 PWA meta 标签、viewport |
| 入口 | `frontend/src/main.js` | 修改：注册全局组件、PWA 初始化 |

---

## 验证方式

1. **响应式测试**: 浏览器 DevTools 切换 375px / 768px / 1440px 视口，验证布局完整性
2. **PWA 测试**: Chrome Lighthouse PWA 评分 > 90；移动端浏览器"添加到主屏幕"
3. **功能测试**: 登录→问诊→查看科室→预约→查看预约→取消预约 完整流程
4. **性能测试**: Lighthouse Performance 评分 > 80
5. **离线测试**: 断开网络后刷新页面，显示自定义离线页面