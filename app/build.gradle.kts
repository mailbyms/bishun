plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.gyjian.bishun"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.gyjian.bishun"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    // 配置APK分割以支持不同架构
    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "armeabi-v7a", "x86", "x86_64")
            isUniversalApk = true
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }

    // 配置APK命名规则
    applicationVariants.all {
        val variant = this
        variant.outputs.all {
            val output = this as com.android.build.gradle.internal.api.BaseVariantOutputImpl
            val buildType = variant.buildType.name
            val versionName = variant.versionName
            val appName = "bishun"

            // 从输出文件名中提取ABI信息
            val originalName = output.outputFileName
            val abi = when {
                originalName.contains("arm64-v8a") -> "arm64-v8a"
                originalName.contains("armeabi-v7a") -> "armeabi-v7a"
                originalName.contains("x86_64") -> "x86_64"
                originalName.contains("x86") -> "x86"
                originalName.contains("universal") -> "universal"
                else -> "universal"
            }

            // 自定义APK文件名格式：应用名称_版本号_构建类型_架构.apk
            output.outputFileName = "${appName}_v${versionName}_${buildType}_${abi}.apk"
        }
    }
  }

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.webkit)
    implementation(libs.material)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}