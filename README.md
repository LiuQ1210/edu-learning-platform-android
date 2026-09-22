# 智能广告基座学习平台 - Android 客户端 README

# 一、项目简介

本项目为课程小组协作开发的**智能广告基座学习平台** Android 客户端，基于 **Kotlin 2.2.10 + Jetpack Compose** 全新搭建的纯净企业级客户端骨架。

项目采用前后端分离架构，与后端（SpringBoot 3.2.5）统一接口规范、统一返回格式（Result）、统一鉴权体系（JWT Token），作为多端体系中的 Android 移动端入口，完全按照企业开发规范搭建，为后续小组业务开发提供稳定、规范的客户端基座。

# 二、技术栈明细

- **核心框架**：Kotlin 2.2.10 + Jetpack Compose（BOM 2026.02.01，全新声明式 UI 生态）
- **UI 组件库**：Material 3（Compose 官方设计体系）
- **构建工具**：AGP 9.3.0 + Gradle 9.5.0（项目自带 Wrapper，无需本地安装 Gradle）
- **基础组件**：Activity Compose 1.8.0、Core KTX 1.10.1、Lifecycle Runtime KTX 2.6.1
- **单元测试**：JUnit 4.13.2
- **仪器测试**：Espresso Core 3.5.1、AndroidX Test JUnit 1.1.5
- **代码风格**：Kotlin Official 官方风格（gradle.properties 已配置）

> 说明：网络层（Retrofit/OkHttp）、图片加载、JSON 序列化等业务依赖**暂未引入**，保持骨架纯净，后续按业务需要统一在 `libs.versions.toml` 中声明添加。

# 三、环境要求

- JDK 版本：17+
- Android Studio：支持 AGP 9.x 的最新稳定版本
- Android SDK：SDK Platform 37（compileSdk），运行设备 API 24（Android 7.0）及以上
- Gradle：无需本地安装（项目自带 Wrapper 9.5.0，依赖走腾讯镜像加速）
- 运行设备：模拟器（AVD）或真机（开启 USB 调试）

# 四、项目骨架结构说明

```Plain Text
com.github.learningplatform
├── MainActivity.kt    // 应用唯一入口 Activity（Compose 承载，骨架核心）
├── ui/theme           // Compose 主题体系（骨架核心）
│   ├── Color.kt       // 品牌颜色集中定义
│   ├── Theme.kt       // 亮/暗主题切换
│   └── Type.kt        // 字体排版规范
└── res                // 资源目录
    ├── drawable       // 矢量图标（启动图标前后景）
    ├── mipmap-*       // 应用图标（多密度适配）
    ├── values         // strings / colors / themes
    └── xml            // 备份与数据提取规则
```

# 五、已搭建的底层公共组件【详细作用说明】

## 1. Compose 主题体系（ui/theme）

**作用：统一应用视觉风格，杜绝页面风格混乱。**

- Color.kt：集中管理品牌色，后续业务色统一在此定义
- Theme.kt：支持亮/暗主题切换，全局统一应用
- Type.kt：定义字体排版规范
- 所有页面统一通过 `EdulearningplatformandroidTheme` 包裹

## 2. 应用入口与页面骨架（MainActivity.kt）

**作用：应用唯一启动入口，提供基础页面框架。**

- ComponentActivity + setContent 声明式入口
- Scaffold 基础页面框架（顶部栏/内容区/底部栏扩展位）
- 后续在此扩展 Navigation 路由，实现页面跳转

## 3. 版本目录构建体系（gradle/libs.versions.toml）

**作用：统一管理所有依赖版本，杜绝版本冲突。**

- AGP、Kotlin、Compose BOM 等版本全部集中声明
- 后续新增依赖只需在 toml 中添加，模块内引用别名即可

## 4. 测试基线（test / androidTest）

**作用：提供单元测试与仪器测试的完整运行基线。**

- ExampleUnitTest：JVM 单元测试示例（./gradlew test）
- ExampleInstrumentedTest：设备仪器测试示例（./gradlew connectedAndroidTest）

## 5. 待搭建组件规划【与后端一一对应】

| 后端组件 | Android 端对应规划 | 状态 |
| --- | --- | --- |
| Result 统一返回 | 统一响应模型 `Result<T>`（解析 code / msg / data） | 待搭建 |
| JwtUtil 令牌工具 | TokenManager + OkHttp 请求拦截器（登录态保存、自动注入请求头、401 统一处理） | 待搭建 |
| GlobalExceptionHandler | 全局异常捕获（协程异常、网络异常统一转友好提示） | 待搭建 |
| WebConfig 跨域配置 | 移动端无跨域问题，仅需统一配置 BaseUrl | 待搭建 |
| Knife4j 接口文档 | 直接对接后端接口文档（无需客户端搭建） | 由后端提供 |

# 六、配置文件说明

## app/build.gradle.kts

- namespace / applicationId：com.github.learningplatform
- compileSdk 37 / minSdk 24 / targetSdk 37
- Compose 构建特性已开启
- 所有依赖通过 `libs.versions.toml` 版本目录引用

## gradle/libs.versions.toml

- 统一依赖版本管理（AGP、Kotlin、Compose BOM 等）

## gradle.properties

- 构建参数：JVM 内存 -Xmx2048m、UTF-8 编码
- Gradle Configuration Cache 已开启（增量构建加速）
- Kotlin 代码风格：official

## gradle/wrapper

- Gradle 9.5.0，distributionUrl 指向腾讯镜像（国内下载加速）

## local.properties

- 本机 SDK 路径配置，**不提交仓库**，每人本地自动生成

## .gitignore

- 已配置完整忽略规则：.idea、.gradle、build、local.properties 等，保证提交代码干净规范

# 七、Git 分支协作规范

与后端仓库保持一致：

- **main**：最终上线稳定分支，仅合并，不直接开发
- **develop**：公共开发分支，所有功能合并到此分支测试
- **feature/xxx**：个人功能开发分支，每人独立分支开发，避免代码冲突

# 八、项目启动方式

1. 使用 Android Studio 打开项目根目录，等待 Gradle 同步完成（首次会下载依赖，走腾讯镜像较快）
2. 确保本机 JDK 17+、Android SDK Platform 37 已安装
3. 启动模拟器（API 24+）或连接真机（开启 USB 调试）
4. 点击 Run 运行，或命令行构建：`./gradlew assembleDebug`
5. 运行测试：
   - 单元测试：`./gradlew test`
   - 仪器测试：`./gradlew connectedAndroidTest`（需连接设备）
6. 前后端联调说明：
   - 模拟器访问本机后端：`http://10.0.2.2:8080`
   - 真机访问本机后端：局域网 IP（如 `http://192.168.x.x:8080`）
   - 后端接口文档：`http://localhost:8080/doc.html`
