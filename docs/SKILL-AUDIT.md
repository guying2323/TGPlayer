# 用 compose-skill / awesome-android-agent-skills 审计报告

审计版本：0.19.0（versionCode 26）
审计日期：2026-10-23

## 安装的技能

| 技能 | 来源 | commit | 安装位置 |
|---|---|---|---|
| compose-expert | github.com/aldefy/compose-skill | 954ef54 | ~/.dsh-beta/skills/compose-expert |
| compose-performance-audit | github.com/new-silvermoon/awesome-android-agent-skills | 82900ea | ~/.dsh-beta/skills/compose-performance-audit |
| compose-ui | 同上 | 82900ea | ~/.dsh-beta/skills/compose-ui |
| android-architecture | 同上 | 82900ea | ~/.dsh-beta/skills/android-architecture |
| android-viewmodel | 同上 | 82900ea | ~/.dsh-beta/skills/android-viewmodel |
| android-accessibility | 同上 | 82900ea | ~/.dsh-beta/skills/android-accessibility |
| android-testing | 同上 | 82900ea | ~/.dsh-beta/skills/android-testing |

审计方法：按 compose-performance-audit 的 Code-First Review 逐屏检查；按 compose-ui 的 state hoisting / modifier / 性能规则核对；按 android-architecture 分层规则检查依赖方向；按 android-accessibility 检查语义与触摸目标。

## 已修复（本轮）

| # | 问题 | 依据 | 修复 |
|---|---|---|---|
| 1 | Thumbnails.load 在调用线程做磁盘读 + Bitmap 解码（实际在主线程），列表滚动会掉帧 | performance: heavy work off composition | 整体包进 withContext(Dispatchers.IO)；头像复用同一缓存，避免重复下载 |
| 2 | rememberLoadable 把 CancellationException 吞成 Failed，切换页面会闪错误 | side-effects / state 规则 | 单独 catch 并重新抛出 CancellationException |
| 3 | LazyColumn / LazyGrid 未设置 contentType，类型混排时复用率低 | lists-scrolling | 聊天 / 视频 / 下载 / 搜索结果加 contentType |
| 4 | 模型含 List 字段（MediaItem.tags）不稳定，列表项可能全量重组 | performance: @Immutable | 对 ChatSummary / MediaItem / RecentPlay / Favorites.Chat、Video 加 @Immutable |
| 5 | 每张卡的收藏状态用 any {} 线性查找，O(n×m) | performance: avoid work in composition | 用 remember(favorites) 转 Set 后 O(1) 查询 |
| 6 | 卡片下载 / 收藏、播放器控制按钮触摸区域小于 48dp | accessibility: touch target | minimumInteractiveComponentSize() / heightIn(48.dp) |
| 7 | 播放器每 500ms 无条件写 snapshot state，触发整屏重组 | performance: limit state writes | 仅当 fresh != snapshot 才赋值 |
| 8 | 聊天头像单独 LruCache + 主线程解码，且无磁盘缓存 | performance / 大图 | 统一走 Thumbnails（字节上限 + 磁盘缓存 + IO） |

## P1 处理进度（0.19.0）

| P1 项 | 状态 | 说明 |
|---|---|---|
| ViewModel / Repository / UDF | 部分完成 | 媒体页（Chats/Videos）已改为 ViewModel + MediaRepository + UiState；Home/Search 仍直接调用 Td |
| DI | 部分完成 | 采用手动 AppContainer + ViewModel Factory（便于测试注入）；未引入 Hilt |
| R8 / minify | 已完成 | release 开启 minify + shrinkResources，release APK 46MB → 32MB，构建通过 |
| 模块化 | 未完成 | 仍是单模块；建议拆 :core:data / :core:ui / :feature:* |
| Baseline Profile / macrobenchmark | 未完成 | 需要 macrobenchmark 模块与真机 trace |

## 仍未解决（按技能建议，列为下一轮）

| 优先级 | 问题 | 技能建议 |
|---|---|---|
| P1 | UI 直接调用 Td 单例，无 ViewModel / UseCase，状态与数据层耦合 | android-architecture + android-viewmodel：screen-scoped ViewModel、StateFlow、Repository |
| P1 | 单模块，未拆 :core:data / :core:ui / :feature:* | android-architecture 的 modularization |
| P1 | 无 baseline profile / macrobenchmark（R8 已开） | compose-performance-audit 的 profiling 流程 |
| P2 | 视频库一次取 200 条，无 Paging 3 | compose-expert paging 参考 |
| P2 | 无 Compose UI / instrumentation 测试；JVM 单测 15 个（含 ViewModel 状态测试） | android-testing 的测试金字塔 |
| P2 | 播放器自绘控制层缺少显式 semantics/role，TalkBack 未走查 | android-accessibility |
| P2 | 偏好写盘仍是 SharedPreferences + JSON，主线程小写入 | 可迁 DataStore（非紧急） |

## 结论

- Compose 结构、modifier 顺序、状态提升整体符合 compose-expert / compose-ui 的规范；本轮把最影响滚动和重组的 8 个点修掉，构建、单测、Lint 全部通过。
- 真正拉低上限的是架构层：没有 ViewModel / Repository / DI，UI 直接依赖 TDLib 单例。这是下一轮最值得做的重构，不应继续堆功能。
- 建议顺序：ViewModel / Repository 抽取 → R8 + baseline profile → Paging → Compose UI 测试。
