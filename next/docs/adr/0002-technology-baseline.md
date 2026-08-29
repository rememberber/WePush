# ADR-0002：Service、WebUI 与 Desktop 技术基线

- 状态：已接受
- 日期：2026-08-22
- 决策者：WePush 项目维护者

## 背景

WePush Next 需要同时提供 Service、WebUI 和 Desktop UI。技术基线必须兼顾 Java 团队的开发效率、交互式管理界面的复杂度、Web 与 Desktop 复用、跨平台发布和长期安全更新。

## 决策

### Service

- Java 基线固定为 Java 21。
- Service Web 框架固定为 Spring Boot 4.1.x，初始实现版本固定为 4.1.1。
- 使用 Spring MVC、Spring Security、Actuator、Validation 和 JDBC/DataSource 集成。
- Service 以可执行 JAR 或应用镜像运行，不部署传统 WAR。
- Spring 依赖只允许存在于 Service App、Web 和 Infrastructure 层，不进入 Core API、Core Engine、Provider SPI、Agent Runtime、Java SDK 或 Embedded SDK。
- Spring Boot 小版本和补丁版本可以在兼容测试通过后更新；升级 4.2 或更高特性线必须单独评审。

### WebUI

- 语言固定为 TypeScript，初始基线为 TypeScript 6.0.x，并启用严格模式。
- 构建工具固定为 Vite 8.1.x。
- UI 框架固定为 React 19.2.x。
- 构建运行时固定为 Node.js 24 LTS，不使用非 LTS Node 作为 CI 和正式构建基线。
- 包管理器固定为 pnpm，通过 Corepack 固定项目使用的精确版本，并提交唯一 Lockfile。
- 样式基线使用 Tailwind CSS。
- shadcn/ui 按需引入，不把完整组件集合一次性加入项目。通过 shadcn CLI 引入的组件源码归 WePush Next 自己维护。
- WebUI 是纯 SPA，通过 Service API 工作，不使用 React Server Components，也不要求 Node.js 作为正式运行时。
- OpenAPI 生成 TypeScript API Client，业务代码通过手写 Facade 使用生成客户端。

### Desktop UI

- Desktop 外壳固定为 Electron。
- 初始 Electron 基线固定为 43.x，在创建工程时锁定当时最新的 43.x 安全补丁版本。
- Desktop 复用 WebUI 的 React 页面、Schema Renderer、API Client 和设计 Token。
- Electron Main、Preload 和 Renderer 分层；Renderer 不直接获得 Node.js 权限。
- 强制启用 `contextIsolation` 和 Renderer Sandbox，关闭 `nodeIntegration`。
- Preload 只暴露经过白名单定义、参数校验和来源校验的最小 IPC API。
- Desktop 只加载发行包内的本地 UI 资源，并通过 GitHub Release 与 `SHA256SUMS` 验证包完整性；不得加载带 Node 能力的远程页面。应用不以商业代码签名作为发行前提。
- Electron 每个受支持主版本生命周期较短，项目持续跟随受支持稳定版本，至少每季度评估一次主版本升级。

## 前端目录

```text
next/ui/
├── pnpm-workspace.yaml
├── package.json
├── pnpm-lock.yaml
├── apps/
│   ├── web/
│   └── desktop/
└── packages/
    ├── api-client/
    ├── schema-renderer/
    ├── ui/
    ├── features/
    └── design-tokens/
```

`apps/web` 和 `apps/desktop` 可以共享 `packages/*`，但共享仅发生在 Next 内部，不影响 Classic 的独立性。

## 结果

### 正面影响

- WebUI 和 Desktop 可以共享大部分交互与业务前端代码。
- Vite 和 React 适合动态 Schema 表单、运行监控和交互式 API 文档。
- Electron 提供成熟的跨平台桌面能力和系统集成能力。
- Spring Boot 统一 Service 的 API、安全、事务、健康检查和部署生命周期。
- Core、Agent Runtime 和 Provider 保持框架无关，未来替换 Service 框架不会推翻执行架构。

### 接受的代价

- Electron 安装包和内存占用高于轻量 WebView 外壳。
- Electron 需要跟随快速发布节奏持续升级。
- 项目同时维护 Maven 和 pnpm 两套构建体系。
- shadcn/ui 组件是项目源码，升级和无障碍质量由项目自行负责。

## 参考

- [Spring Boot](https://spring.io/projects/spring-boot/)
- [React Versions](https://react.dev/versions)
- [Vite 8](https://vite.dev/blog/announcing-vite8)
- [Node.js Releases](https://nodejs.org/en/about/previous-releases)
- [Electron Releases](https://releases.electronjs.org/)
- [Electron Security](https://www.electronjs.org/docs/latest/tutorial/security)
- [shadcn/ui for Vite](https://ui.shadcn.com/docs/installation/vite)
