# RuLearner · 俄语学习器

一个跨平台的俄语学习应用：**Android**、**iOS**、**Windows / macOS 桌面端** 共用同一套学习数据与 SRS 复习算法。
纯本地优先（不联网也能学），支持自建词库 / 教材 + 音频包、间隔重复复习、拼写与测验、随身听、描红练习。

> 应用显示名 **RuLearner** · 当前版本 **1.0.2**（versionCode 11）

---

## 功能特性

### 📱 Android（Kotlin + Jetpack Compose）
| 模块 | 说明 |
|---|---|
| 首页 | 今日学习进度环、总词条/已掌握/连续打卡/今日已学、每日一词 |
| 单词 | 多词书切换、搜索、词条详情（掌握度 / 发音 / 在线词典） |
| 练习 | 今日复习、中→俄拼写（内置 ЙЦУКЕН 俄文键盘）、四选一测验 |
| 字母 | 33 个西里尔字母（手写体字形）、字母测验、**描红练习 + 笔顺演示动画** |
| 随身听 | 后台播放教材音频、顺序/随机、整课音轨时间轴定位、**音频同步词（双语逐句）** |
| 导入 | 词库（JSON/TSV/CSV）、课程 JSON、**zip 音频书**、按 URL 下载 |
| 设置 | 每日目标、有道智云密钥、发音、**导出/导入学习进度**、检查更新、查看公告 |
| 小组件 | 桌面「今日学习」「每日一词」两个小组件 |


### 🖥️ 桌面端（Compose for Desktop）开发中。。
macOS / Windows 原生窗口应用，共享同一套 `shared` 数据层。

---

## 目录结构

```
.
├── app/         Android 端（Compose，7314 行）
├── shared/      跨端共享核心：数据模型 / 词库 / 教材 / SRS / 测验逻辑（1236 行）
├── desktop/     Compose for Desktop 端（1075 行）
├── ios/         iOS 端（SwiftUI，27 个 Swift 文件，3285 行）
├── docs/        公告与版本配置（config.json + index.html）
├── tools/       词库构建、音频包、桌面同步、进度备份等脚本
├── index.html   静态分发页 + 意见反馈表单（GitHub Pages）
└── russian-cheatsheet.html  俄语速查表
```

---

## 快速开始

### Android
```bash
./gradlew :app:assembleRelease     # 发布版 APK
./gradlew :app:assembleDebug       # 调试版
./gradlew :app:lintDebug           # 静态检查
```

产物：`app/build/outputs/apk/release/app-release.apk`

**签名**：在 `app/build.gradle.kts` 的 `signingConfigs.release` 配置 keystore，
或通过环境变量 `RU_KEYSTORE` / `RU_KEYSTORE_PASSWORD` / `RU_KEY_ALIAS` / `RU_KEY_PASSWORD` 注入；
未配置时自动回退 debug 签名，方便开源协作者直接构建。

> ⚠️ 若构建报 `Failed file name validation ... "xxx 2.xml"` 之类的错误，
> 说明 `app/build` 里有旧文件残留，执行 `./gradlew clean` 后重新构建即可。

### iOS
```bash
open ios/RuLearner.xcodeproj      # Xcode 打开，选模拟器 ⌘R
```

打 IPA（真机 Release）：
```bash
xcodebuild -project ios/RuLearner.xcodeproj -scheme RuLearner \
  -sdk iphoneos -configuration Release -derivedDataPath /tmp/rl_dd \
  CODE_SIGNING_ALLOWED=NO build
# 产物在 /tmp/rl_dd/Build/Products/Release-iphoneos/RuLearner.app
# 再打包成 ipa：把 .app 放进 Payload/ 后 zip 即可
```

未签名的 IPA 需用 **Sideloadly / AltStore** 走自己的 Apple ID 签名后安装。

### 桌面端
```bash
./gradlew :desktop:packageDistributionForCurrentOS
```
产物在 `desktop/build/compose/binaries/main/`。

---

## 公告与版本更新（可在 GitHub 上自行配置）

App 的**公告**与**最新版本号**托管在仓库里，改完提交即可，App 自动拉取：

