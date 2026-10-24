# TMPlayer Android 开发说明与下一步计划

> 版本：0.19.0 · 更新：2026-10-23
> 状态：**可编译、可安装的骨架版**；登录/聊天列表可用，视频列表仍有缺陷待修（见 §4）。

---

## 1. 项目是什么

以 [TMPlayer](https://github.com/dracu-lah/TMPlayer) 的核心思路重建的**单模块纯 Android 客户端**：

- 用 TDLib 登录用户自己的 Telegram 账号（扫码或手机号）；
- 浏览聊天里的视频，边下边播（TDLib 分片窗口 + Media3）；
- 中文界面，配色取设计板「深海影院」；
- 目标是手机 + 电视都能用，v1 先保证手机。

上游是 Kotlin Multiplatform（Android / JVM / Wasm 五模块）；本项目**只做 Android 单模块**，复用它的数据获取与流式播放思路，不搬 KMP 结构。

## 2. 技术栈

| 项 | 选型 |
|---|---|
| 语言 / 构建 | Kotlin 2.4.21、AGP 9.4.1、Gradle 9.6.1、JDK 21 |
| Android | compileSdk 37、minSdk 26、targetSdk 35 |
| UI | Jetpack Compose + Material 3（Compose BOM 2026.09.00） |
| 播放 | Media3 ExoPlayer 1.11.1 + nextlib-media3ext 1.11.1-0.16.0（NextPlayer 解码核心）+ 自定义 DataSource |
| Telegram | tdl-coroutines 15.0.0（内含各 ABI 的 libtdjson） |
| 二维码 | ZXing core 3.5.4 |
| 图标 | react-icons 5.7.0（Feather/Lucide）抽取，生成 Compose ImageVector |
| ABI | 仅 arm64-v8a |

## 3. 架构

```
UI (Compose)                     播放 (Media3)
  AppRoot / 各 Screen              PlayerActivity / PlayerScreen
        │                                │
        ▼                                ▼
  Td (单例)  ◄────── 下载/文件 ──── TdByteWindow ── TdDataSource(media3)
        │
        ▼
    TDLib (tdl-coroutines, 原生 libtdjson)
        │
        ▼
    Telegram 服务器
```

- **单模块、无 DI 框架**：进程单例 + 显式初始化（`TmApp` 里 `Settings.install` / `Td.install`），与上游一致。
- **Td** 负责：客户端生命周期、授权状态机、聊天列表、按类型搜索视频、文件下载/读取、缩略图字节。
- **player**：`TdByteWindow` 把“播放器要的字节位置”翻译成 TDLib `downloadFile(offset)`，并在本地 partial 文件上随机读；`TdDataSource` 把窗口包成 Media3 DataSource。
- **ui**：`AppRoot` 用宽度 + TV 判定切换底部导航 / 侧边导航；各屏幕直接调 `Td` 的 suspend 函数。

### 关键文件

| 文件 | 作用 |
|---|---|
| `data/Td.kt` | TDLib 单例、登录状态机、聊天表、视频检索、文件原语、诊断 |
| `data/MediaMapper.kt` | Message → MediaItem（视频/文档/动图/圆形视频），提取 #标签 |
| `data/Models.kt` | AuthState、ChatSummary(+kind)、MediaItem、TdFile |
| `player/TdByteWindow.kt` | 流式字节窗口（seek 重新定向下载） |
| `player/TdDataSource.kt` | Media3 DataSource（`tdfile://<fileId>`） |
| `player/NextPlayerCore.kt` | NextPlayer 播放核心（NextRenderersFactory + ExoPlayer 配置） |
| `player/PlayerActivity.kt` / `PlayerScreen.kt` | 全屏播放、手势与控制层 |
| `ui/SignInScreen.kt` | 扫码 / 手机号 / 验证码 / 两步验证 |
| `ui/ChatsScreen.kt` | 聊天列表（真实头像、重试） |
| `ui/VideosScreen.kt` | 视频海报网格（3 列）、标签筛选、聊天内搜索 |
| `ui/findroid/FindroidComponents.kt` | Findroid 卡片组件（ItemCard/ItemPoster/ProgressBar/HomeSection） |
| `ui/SearchScreen.kt` | 按名称搜频道 / 群组 / 聊天 |
| `ui/SettingsScreen.kt` | 设置、版本、诊断入口、作者卡占位 |
| `design/` | 设计板、UI 规范、图标数据与许可说明 |

## 4. 当前状态（如实）

### 已实现

- 登录：扫码、手机号、验证码、两步验证；手机号与扫码互为一级入口。
- 聊天列表：`loadChats` 拉到 404，真实聊天头像，断连重试。
- 视频检索：TDLib `searchChatMessages` 的 **Video / Document / Animation** 三 filter 并发分页，按 messageId 合并；历史扫描兜底。
- 搜索：按名称搜频道/群组/聊天，分组展示；不再搜消息内容。
- 视频网格：2:3 海报、时长、分辨率/大小/年份、续播条；手机 3 列、平板 140dp、电视 220dp。
- `#标签` 筛选：caption 提取，顶部 chips 单选。
- 播放器：核心与 UI 改编自 **NextPlayer**（`NextRenderersFactory` FFmpeg 软解回退、ExoPlayer 轨道/音频配置、顶部栏+中央播放键+自定义进度条+锁定+双击 seek+竖向音量/亮度指示）。
- 播放器 UI：按 NextPlayer 的 ControlsTopView / ControlsMiddleView / ControlsBottomView 与 SimpleSlider 逐块移植（顶部栏、中央播放键、白色滑块进度条、音轨/字幕/画面/锁定/PiP），核心仍是 NextPlayerCore。
- 轨道能力（对齐 NextPlayer 的 `Player.switchTrack` / `addAdditionalSubtitleConfiguration`）：音轨切换、内嵌字幕开关、**本地字幕加载**（.srt/.vtt/.ass，SAF 选择并持久化权限）、画面适应/填充/裁剪（`AspectRatioFrameLayout`）。
- UI：首页与视频网格复用 **Findroid** 的 ItemCard / ItemPoster / ProgressBar / HomeSection（GPL-3.0，commit `f7d090f7814c7aa6e1e19fe319fe15f4e6e01ce4`），模型适配为 MediaItem。
- 首次启动引导：4 页 Compose Pager（欢迎/登录/影视库/下载与省流），参考 TMPlayer setup，完成后写偏好。
- 统一组件：`UiState` + `rememberLoadable` + `LoadingView/ErrorView/EmptyView/SkeletonList`，聊天与视频库已接入。
- 架构文档：`docs/ARCHITECTURE.md` + archify 生成的 `docs/architecture.html`。
- 性能：缩略图缓存按字节、预取 3 并发、轮询/写盘降频、低内存自适应缓冲、Debug-only analytics。
- 耗电：后台自动暂停播放、WorkManager 存储约束、下载轮询降频。
- 现代设备：predictive back、edge-to-edge、LocalWindowInfo 自适应、16 KB 页对齐（全部 .so LOAD align=0x4000 已校验）。
- 动效：页面淡入+位移切换、底部导航滑入滑出、卡片/主按钮 96% 弹簧按压、列表 `animateItem`、骨架 `shimmer`、播放器控制层滑入。
- 播放缓冲：30s/120s LoadControl，TDLib 下载优先级 32、顺序预取、复用 RandomAccessFile。
- 播放队列：视频库按当前筛选结果组成队列，播完自动下一个，顶栏可手动「下一个」。
- 画中画：PlayerActivity 支持 PiP，播放器顶栏按钮进入，PiP 内隐藏控制层。
- 媒体库：聊天列表支持频道/群组/私聊筛选与最近/名称排序（Findroid 的库筛选项）。
- 继续观看：记录真实缩略图，首页 hero/横卡显示封面而不是渐变。
- 本地播放：首页「打开本地视频（SAF）」调用系统文件选择器，走 NextPlayer 的 `DefaultMediaSourceFactory + DefaultDataSource` 管线，持久化 `content://` 读权限。
- Telegram 播放：仍走 TDLib 流式窗口（`TdByteWindow` / `TdDataSource`）。
- 设置：播放、下载、显示（主题 + 动态取色）、**多账号（切换/新增/移除）**（每账号独立 `tdlib/<id>` 目录，`Td.switchAccount` 关闭旧客户端并重启；当前用户来自 `getMe`）、诊断、关于、作者卡。
- 下载：**WorkManager 前台 Worker**（`DownloadWorker`，通知栏进度、杀进程后续传），完成后复制到应用 `Downloads/`，可离线播放；卡片下载按钮 + 下载页暂停/恢复/删除。
- 图标：自适应图标（深色底 + 青色播放键 + 金色点）与 TV banner。
- 封面：内存 + 磁盘两级缓存、720px 采样解码、列表预取（`Thumbnails`）。
- 工程：arm64-only APK（约 37 MB），源码包 zip。

### 已验证

- `assembleDebug` 编译通过（0.2.4）。
- APK 可安装、可启动；登录页在模拟器上验证过（扫码/手机号入口渲染正常）。
- APK 的 `native-code` 只有 `arm64-v8a`。

### 未验证 / 已知问题

| 问题 | 状态 | 说明 |
|---|---|---|
| **视频列表为空** | 🔴 未解决 | 聊天有视频但网格不显示；已加诊断（设置 → 运行诊断）与空页计数，等实测数据定位 |
| 搜索未实测 | 🟡 | 逻辑为名称检索，需登录后实测 |
| 播放未实测 | 🟡 | NextPlayer 核心/UI + 本地 SAF 管线已接入并编译通过；需真实视频验证本地播放、手势与 seek |
| 首页简化 | 🟡 | 只有“继续观看”入口 + 最近聊天，未做设计板里的横向海报轨 |
| 登录页切换 | 🟢 已修 | 0.2.2 修复“点手机号无反应” |
| 下载后台化 | ✅ 已做 | WorkManager 前台 Worker + 通知；仅 Wi-Fi 用 `UNMETERED` 约束 |
| 投屏 | ⏸ 放弃 | 按最新要求不再实现 |
| 多账号 | ✅ 已做 | 账号槽位 + 切换；**重复登录同一账号自动合并**（Telegram user id），添加流程中再次点按会被忽略 |
| 在线字幕 | ⏸ 放弃 | 按最新要求不再实现；保留本地字幕加载 |
| 测试 | 🟡 部分 | 11 个 JVM 单测（格式 + #标签），无 UI/仪器测试 |
| 电视 | 🟡 | 有 leanback 声明与焦点样式，未在 TV 设备验证 |

## 4.5 自检记录（0.11.0）

- `assembleDebug` 通过；`testDebugUnitTest` 11/11 通过；`lintDebug` 0 error（0.13.0 复跑）。
- 剩余 29 条 lint warning：依赖有更新版本、SharedPreferences KTX 建议、targetSdk 未跟到最新等，均非阻断。
- 未做真机验证：下载 Worker、通知、多账号切换、主题切换仍以编译/静态检查为准。

## 4.6 技能审计（0.18.0）

按 compose-performance-audit / compose-ui / android-architecture / android-viewmodel / android-accessibility 审计，修复 8 项（IO 线程、取消异常、contentType、@Immutable、收藏集合、触摸目标、snapshot 重组、头像缓存），详见 docs/SKILL-AUDIT.md。

## 4.7 P1 架构修复（0.19.0）

- 新增 MediaRepository 接口 + TdMediaRepository，Chats/Videos 改为 ViewModel（StateFlow<UiState>）+ Factory，UI 不再直接调用 Td。
- 手动 DI：AppContainer 提供 repository；ViewModel 构造注入，测试可替换 Fake。
- release 开启 R8 + shrinkResources，release APK 32MB；新增 proguard 规则（TDLib JNI / nextlib）。
- 单测 11 → 15（新增 ChatsViewModel / VideosViewModel 状态与失败路径测试）。

## 5. 下一步计划

### P0 · 修复视频列表为空（最高优先）

1. 取一次现场证据：设置 → 诊断 → 运行诊断；记录 `msgs / mapped / first` 与错误。
2. 按结果分支：
   - `msgs>0, mapped=0` → 补 `first` 类型的映射（可能是 `MessagePaidMedia`、`MessageText` 链接预览、`MessageVideoNote` 等）。
   - `msgs=0` 且有错误 → 对齐上游的会话前置条件（连接就绪、`openChat`、频道未同步等），必要时增加“打开聊天再搜索”的步骤。
   - `msgs=0` 无错误且历史兜底也空 → 检查 `chatId` 是否来自 `getChat` 的真实 id、是否 topics 论坛聊天（需要按 topic 搜索）。
3. 验收：任选 3 个有视频的聊天，网格数量 > 0 且与 Telegram 内一致；搜索能按名称返回频道/群组。

### P1 · 端到端验证（登录后）

- 手机号登录 → 验证码 → 两步验证全链路。
- 视频点击播放：首帧时间、拖动进度、远距离 seek 重新缓冲、返回后继续播放。
- 搜索：频道/群组/聊天分组正确；空结果与失败提示。
- 标签：chips 数量、单选/取消、无标签时隐藏。
- 验收标准：以上路径无崩溃，错误可读，日志无未捕获异常。

### P2 · 体验对齐设计

- 首页补齐“继续观看”横向海报轨（读续播记录）与“最近更新”网格。
- 播放器控制项补齐：字幕、音轨、锁定、下一集（当前只有播放/进度/倍速）。
- 动效落地：列表错峰入场、按压回弹、底部弹层、骨架微光、焦点呼吸（见 `design/UI-SPEC.md` §7）。
- 电视：焦点遍历顺序、横向卡片、返回键行为。

### P3 · 能力补齐

- 下载队列与离线播放（对齐上游 `DownloadService` / `OfflineDownloads`）。
- 字幕（OpenSubtitles/Subdl）与多音轨选择。
- 投屏（局域网 HLS）。
- 多账号、缓存上限、清晰度选择。

### P4 · 工程化与发布

- 单元测试：`DownloadWindow` 窗口算术、`MediaMapper` 映射、`Settings` 序列化。
- Lint / CI：`assembleDebug` + `test` + `lint` 作为门禁。
- 版本策略与 release 签名；R8/minify；崩溃上报（可选）。
- 文档：本文件持续更新，发布说明与验收清单。

## 6. 开发流程

```bash
# 构建（默认 arm64）
./gradlew :app:assembleDebug

# 其它 ABI
./gradlew :app:assembleDebug -PtmAbis=arm64-v8a,armeabi-v7a

# 安装
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

- 凭据：复制 `local.properties.example` → `local.properties`，填 `TG_API_ID/HASH`；**不得提交**。
- 每次改动升 `versionCode` 与 `versionName`，设置页可直接核对装的是哪版。
- 运行时诊断：`adb logcat -s Td`（视频计数与错误），或设置页“运行诊断”。

## 7. 风险与对策

| 风险 | 影响 | 对策 |
|---|---|---|
| TDLib 搜索行为与预期不一致 | 视频为空 | 诊断先行；保留历史扫描兜底；按 topic/类型补分支 |
| 上游 GPL-3.0 | 合规 | 本项目同为 GPL-3.0-or-later，保留版权与来源说明 |
| 只打 arm64 | 32 位设备/模拟器装不上 | 用 `-PtmAbis` 临时出其它 ABI |
| 无自动化测试 | 回归靠手工 | P4 补单测与 CI 门禁 |
| 大文件/断网 | 播放中断 | 窗口重试、错误可读、后续加重连策略 |

## 8. 附录

- 设计：`design/ui-preview.html`、`design/UI-SPEC.md`
- 图标：`design/icons.json`、`design/tools/extract-icons.mjs`、`design/icons-NOTICE.md`
- 上游：<https://github.com/dracu-lah/TMPlayer>（GPL-3.0-or-later）
- 播放器核心 / UI：<https://github.com/anilbeesetti/nextplayer>（GPL-3.0，commit e1f406f5，见 `NOTICE-NEXTPLAYER.md`）
- UI 卡片组件：<https://github.com/jarnedemeulemeester/findroid>（GPL-3.0，commit `f7d090f7814c7aa6e1e19fe319fe15f4e6e01ce4`，见 `NOTICE-FINDROID.md`）
