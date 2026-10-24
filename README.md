# TGPlayer

> 非官方 Telegram 视频播放器 · Android 手机 / 平板 / TV · 中文界面

TGPlayer 用你自己的 Telegram 账号登录，浏览聊天、频道和群组里的视频，支持边下边播、离线下载、收藏、字幕与多账号。它基于 [TMPlayer](https://github.com/dracu-lah/TMPlayer) 的核心思路重建，播放器与 UI 参考了 NextPlayer 和 Findroid。

![version](https://img.shields.io/badge/version-0.22.0-blue) ![license](https://img.shields.io/badge/license-GPL--3.0--or--later-green) ![platform](https://img.shields.io/badge/platform-Android%208.0%2B%20arm64-lightgrey)

## 架构图

![TGPlayer 架构图](docs/architecture.svg)

> 上图由 archify 生成；交互版（可缩放、悬停/深色模式）见 [docs/architecture.html](docs/architecture.html)，分层说明见 [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)。

## 功能

- **登录**：扫码 / 手机号 + 验证码 / 两步验证，TDLib 会话只存在应用私有目录
- **多账号**：账号槽位切换、添加、移除；重复登录同一账号会按 Telegram user id 自动合并
- **媒体库**：频道 / 群组 / 私聊筛选，最近 / 名称排序，聊天内搜索，`#标签` 筛选
- **收藏**：收藏整个聊天或单条视频；入口在聊天列表、频道顶栏、视频卡和播放器
- **播放**：NextPlayerCore（ExoPlayer + nextlib FFmpeg 软解回退），边下边播；手势、锁定、倍速、音轨、内嵌/本地字幕、画面比例、画中画
- **播放队列**：按当前筛选结果组成队列，播完自动下一个，顶栏可手动切换
- **下载**：WorkManager 前台任务，通知进度，最多 2 个并发；暂停 / 恢复 / 删除 / 离线播放；仅 Wi-Fi 与存储不足约束
- **首页**：继续观看 hero、横滑分区、收藏与本地视频入口
- **本地播放**：系统文件选择器（SAF）打开本地视频
- **设置**：主题（跟随系统/深色/浅色 + 动态取色）、默认倍速、记住进度、节省流量、高清封面、缓存清理、诊断
- **首次启动引导**：4 页说明（欢迎 / 登录 / 影视库 / 下载与省流）
- **适配**：edge-to-edge、predictive back、手机底栏 / 平板与 TV 侧栏、arm64 16 KB page 对齐

## 技术栈

| 项 | 版本 |
|---|---|
| Kotlin / AGP / Gradle | 2.4.21 / 9.4.1 / 9.6.1 |
| Android | minSdk 26 · targetSdk 35 · compileSdk 37 |
| UI | Jetpack Compose + Material 3（Compose BOM 2026.09.00） |
| 播放 | Media3 ExoPlayer 1.11.1 + nextlib-media3ext 1.11.1-0.16.0 |
| Telegram | tdl-coroutines 15.0.0（libtdjson） |
| 后台 | WorkManager 2.11.0 |
| 二维码 | ZXing core 3.5.4 |
| ABI | 仅 arm64-v8a |

## 架构

```
UI (Compose)  →  ViewModel (Chats / Videos)  →  MediaRepository  →  Td (TDLib)
                                                       ↓
Player (NextPlayerCore) → TdDataSource → TdByteWindow → TDLib → Telegram
Downloads (WorkManager Worker) → TDLib downloadFile → Downloads/
```

- 数据页统一使用 `UiState`（Loading / Ready / Failed）与公共的 `LoadingView / ErrorView / EmptyView / SkeletonList`
- 依赖方向：UI → repository/player → data；`AppContainer` 提供手动依赖注入，ViewModel 通过 Factory 注入
- 详细说明：[`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md)，可交互图：[`docs/architecture.html`](docs/architecture.html)

## 目录

```
app/src/main/java/com/tgplayer/android/
  data/            TDLib、账号、下载、收藏、偏好、Repository
  player/          NextPlayerCore、PlayerScreen、TdDataSource、TdByteWindow
  ui/              首页、媒体、视频库、搜索、下载、设置、收藏、引导
  ui/chats|videos  ViewModel
  ui/common/       UiState、加载组件、缩略图缓存、动效
  ui/findroid/     移植的 Findroid 卡片组件
  di/              AppContainer
docs/              架构、计划、技能审计、架构图
design/            设计板、UI 规范、图标数据
```

## 构建

### 环境

- JDK **21**
- Android SDK **37**、Build-Tools 36
- 目标设备 **arm64-v8a**（默认只打这一种 ABI）

### 配置凭据

```bash
cp local.properties.example local.properties
```

在 `local.properties` 填入自己的 `TG_API_ID` / `TG_API_HASH`（https://my.telegram.org）。该文件已被 gitignore，不会进仓库或源码包。

### 命令

```bash
./gradlew :app:assembleDebug        # 调试 APK
./gradlew :app:assembleRelease      # R8 release（未签名）
./gradlew :app:testDebugUnitTest    # JVM 单测
./gradlew :app:lintDebug            # Lint
./gradlew :app:assembleDebug -PtmAbis=arm64-v8a,armeabi-v7a
```

产物：

- Debug：`app/build/outputs/apk/debug/app-debug.apk`（约 46 MB）
- Release（R8）：`app/build/outputs/apk/release/app-release-unsigned.apk`（约 32 MB）

## 质量状态（0.21.0）

| 检查 | 结果 |
|---|---|
| `assembleDebug` | 通过 |
| `assembleRelease`（R8 + shrinkResources） | 通过，32 MB |
| `testDebugUnitTest` | 15 个用例通过 |
| `lintDebug` | 0 error，33 warning（依赖版本、KTX 建议等非阻断） |
| 16 KB page 对齐 | 已校验所有 `.so` 的 `LOAD align=0x4000` |
| 应用名 | `application-label: TGPlayer` |

> 说明：以上是构建/静态检查结果，UI 与播放仍未在真机做完整回归；视频列表、收藏、下载速度等运行时问题需要设备日志确认。

## 已知限制 / 路线图

- 包名已迁移到 `com.tgplayer.android`：系统会把它视为**新应用**，旧 `com.tmplayer.android` 的登录、收藏、下载数据不会继承，旧版可卸载


- Home / Search 仍直接调用 `Td`，下一步抽 ViewModel；尚未模块化、没有 baseline profile
- 无 Compose UI / instrumentation 测试
- 投屏、在线字幕已按需求放弃；播放器只支持 Telegram 单文件流，没有多清晰度切换
- 下载使用当前账号会话；切号时进行中的任务可能失败重试

## 第三方与许可

- 上游思路：[TMPlayer](https://github.com/dracu-lah/TMPlayer)（GPL-3.0-or-later）
- 播放器：[NextPlayer](https://github.com/anilbeesetti/nextplayer)（GPL-3.0，见 `NOTICE-NEXTPLAYER.md`）
- UI 组件：[Findroid](https://github.com/jarnedemeulemeester/findroid)（GPL-3.0，见 `NOTICE-FINDROID.md`）
- 图标：react-icons（Feather / Lucide，见 `design/icons-NOTICE.md`）
- 本项目以 **GPL-3.0-or-later** 发布，保留上游版权与来源说明

## 文档

- [`docs/DEV-PLAN.md`](docs/DEV-PLAN.md) 开发说明与下一步计划
- [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) 分层与依赖约定
- [`docs/SKILL-AUDIT.md`](docs/SKILL-AUDIT.md) compose-skill / awesome-android-agent-skills 审计报告
- [`design/UI-SPEC.md`](design/UI-SPEC.md) 设计规范
