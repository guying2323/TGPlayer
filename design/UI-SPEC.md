# TMPlayer Android · 中文 UI 规范（v1 草案）

> 配套预览：`design/ui-preview.html`（浏览器直接打开，配色 A 已锁定）。
> 本文是实现的唯一视觉依据；Compose 中的颜色、字号、间距必须取自这里。

## 1. 设计方向

- **深色影院感**：大面积近黑底 + 单卡片层级，封面承担主要色彩，界面本身保持安静。
- **内容优先**：一屏一个主任务；首页先“继续观看”，聊天里先视频网格，播放页只留视频与控制。
- **电视与手机同源**：同一套组件，电视只放大字号、加强焦点、加上下安全边距。
- **配色已定**：A 深海影院（青 + 金）。方案 B（霓虹胶片）未采用，已从预览与规范中移除。

## 2. 设计令牌

### 配色（方案 A · 深海影院）

| Token | 值 | 用途 |
|---|---|---|
| bg | `#0B0D10` | 页面底 |
| surface | `#12151A` | 卡片/输入框 |
| surfaceHigh | `#1A1F26` | 弹层/选中底色 |
| line | `#252C35` | 分割线/描边 |
| text | `#E8EAED` | 正文 |
| textMuted | `#98A2AE` | 次要信息 |
| accent | `#4FD1C5` | 主操作/进度/焦点 |
| onAccent | `#052A27` | 主色上的文字 |
| gold | `#E5B650` | 提醒/评分等强调 |
| danger | `#E9A6A6` | 退出登录等破坏性操作 |

> 方案 B（霓虹胶片：`#0A0A12` / `#FF5A7A` / `#7C5CFF`）未采用，仅保留在 git 历史中。

### 字体与字号

系统字体栈：PingFang SC / Microsoft YaHei / Noto Sans SC；数字与英文用同族，跟随系统。

| Token | 手机 | 电视（×1.25） | 用途 |
|---|---|---|---|
| display | 34sp / 700 | 42sp | 页面主标题、播放器标题 |
| title | 22sp / 700 | 27sp | 区块标题、海报标题 |
| subtitle | 16sp / 600 | 20sp | 卡片标题、列表主行 |
| body | 15sp / 400 | 19sp | 正文 |
| label | 13sp / 500 | 16sp | 按钮、标签、时间 |
| caption | 11sp / 400 | 14sp | 角标、说明 |

### 间距 / 圆角 / 层级

- 间距阶梯：4 / 8 / 12 / 16 / 24 / 32 / 48 dp；页面左右边距 20dp（电视 34dp）。
- 圆角：12dp（小卡、输入框）/ 16dp（海报、面板）/ 24dp（大卡、弹层）/ 999dp（胶囊）。
- 层级：bg → surface → surfaceHigh；不使用重阴影，电视用可见焦点环。
- 焦点环：2dp 主色描边 + 外扩 3dp；配 1.04 缩放（仅电视/键盘）。
- 动效：见第 7 节（120 / 180 / 240 / 320ms + 弹性回弹 + 减弱动效开关）。

### 栅格

- 手机：3 列海报网格，最小列宽 104dp（390dp 宽下稳定 3 列）；卡片间距 8dp。
- 平板：左导航 88dp + 内容区；视频库最小列宽 140dp。
- 电视：安全区左右 48dp、上下 36dp；海报卡 220dp；行间距 20dp。

## 3. 断点与导航

| 宽度 | 导航 | 布局 |
|---|---|---|
| < 600dp | 底部导航（首页 / 聊天 / 搜索 / 设置） | 单栏 |
| 600–1023dp | 左侧导航栏（图标+文字） | 列表-详情双栏（聊天 + 视频库） |
| ≥ 1024dp | 左侧导航或顶部导航（电视，含搜索） | 首页 2 栏 / 视频库 4–6 列 |
| 电视（leanback） | 顶部导航 | 大字号、焦点优先、无滚动条 |

电视判定：`UiModeManager` 为 TV 或 `PackageManager.FEATURE_LEANBACK`；不锁方向、不加 `configChanges`。

## 4. 组件

