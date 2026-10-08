plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
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

        // 后端 BaseUrl：由 BuildConfig 注入，code 侧通过 Constants.BASE_URL 读取。
        // v3.0 的 URL 前缀为 /api/v1（不再是 /api/v1/app）。
        buildConfigField("String", "BASE_URL", "\"https://api.learnplatform.com/api/v1/\"")

        // UI 预览开关：true 时 Repository 直接返回本地样例数据，不发任何网络请求。
        // 用途：后端未就绪时先把界面跑起来给人看（四个 Tab、列表、详情、空/错状态）。
        // 关掉它：./gradlew :app:assembleDebug -PuiPreview=false
        val uiPreview = (project.findProperty("uiPreview") as String?)?.toBoolean() ?: true
        buildConfigField("boolean", "UI_PREVIEW", uiPreview.toString())
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

            // 生产构建关掉 UI 预览短路，走真实网络路径。
            // DemoData 与 58 处 preview 分支会被 R8 一并裁掉（DemoData.enabled 是常量 false）。
            buildConfigField("boolean", "UI_PREVIEW", "false")
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
    // Core / Lifecycle
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.splashscreen)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
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
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
}

// ---------------------------------------------------------------------------
// 依赖对齐：AGP 9 内置的 Kotlin 编译器为 2.2.x，无法读取 kotlin-stdlib 2.4 的元数据。
// 而 Coil 3.6.2 与 androidx.collection 1.5.0（Room 2.8.5 传递依赖）会把 stdlib 顶到 2.4.10，
// 导致 "Class 'kotlin.Unit' was compiled with an incompatible version of Kotlin"。
// kotlin-stdlib 向后兼容，这里统一压回与编译器一致的版本。
// 若将来 AGP 内置 Kotlin 升级到 2.4+，可删除此约束。
// ---------------------------------------------------------------------------
configurations.configureEach {
    resolutionStrategy {
        force("org.jetbrains.kotlin:kotlin-stdlib:2.2.10")
    }
}

// ---------------------------------------------------------------------------
// JVM 目标对齐：toolchain 是 JDK 25，若不做限制 Kotlin 会回退到 JVM_24 目标，
// 而 compileOptions 指定的是 17，两者不一致（且 Android 不需要 24 字节码）。
// 这里显式钉到 17，与 Java 保持一致。
// ---------------------------------------------------------------------------
kotlin {
    jvmToolchain(17)
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}
