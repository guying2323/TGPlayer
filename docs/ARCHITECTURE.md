# 架构说明（0.15.0）

> 可交互架构图：`docs/architecture.html`（由 archify 生成，candidate 在 `.archify/architecture-tmplayer-rebuild-20261023084425/candidate.json`）。

## 分层

| 层 | 职责 | 主要文件 |
|---|---|---|
| UI | Compose 页面、Findroid 主题/卡片、统一加载组件、首次启动引导 | `ui/AppRoot.kt`、`ui/HomeScreen.kt`、`ui/ChatsScreen.kt`、`ui/VideosScreen.kt`、`ui/Onboarding.kt`、`ui/common/UiState.kt`、`ui/findroid/FindroidComponents.kt` |
| 播放 | NextPlayer 核心 + 移植的控制层、TDLib 流式字节窗口 | `player/NextPlayerCore.kt`、`player/PlayerScreen.kt`、`player/TdDataSource.kt`、`player/TdByteWindow.kt` |
| 领域/数据 | TDLib 会话、聊天/视频检索、账号槽位、下载队列、偏好 | `data/Td.kt`、`data/Accounts.kt`、`data/Downloads.kt`、`data/DownloadWorker.kt`、`data/Settings.kt` |
| 平台 | WorkManager、FileProvider、SAF、通知 | `AndroidManifest.xml`、`data/DownloadWorker.kt` |

依赖方向：UI → data/player，player → data，data → TDLib/平台；不允许反向依赖。

## 统一状态与加载

所有数据页使用 `ui/common/UiState.kt`：

- `UiState.Loading / Ready<T> / Failed(message)`
- `rememberLoadable(keys…) { suspend load }` 返回 `(state, retry)`
- 展示组件：`LoadingView`、`ErrorView`、`EmptyView`、`SkeletonList`（shimmer 与卡片共用）

约定：页面不再自己写 spinner / 错误文案 / 重试按钮；新增数据页直接接 `rememberLoadable`。

## 首次启动引导

`OnboardingScreen` 四页（欢迎 / 登录 / 影视库 / 下载与省流），取自 TMPlayer 的 setup 思路：一页一个决定，最后一页直接写偏好；完成后 `Settings.onboardingDone = true`，进入登录。

## 播放链路

`PlayerScreen → NextPlayerCore（ExoPlayer + nextlib）→ TdDataSource → TdByteWindow → Td.file/downloadFile → TDLib → Telegram`

下载链路：`Downloads.enqueue → WorkManager DownloadWorker → Td.downloadFile → 完成后复制到 Downloads/`。

## 账号

`Accounts` 保存槽位（id/name/rootDir/userId），`Td` 单客户端串行切换；登录后按 Telegram user id 去重，重复账号自动合并。