| 组件 | 规格 |
|---|---|
| 主按钮 | 高 44dp、圆角 999、主色底、onAccent 文字、左右 18dp；按下 0.98 缩放 |
| 次按钮 | 同尺寸、1dp 描边、透明底 |
| 图标按钮 | 36dp 方、12dp 圆角、1dp 描边 |
| 海报卡 | 2:3 封面 + 圆角 10；右下 9sp 时长角标；底部渐变压暗；11sp 标题 2 行；9sp meta 行 `分辨率 · 大小 · 年份`；已看过的加 3dp 续播进度条；卡片上不显示标签；焦点时描边 |
| 会话行 | 48dp 头像（圆角 15）+ 标题 + 副标题 + 时间；高 72dp |
| 头像 | 48dp、圆角 15；优先 `chat.photo.small` 的 Telegram 缩略图（`object-fit: cover`）；无头像才回退首字母 + 稳定色；加载中显示骨架。内存 LRU + 磁盘缓存，滚动不闪烁 |
| 搜索框 | 高 48dp、圆角 12、surface 底、leading 图标、占位“搜索聊天” |
| 分段芯片 | 32dp 高、胶囊、选中主色底 |
| 标签筛选 | 横向 FilterChip：「全部」+ 各 `#标签`，按出现次数排序，单选、再点取消；无标签时整行隐藏 |
| 进度条 | 4dp 高；已播放主色，已缓存白色 28% 叠加；电视 5dp |
| 播放控制 | 播放/暂停、倍速、字幕、音轨、锁定、下一集；图标 21dp + 10sp 文字；选中主色 |
| 骨架 | 12dp 高圆角条，`shimmer` 1.6s 循环 |
| 空态 | 52dp 虚线圆 + 一句说明 + 一句引导 |
| 提示条 | 圆角 12、虚线边、7% 底色；用于“未配置凭据”“网络断开” |

## 5. 界面规格

### 01 登录
- 结构：Logo → 二维码卡（236dp 白底，圆角 26）→ 说明 → “或”分隔线 → “手机号 / 验证码登录”主按钮 → 说明“手机号登录同为一级入口” → 凭据提示条 → 底部版权。
- 入口权重：扫码与手机号是同级入口，手机号不折叠进菜单、不降级为文字链接。
- 状态：连接中（二维码位置显示进度）、二维码过期（覆盖“点击刷新”）、手机号输入、验证码（N 位）、两步验证、失败（错误文案 + 重试）。
- 文案：`用手机 Telegram 扫描二维码登录` / `使用手机号登录` / `验证码已发送至 +86 …` / `两步验证` / `网络已断开，登录需要网络`。

### 02 首页
- 结构：顶部 Logo + 搜索/设置 → 继续观看大卡（封面 + 标题 + 进度 + 播放按钮）→ 继续观看横向轨 → 最近更新 2 列。
- 空态：无观看记录时隐藏大卡，显示“从聊天里挑一个视频开始”。
- 数据：最近更新的 3 个聊天；卡片角标显示时长；进度条显示已看比例。

### 03 聊天
- 结构：标题“聊天” + 搜索 → 芯片（全部 / 未读 / 已归档）→ 会话行列表。
- 头像：取 `chat.photo.small`（缺失时 `big`）的 file id，经缩略图加载器下载并缓存后显示；这是真实的频道 / 群组 / 私聊头像，不使用生成的字母色块。
- 状态：加载骨架 5 行、空态“还没有聊天”、搜索无结果。
- 宽屏：左列表 + 右侧视频库预览。

### 04 视频库
- 结构：返回 + 聊天标题 + 视频数 + 聊天内搜索框；自适应海报网格：**手机 3 列（最小 104dp）**、平板 140dp、电视 220dp。
- 卡片：2:3 封面（Telegram 缩略图）、10dp 圆角、右下 9sp 时长角标、11sp 标题 2 行、9sp meta 行（分辨率 / 大小 / 年份）、续播进度条；卡片上不再显示标签（标签在顶部 chips 筛选）。
- 行为：点击直接进播放器；聊天内搜索按标题即时过滤。
- 标签：从 caption 提取 `#hashtag`（去重、小写、最多 8 个）；顶部 chips 单选筛选；卡片 meta 下显示前 2 个标签。
- 数据来源（对齐原项目）：先 `loadChats` 拉取聊天页（404 表示已全部本地化）；视频不走“翻聊天记录”，而是 `searchChatMessages` 同时跑 **Video / Document / Animation** 三类 filter，按 `nextFromMessageId` 分页、按 messageId 合并去重；单条消息映射失败不影响整页。
- 空态：“这个聊天里还没有可播放的视频”。

