# FloatHearing（Android）

FloatHearing 是一款面向 Android 的本地音乐播放器，目前处于非常早期的开发阶段。

## 系统要求

- **Android 12（API 31）及以上**
- 仅支持 Android 平台

## 构建说明

### 1. 克隆本项目

```bash
git clone <本项目仓库地址>
cd FloatHearing
```

### 2. 获取 UI 库

本项目依赖 CloverUI 组件库，**该库尚未发布到 Maven 仓库**，因此需要手动克隆并发布到 Maven Local：

```bash
git clone --depth 1 https://github.com/BreadKat0707/CloverUIforAndroid.git ../CloverUI
cd ../CloverUI
chmod +x gradlew
./gradlew :clover-ui:publishReleasePublicationToMavenLocal
```

完成后回到 FloatHearing 目录即可正常编译。

### 3. 编译 Debug APK

```bash
./gradlew assembleDebug
```

构建产物位于 `app/build/outputs/apk/debug/`。

## CI / GitHub Actions

本仓库已配置 GitHub Actions Workflow：

- 触发条件：向 `main`、`master` 或 `develop` 分支推送代码，以及针对这些分支的 Pull Request。
- CI 会自动克隆并发布 CloverUI 到 Maven Local。
- 目前 **仅配置 Debug 包构建**，构建完成后会上传 Debug APK 作为 Artifact。

## 项目状态与反馈

> ⚠️ **本项目处于很早期的开发阶段，代码结构、功能实现和 UI 都可能发生较大变动。**
>
> 在发布第一个正式 Release 之前，**暂不接收反馈和 Issue**。如果你有兴趣跟进，可以在正式版发布后再提交建议或问题。

## 许可证

待定 / 稍后补充。
