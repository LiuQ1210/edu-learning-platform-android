import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

// Taku 的 AppID / AppKey / PlacementID / DebugKey 只存在 local.properties，不进仓库
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

android {
    namespace = "com.github.learningplatform"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.github.learningplatform"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // 后端 BaseUrl：默认指向当前部署的演示服务器。
        //
        // 本机联调时用 -PbaseUrl 覆盖，不需要改代码：
        //   ./gradlew :app:assembleDebug -PbaseUrl=http://127.0.0.1:80/
        // 本机后端是 server.port=80、context-path=/；模拟器走 adb reverse tcp:80 tcp:80
        // （Android 16 起 10.0.2.2 会被「本地网络访问限制」拦截，回环地址不受影响）。
        // Retrofit 要求 baseUrl 必须以 / 结尾，这里统一补齐。
        val rawBaseUrl = (project.findProperty("baseUrl") as String?)?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: localProperties.getProperty("baseUrl")?.trim()?.takeIf { it.isNotEmpty() }
            ?: "http://192.168.100.3:1000/"
        val baseUrl = if (rawBaseUrl.endsWith("/")) rawBaseUrl else "$rawBaseUrl/"
        buildConfigField("String", "BASE_URL", "\"$baseUrl\"")

        buildConfigField("String", "TAKU_APP_ID", "\"${localProperties.getProperty("taku.appId").orEmpty()}\"")
        buildConfigField("String", "TAKU_APP_KEY", "\"${localProperties.getProperty("taku.appKey").orEmpty()}\"")
        buildConfigField("String", "TAKU_PLACEMENT_ID", "\"${localProperties.getProperty("taku.placementId").orEmpty()}\"")
        buildConfigField("String", "TAKU_SPLASH_PLACEMENT_ID", "\"${localProperties.getProperty("taku.splashPlacementId").orEmpty()}\"")
        buildConfigField("String", "TAKU_NATIVE_PLACEMENT_ID", "\"${localProperties.getProperty("taku.nativePlacementId").orEmpty()}\"")
        buildConfigField("String", "TAKU_HOME_NATIVE_PLACEMENT_ID", "\"${localProperties.getProperty("taku.homeNativePlacementId").orEmpty()}\"")
        buildConfigField("String", "TAKU_COMMUNITY_NATIVE_PLACEMENT_ID", "\"${localProperties.getProperty("taku.communityNativePlacementId").orEmpty()}\"")
        buildConfigField("String", "TAKU_REWARD_PLACEMENT_ID", "\"${localProperties.getProperty("taku.rewardPlacementId").orEmpty()}\"")

    }

    /*
     * 发布签名。
     *
     * 口令优先从环境和 Gradle 属性读，默认值仅供本地演示构建使用 ——
     * 正式发布请通过 -PKEYSTORE_PASS=xxx 传入，不要把真实口令提交进仓库。
     *
     * 注意：keystore 路径用 rootProject 相对定位，别写绝对路径，
     * 否则换台机器就构建不了。
     */
    signingConfigs {
        create("release") {
            storeFile = rootProject.file("keystore/release.jks")
            storePassword = (project.findProperty("KEYSTORE_PASS") as String?) ?: "learn123456"
            keyAlias = (project.findProperty("KEY_ALIAS") as String?) ?: "learnplatform"
            keyPassword = (project.findProperty("KEY_PASS") as String?) ?: "learn123456"
        }
    }

    /*
     * Release 构建关闭 lintVital。
     *
     * 为什么：assembleRelease 默认会触发 lintVitalRelease，而它需要下载 lint 工具
     * （com.android.tools.lint:lint-* 系列）。在本项目的网络环境下拉不下来，
     * 报 "Failed to calculate the value of task 'lintTool.versionKey'"，把 release 构建整个卡死。
     *
     * 这不是"跳过检查换速度"—— 本机的 lint 从来没跑成功过（工具没下载），
     * 所以关掉它不损失任何已有的检查能力。
     * 需要 lint 时单独跑 `:app:lintDebug`，或先解决网络问题再打开。
     */
    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")

            // 开启 R8 压缩与混淆。
            // keep 规则在 src/main/keepRules/rules.keep；
            // Hilt / Room / Compose 的规则由各自 AAR 的 consumer rules 自动合并，不用手写。
            isMinifyEnabled = true
            isShrinkResources = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    testOptions {
        unitTests {
            // 让单测能拿到真实的 strings/colors 等 Android 资源
            isIncludeAndroidResources = true
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Compose BOM 只声明一次，implementation / androidTestImplementation 复用
    val composeBom = platform(libs.androidx.compose.bom)
    // Core / Lifecycle
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.splashscreen)

    // Compose
    implementation(composeBom)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.navigation.compose)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // Network
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)

    // Local storage
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    implementation(libs.room.paging)
    ksp(libs.room.compiler)
    implementation(libs.datastore.preferences)

    // Image
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)

    // Paging
    implementation(libs.paging.runtime)
    implementation(libs.paging.compose)

    // Video
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.ui)
    implementation(libs.media3.session)

    // Log
    implementation(libs.timber)

    // Ads（Taku 聚合 + 酷盈，调试阶段无其他广告源）
    implementation("com.anythink.sdk:core-taku:6.6.40")
    implementation("com.anythink.sdk:adapter-taku-sdm:6.5.90.1.0")
    implementation("com.smartdigimkttech.sdk:sdm-sdk-cn:6.5.90")

    // Test
    //
    // 刻意只用 JUnit + coroutines-test，**不引入 Robolectric**。
    // 原因：Robolectric 会拖一个 100MB+ 的 nativeruntime 包，而在本项目的网络环境下
    // 从 Maven Central 拉取不稳定（实测 21 分钟超时）。缓存逻辑本身不依赖 Android，
    // 用「假 DAO」就能测，没必要为了测试引入重型依赖。
    // 需要真正的 Room 行为验证时（如 SQL 语义），写 instrumented test 更合适。
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(composeBom)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
}

/**
 * 将kotlin标准库版本控制防止编译错误
 */
configurations.configureEach {
    resolutionStrategy {
        force("org.jetbrains.kotlin:kotlin-stdlib:2.2.10")
    }
}

/**
 * 编译一致性，消除警告和错误
 */
kotlin {
    jvmToolchain(17)
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

/**
 * Android 16（API 36+）有本地网络访问限制，app 不能直连 10.0.2.2，
 * baseUrl 用的是 127.0.0.1:80，必须靠 `adb reverse tcp:80 tcp:80` 把设备回环
 * 转发到宿主机 80。reverse 在模拟器/电脑重启后会丢，这里让 installDebug 装完自动重建，
 * 以后只管 `./gradlew installDebug`，不用手动敲 reverse。
 */
tasks.register("adbReverse80") {
    group = "run"
    description = "After installDebug, re-run: adb reverse tcp:80 tcp:80"
    doLast {
        val adb = "D:\\Android\\Sdk\\platform-tools\\adb.exe"
        try {
            val p = Runtime.getRuntime().exec(arrayOf(adb, "reverse", "tcp:80", "tcp:80"))
            p.waitFor()
            println(">> adb reverse tcp:80 tcp:80 OK")
        } catch (e: Exception) {
            println(">> adb reverse 失败（设备未连？）: ${e.message}")
        }
    }
}
tasks.whenTaskAdded {
    if (name == "installDebug") finalizedBy("adbReverse80")
}