### 05 播放器
- 结构：全屏；顶部返回 + 标题（10s 无操作淡出）；中央播放/暂停；底部进度（含已缓存段）+ 时间 + 控制行。
- 控制项固定为：播放/暂停、倍速、字幕、音轨、锁定、下一集。v1 不做清晰度切换（按默认清晰度播放）。
- 状态：加载中（中央两点呼吸）、缓冲中（显示“缓冲中…”）、已缓存标记、播放结束（下一集卡片）。
- 手势：单击显隐控制、双击播放/暂停（手机）、左右滑 10s、上下滑亮度/音量；电视方向键左右快进/快退 10s。
- 跨配置：位置与 playWhenReady 保存；切回重建不重播。

### 06 设置
- 分组：账号（已登录 / 退出登录）、播放（默认清晰度、节省流量、自动续播）、存储（缓存占用、清除缓存）、关于（版本、开源许可）。
- 作者卡（关于之后）：作者名称 / 官网 / 开源代码地址，三项留空占位，发布前填写；值为空时显示“留空占位”，不显示空行。
- 退出登录需二次确认弹层。

### 07 搜索
- 结构：标题“搜索” + 输入框 + 结果分区（频道 / 群组 / 聊天）。
- 范围：只按**名称**搜索频道、群组、私聊（TDLib `searchChats`），不搜消息内容。
- 行为：输入后 300ms 防抖；走 TDLib `searchChats` 按名称匹配；结果按 `chat.type` 分到频道 / 群组 / 聊天三组；点结果进视频库。
- 失败要可见：连接状态变化会自动重载；加载失败显示错误与「重试」，设置页「诊断」显示最近一次错误。
- 空态：“没有找到结果”。

## 6. 文案表（中文为默认语言）

| Key | 中文 |
|---|---|
| app_name | TMPlayer |
| nav_home / nav_chats / nav_settings | 首页 / 聊天 / 设置 |
| signin_scan_title | 用手机 Telegram 扫描二维码登录 |
| signin_phone | 使用手机号登录 |
| signin_code_hint | 验证码已发送 |
| signin_password | 两步验证密码 |
| signin_api_missing | 未配置 API 凭据：请在 local.properties 填写 TG_API_ID / TG_API_HASH 后重新构建 |
| home_continue | 继续观看 |
| home_recent | 最近更新 |
| home_empty | 从聊天里挑一个视频开始 |
| chats_search | 搜索聊天 |
| chats_all / chats_unread / chats_archive | 全部 / 未读 / 已归档 |
| chats_empty | 还没有聊天 |
| videos_empty | 这个聊天里还没有可播放的视频 |
| player_cached | 已缓存 |
| player_buffering | 缓冲中… |
| player_next | 下一集 |
| player_speed | 倍速 |
| player_subtitles | 字幕 |
| player_audio | 音轨 |
| search_hint | 搜索聊天、视频或 #标签 |
| tags_all | 全部 |
| search_empty | 没有找到结果 |
| settings_logout | 退出登录 |
| settings_save_data | 节省流量 |
| settings_autoplay | 自动续播 |
| settings_cache | 缓存占用 |
| settings_clear_cache | 清除缓存 |
| settings_license | 开源许可 |
| settings_author_name | 作者名称 |
| settings_author_site | 官网 |
| settings_author_source | 开源代码地址 |
| settings_placeholder | 留空占位 |
| about_unofficial | 非官方客户端 · GPL-3.0-or-later |

英文放 `values-en/strings.xml`；默认 `values/strings.xml` 为中文。

## 7. 动效方案（热门节奏）

采用“快、短、有回弹”的短视频式节奏，只动 transform / opacity / 颜色，不动布局。

### 令牌

