# 怎么查看这个 App

> 说明：`docs/` 目录曾被清进回收站并清空过一次（原始附件 v3 文档仍在，本项目文档已按修订重建）。
> 这份说明随之重建。

## 一、最快的方式：用已构建好的 APK

```
APK 路径：app\build\outputs\apk\debug\app-debug.apk
```

**装到手机**（开启「USB 调试」后连电脑）：

```powershell
adb install -r -t app\build\outputs\apk\debug\app-debug.apk
```

**装到模拟器**（先在 Android Studio 里启动一个 AVD）：

```powershell
adb devices                       # 确认设备已连接
adb install -r -t app\build\outputs\apk\debug\app-debug.apk
adb shell am start -n com.github.learningplatform/.MainActivity
```

**登录**：当前构建开了「UI 预览模式」，**用户名和密码随便填**（如 `demo` / `demo123456`）即可进入，
不需要后端。四个 Tab、列表、详情、评论、签到、笔记、待办全部有样例数据。

---

## 二、UI 预览模式

后端还没就绪时，界面必须能被打开、被点、被评审。所以加了一个**编译期开关**：

- 开关打开（默认）：所有 Repository 直接返回本地样例数据，**一个网络请求都不发**
- 开关关闭：走真实接口，`BuildConfig.BASE_URL` 指向 `https://api.learnplatform.com/api/v1/`

样例数据在 `app\src\main\java\com\github\learningplatform\data\demo\DemoData.kt`，
字段与分页结构和接口文档一致，所以滚动加载、空状态、错误提示、详情跳转都是真跑的。

短路逻辑在 `data\demo\Preview.kt`，Repository 里每个方法开头一行：

```kotlin
suspend fun getCourses(...): PageData<CourseDto> {
    preview { return DemoData.coursePage(pageNum, pageSize, videoType) }
    return safeApiCall { courseApi.getCourses(...) }
}
```

### 关掉预览模式（接真实后端）

```powershell
.\gradlew.bat :app:assembleDebug -PuiPreview=false
```

或改 `app\build.gradle.kts` 里的默认值：

```kotlin
val uiPreview = (project.findProperty("uiPreview") as String?)?.toBoolean() ?: true
//                                                                        ↑ 改成 false
```

同时把 `BASE_URL` 换成真实后端地址。**release 构建不会带样例数据**：
`DemoData.enabled` 是编译期常量，R8 会把整块数据和分支裁掉。

---

## 三、自己构建

```powershell
$env:GRADLE_USER_HOME='D:\dev-cache\gradle'   # Gradle 缓存在 D 盘
Set-Location 'D:\Android\edu-learning-platform-android-main'

.\gradlew.bat :app:assembleDebug --console=plain
```

产物：`app\build\outputs\apk\debug\app-debug.apk`

环境要求：JDK 17（已指向 Android Studio 自带 JBR）、Android SDK Platform 37。

---

## 四、已完成的页面

| Tab / 页面 | 状态 |
|---|---|
| 启动页 | `core-splashscreen` 主题，读本地登录态 |
| 登录页 | 完成（微信入口保留，抖音/QQ 按要求去掉） |
| 注册页 | 完成（手机号/邮箱双通道、验证码 + 60 秒倒计时） |
| ① 课程首页 | 完成（搜索、分类胶囊、广告位、热门课程、热度排行、上拉加载） |
| ② 视频 | 完成（上下滑短视频流、点赞/评论/收藏/分享） |
| ③ 社区 | 完成（分类、最新/最热/精华、文章流、发布入口） |
| ④ 个人主页 | 完成（头像昵称、连续签到、7 个功能入口、退出登录） |
| 课程详情 | 完成（章节目录、免费试看、评分、评价列表、加入课程） |
| 播放器 | 完成（全屏、进度心跳、动态水印） |
| 搜索 | 完成（热词榜、文章/视频双 Tab、分页） |
| 全部分类 | 完成（多级分类，点分类进筛选列表） |
| 文章详情 | 完成（HTML 正文、三级评论、点赞/收藏/分享/举报） |
| 发布/编辑文章 | 完成（封面上传、标签、存草稿/发布） |
| 我的发布 | 完成（状态筛选、编辑、删除） |
| 话题页 | 完成（按标签浏览） |
| 他人博客主页 | 完成（作者信息 + 统计 + 文章列表） |
| 编辑资料 | 完成（头像、昵称、性别、简介） |
| 我的课程 | 完成（学习进度、退出课程） |
| 我的收藏 | 完成（文章/视频筛选） |
| 观看历史 | 完成（进度、清空） |
| 笔记本 / 笔记详情 | 完成（搜索、编辑、删除） |
| 待办 | 完成（优先级、截止时间、完成切换、新建/删除） |
| 签到（4 种类型） | 完成（正常/位置/手势/拍照） |
| 签到记录 | 完成 |
| 设置 / 修改密码 | 完成 |
| 我的点赞 / 我的下载 | 接口已封装，页面未做（入口未开） |
| 从内容详情页新建笔记 | **未做**（接口 10.2 要求 sourceId 必填且必须命中真实内容，笔记入口需挂在文章/视频下） |

---

## 五、配色约定

