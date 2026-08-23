# FloatHearing（Android）

FloatHearing（FH Reborn）是一款基于 MediaStore 的本地音乐播放器，使用 Jetpack Compose 与 [Miuix](https://github.com/compose-miuix-ui/miuix) 构建，界面遵循 MIUI 风格设计语言。

## 功能特性

- **媒体库**：歌曲 / 专辑 / 艺术家 / 文件夹四个视图，支持搜索、排序、隐藏文件夹
- **歌单**：创建、编辑（封面、标题、描述）、排序，支持自定义排序
- **播放器**：播放队列、逐字歌词、定时停止、计划暂停、播放模式切换
- **听歌统计**：记录播放历史，按日 / 周 / 月展示总时长、次数与常听专辑 / 艺术家
- **批量操作**：多选后批量加入歌单、加入播放队列、分享、删除
- **文件夹浏览**：媒体库文件夹 tab 直达二级页面，浏览路径支持路径面包屑导航
- **个性化**：自定义壁纸（图片背景 + 前景色，跟随深浅色模式）、动态取色
- **大屏适配**：宽屏下自动切换侧边栏导航与响应式卡片网格

## 系统要求

- **Android 12（API 31）及以上**
- 仅支持 Android 平台

## 技术栈

| 组件 | 说明 |
| --- | --- |
| Kotlin / Jetpack Compose | UI 与逻辑 |
| [Miuix](https://github.com/compose-miuix-ui/miuix) 0.9.4-rc01 | MIUI 风格组件库 |
| Room | 歌单、播放统计等本地数据 |
| MediaStore | 音乐库扫描与封面读取 |

## 构建说明

### 环境要求

- JDK 21+
- Android SDK（compileSdk 36）
- Gradle 9.4.1（或直接使用项目自带的 Gradle Wrapper）

### 编译 Debug APK

```bash
./gradlew assembleDebug
```

构建产物位于 `app/build/outputs/apk/debug/`。

## CI / GitHub Actions

- 默认分支为 `miuix`，向该分支推送或提交 Pull Request 会自动触发 Debug 构建，并上传 APK 产物（Artifact）。
- 打 `v*` 标签会触发 Release 构建（需配置签名相关 Secrets），完成后自动创建 GitHub Release 并附带 APK。

## 相关链接

- Telegram 频道：https://t.me/breadkat_nest
- GitHub 仓库：https://github.com/BreadKat0707/FloatHearingAndroid

## 许可证

暂未指定。