| Token | 值 | 用途 |
|---|---|---|
| durFast | 120ms | 按压、开关、选中 |
| durBase | 180ms | 淡入、图标变化、控制层显隐 |
| durSlow | 240ms | 底部弹层、转场 |
| durEmphasis | 320ms | 大卡、共享元素进入播放器 |
| easeStandard | \`cubic-bezier(.2,0,0,1)\` | 默认（快出慢收） |
| easeSpring | \`cubic-bezier(.34,1.56,.64,1)\` | 按压回弹、弹层入场 |
| stagger | 40ms | 列表错峰入场 |

### 七组动效

1. **列表错峰入场**：首屏 4 行，40ms 间隔，透明度 0→1、位移 14dp→0（durBase）；滚动加载不重复。
2. **弹性按压**：按下缩到 0.95，抬起回弹 1.02 再归位（durFast + easeSpring）；电视用焦点替身（1.04 放大）。
3. **底部弹层**：上滑 240ms easeSpring + 遮罩 180ms 淡入；返回键同向收起。
4. **骨架微光**：1.6s 线性循环，只扫过占位块，不整页闪。
5. **进度与缓存**：宽度变化 240ms easeStandard；缓冲中两点呼吸 1.2s。
6. **电视焦点呼吸**：焦点环 1.8s 呼吸 + 1.04 放大；方向键移动不重新入场。
7. **Toast**：底部上滑 18dp + 淡入，停留 2.8s 后滑出。

### 约束

- 单屏同时入场元素 ≤ 8；首帧不做长动画，播放中不做装饰性循环动画。
- 播放页控制层 3s 无操作淡出（240ms），点击/按键恢复。
- 跟随系统“移除动画”：关闭位移与缩放，仅保留 120ms 淡入。

## 8. 图标

- **来源**：react-icons **5.7.0** 的 `fi`（Feather，MIT）与 `lu`（Lucide，ISC），从生成的组件文件里抽取真实 SVG 几何；不是手绘相似形状，也不是 Material Symbols。
- **固化文件**：`design/icons.json`（含包版本、组件名、原始节点），`design/tools/extract-icons.mjs` 可重跑抽取；许可声明见 `design/icons-NOTICE.md`。
- **预览板**：把 icons.json 内联为 `<symbol>` 雪碧图，24px 网格、2.0 描边、圆角端点、`currentColor`。
- **Compose**：从同一份 icons.json 生成 `ui/theme/AppIcons.kt` 的 `ImageVector`（path / polyline / line / circle 转 PathBuilder），不引 Material Symbols，保证两端几何一致。
- **已选图标**：home / chat / settings / search / play / pause / back / cast / lock / next / gauge / captions / volume / offline / download / refresh / more / clock / user / folder / sliders / star / check / close / archive / tv / language / pip。
- **无障碍**：承担语义的图标必须给 `contentDescription`；纯装饰图标传 null。

## 9. 可访问性

- 所有可点击区域 ≥ 48dp；电视焦点环对比度 ≥ 3:1。
- 文字与背景对比：正文 ≥ 4.5:1，次要文字 ≥ 3:1（已按令牌核算）。
- 支持系统字体缩放（`sp`，不用 `dp` 写文字）；大字号时卡片标题最多 2 行省略。
- 播放器控件提供内容描述（contentDescription），遥控可全程操作。
- 关闭动画（系统“移除动画”）时取消淡入与缩放。

## 10. 与 Compose 的映射

| 设计 | Compose |
|---|---|
| 令牌 | `ui/theme/Tokens.kt`（Color/Type/Spacing 对象） |
| 主色/深色 | `ui/theme/TmTheme.kt`（Material3 colorScheme，仅深色） |
| 海报卡 | `ui/common/PosterCard.kt`（`focusable` + `onFocusChanged`） |
| 进度条 | `ui/common/CachedProgress.kt`（Canvas 绘制两层） |
| 底部/侧边导航 | `ui/nav/AdaptiveNav.kt`（按宽度选择） |
| 网格 | `LazyVerticalGrid(GridCells.Adaptive(168.dp))` |
| 骨架 | `ui/common/Skeleton.kt`（infiniteTransition） |
| 播放控制 | `player/PlayerOverlay.kt` |
| 搜索 | `ui/SearchScreen.kt` + `Td.searchChats/searchVideos` |
| 视频卡 | `ui/VideosScreen.kt`（2:3 海报 + meta + 续播条） |
| 聊天头像 | `ui/common/ChatAvatar.kt` + `data/ThumbnailStore.kt`（TDLib file id → Bitmap，内存 LRU + 磁盘缓存） |
| 图标 | `ui/theme/AppIcons.kt`（由 `design/icons.json` 生成 ImageVector） |
| 动效令牌 | `ui/theme/Motion.kt`（时长 / 曲线 / 错峰） |
| 入场动画 | `ui/common/StaggerIn.kt`（AnimatedVisibility + delay） |

## 11. 构建与凭据

- Telegram \`api_id\` / \`api_hash\` 只写在 \`local.properties\`（已在 .gitignore），构建时注入 \`BuildConfig.TG_API_ID\` / \`TG_API_HASH\`，不写入源码、不提交。
- 凭据缺失时登录页显示提示条并禁用登录按钮，不崩溃。
- 这是应用级 API 凭据，不是账号密码；登录态由 TDLib 保存在本机数据库。

## 12. 不在本版

Cast 按钮、下载管理页、在线字幕、TV 频道、统计/评分、动态壁纸、多语言（先中英）。