- 配置文件：`docs/config.json`
- 展示页：`docs/index.html`（可开 GitHub Pages 获得一个可访问的网址）
- App 拉取地址：jsDelivr CDN（`cdn.jsdelivr.net/gh/<owner>/<repo>@main/docs/config.json`），国内可访问

```jsonc
{
  "latestVersionCode": 11,          // 大于 App 内的 versionCode 才会提示更新
  "latestVersionName": "1.0.2",
  "apkUrl": "https://github.com/.../app-release.apk",
  "announcements": [
    {
      "id": "唯一ID",
      "title": "公告标题",
      "date": "2026-10-06",
      "level": "info | warn | danger | ok",
      "body": "公告正文，支持换行"
    }
  ]
}
```

用户可在「设置 → 备份与更新」里点**检查更新** / **查看公告**；新版本启动时也会自动提示。
APK 下载地址指向 GitHub Release（`.github/workflows/release.yml` 在打 `v*` 标签时自动发版）。

---

## 学习进度备份

「设置 → 备份与更新 → 导出学习进度」会把**掌握度 + 全部设置/统计**打包成一个 JSON：

```jsonc
{
  "app": "RuLearner",
  "schema": 1,
  "exportedAt": "2026-10-06T15:00:00",
  "versionCode": 11,
  "versionName": "1.0.2",
  "progress": { "вода": { "lvl": 3, "due": 1760000000000, "seen": 5, "ok": 4, "bad": 1 } },
  "prefs": { "daily_goal": "20", "streak": "7", "...": "..." }
}
```

拿到文件后，一条命令同步到桌面并上传 GitHub：

```bash
bash tools/backup_progress.sh /path/to/rulearn-progress-20261006-150000.json
```

脚本会把文件复制到 `~/Desktop/RuLearn进度备份/`，并提交推送到仓库的 `progress-backups/` 目录。

> ⚠️ 当前仓库是**公开**仓库，学习数据会公开可见。若需隐私，把脚本里的 `BRANCH` / `REMOTE`
> 改成一个**私有**仓库或分支。

导入进度：App 内「设置 → 导入学习进度」选该 JSON 即可恢复（Android / iOS 通用）。

---

## 资源格式规范

### 词库（JSON 数组）
```json
[ { "ru": "вода", "zh": "水", "audio": "", "img": "", "note": "" } ]
```
也兼容 `ru/word/text`、`zh/mean/meaning/trans` 等别名，以及 `{"words":[...]}` 包装。
纯文本用 **TSV / CSV / 分号**分隔两列（`俄语<TAB>中文`），首行 `ru` 会被当表头跳过。

### 课程 / 教材 JSON
```jsonc
{
  "lessons": [
    {
      "n": 1,
      "title": "урок 1",
      "audio": "audio/full.mp3",      // 整课音轨（可选）
      "segments": [
        { "i": 1, "ru": "Здравствуйте", "zh": "你好",
          "start": 0.0, "end": 2.5,     // 用整课音轨时按时间戳定位
          "audio": "audio/s1.mp3" }    // 或单独给本段音频
      ]
    }
  ]
}
```

### zip 音频书（推荐）
把清单和音频打成一个 zip：包内放 `book.json`（或 `lessons.json` / `manifest.json` / `index.json`）
+ `audio/` 目录，清单里写**相对路径**。App 解压后自动改写成本机绝对路径，并按课号合并。

---

## 常用脚本

| 脚本 | 作用 |
|---|---|
| `tools/copy_apk_to_desktop.sh` | 把最新 APK 以版本号命名同步到桌面（`RuLearner-<版本>.apk`），顺带同步 `vocab.json` |
| `tools/backup_progress.sh` | 导出的学习进度 → 复制到桌面 + 推送 GitHub |
| `tools/build_vocab.py` | 构建/扩充词库 |
| `tools/expand_vocab.py` | 词库扩充 |
| `tools/build_textbook_zip.py` | 把教材 + 音频打成 zip 音频书 |
| `tools/transcribe_whisper.py` | Whisper 语音转写生成课文文本 |
| `tools/download_letter_audio.py` | 下载字母 / 例词发音音频 |

---
## 开源许可

[Apache License 2.0](LICENSE) · 版权声明见 [NOTICE](NOTICE)。