配色不是随手定的，是从设计稿（启动页 750x1624）**逐像素采样**出来的。采样脚本在
`tools\sample_palette.ps1` 与 `tools\sample_palette2.ps1`，设计稿改版后可重跑核对。

### 5.1 品牌色

| 用途 | 色值 | 来源 |
|---|---|---|
| 品牌主色 | `#3677EF` | 大标题「学习平台」与底部 logo 文字的饱和色 |
| 主色浅版 | `#5B9BFF` | 插画里的蓝（屏幕 / 衣服） |
| 主色深版 | `#2A5FD0` | 渐变末端 |
| 深海军蓝 | `#282840` | 底部 logo 暗部 |
| 暖黄 | `#F0B000` | 便利贴 |
| 珊瑚红 | `#D04030` | 便利贴、告警 |
| 柔粉 | `#F0B0B0` | 椅子垫 |
| 青绿 | `#00B0C0` | 底图右下、logo 高光 |

全部集中在 `ui\theme\Color.kt`，换品牌色只动那一个文件。

### 5.2 渐变

设计稿是**四段**竖向渐变（暖白 → 杏 → 粉 → 白），不是两端插值。实采停靠点：

| 位置 | 色值 |
|---|---|
| 顶部 | `#FFFFFF` |
| 22% | `#FFFBF2`（暖奶油） |
| 38% | `#F8F0ED`（杏粉，最深处） |
| 72% | `#FDF5F5`（淡粉） |
| 底部 | `#FFFFFF` |

用 `ui\theme\GradientBackground.kt` 里的 `GradientBackground { }` 包住内容即可，
中间那层柔光圆通过 `showGlow` 开关。

**哪些页面铺整屏渐变**：启动页、登录页、注册页 —— 内容少，渐变是主体。
**哪些只铺顶部一条**：课程首页 —— `Brush.verticalGradient(0f to GradBlush, 0.22f to background)`
做顶部渐隐，往下很快回到中性灰蓝。列表页整屏渐变会让长时间阅读很累，这是有意区分。

### 5.3 两个坑

**坑一：`lightColorScheme` 只传几个参数，其余角色会退回紫色基线。**
症状是底栏选中胶囊变淡紫、图标文字变橙 —— 因为 `NavigationBarItem` 取的是
`secondaryContainer` / `onSecondaryContainer`，而不是 `primary`。
所以 `Theme.kt` 里必须把 `primaryContainer`、`secondaryContainer`、`surfaceContainer*`（5 档）、
`outline`、`inverse*` 全部显式定义。

**坑二：不配 splash 主题，冷启动会闪一下默认图标 + 绿底。**
`core-splashscreen` 需要 `Theme.X.Splash` 主题并在 Manifest 上引用，否则系统会用
`android:windowBackground` 加默认 launcher 图标。见 `res\values\themes.xml`：

```xml
<style name="Theme.LearnPlatform.Splash" parent="Theme.SplashScreen">
    <item name="windowSplashScreenBackground">@color/splash_background</item>
    <item name="windowSplashScreenAnimatedIcon">@drawable/ic_splash_logo</item>
    <item name="postSplashScreenTheme">@style/Theme.LearnPlatform</item>
</style>
```

启动窗口底色用**纯白**而不是品牌蓝：Compose 启动页顶部本来就是 `#FFFFFF`，
用蓝底会造成「蓝闪一下再变奶油底」的跳色。

### 5.4 底栏显式传色

不依赖默认角色映射：

```kotlin
NavigationBar(containerColor = BottomNavContainer) {   // 纯白
    NavigationBarItem(colors = NavigationBarItemDefaults.colors(
        selectedIconColor = BottomNavSelected,     // #3677EF
        selectedTextColor = BottomNavSelected,
        indicatorColor = BottomNavIndicator,       // #DCE8FF
        unselectedIconColor = BottomNavUnselected, // #8A9099
        unselectedTextColor = BottomNavUnselected
    ))
}
```

实测四个 Tab 的选中态都是 `#3677EF`、未选中 `#8A9099`
（用 `tools\check_gradient.ps1` 采样截图得到，不是肉眼判断）。

---

## 六、模拟器起不来时的处理

这台机器物理内存 15.5 GB，模拟器默认要 4 GB，容易报
`Insufficient RAM free for launching emulator`。用 2 GB 启动：

```powershell
# 先清掉卡死的实例
adb kill-server
Get-Process -Name 'qemu-system-x86_64','emulator' -ErrorAction SilentlyContinue | Stop-Process -Force

# 无窗口 + 2GB 内存启动
& 'D:\Android\Sdk\emulator\emulator.exe' -avd Small_Phone `
    -no-window -no-audio -no-boot-anim -memory 2048 `
    -gpu swiftshader_indirect -no-snapshot
```

等 `adb shell getprop sys.boot_completed` 返回 `1` 再装包。

> 视频页在模拟器上是黑屏：ExoPlayer 日志显示 Init/Release 正常、无解码错误，
> 是模拟器访问不了外网视频 CDN（`ping commondatastorage.googleapis.com` 100% 丢包）。
> 换能联网的真机，或把 `DemoData.playInfo` 的 `playUrl` 指向局域网流即可验证。
