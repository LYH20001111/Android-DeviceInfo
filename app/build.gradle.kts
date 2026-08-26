import com.android.build.api.variant.impl.VariantOutputImpl
import org.gradle.language.nativeplatform.internal.Dimensions.applicationVariants
import java.text.SimpleDateFormat
import java.util.Date

plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.newland.deviceinformation"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.newland.deviceinformation"
        minSdk = 24
        targetSdk = 36
        versionCode = 2
        versionName = "2.0.1"

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

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

androidComponents {
    onVariants { variant ->
        // 1. 获取时间戳
        val time = SimpleDateFormat("yyyyMMddHHmm").format(Date())

        // 2. 获取版本信息 (从 android 闭包的全局配置中读取)
        val vCode = android.defaultConfig.versionCode ?: 0
        val vName = android.defaultConfig.versionName ?: "1.0.0"

        // 3. 获取构建类型和渠道名
        val buildTypeName = variant.buildType ?: "UNKNOWN"
        val flavorName = variant.flavorName ?: ""

        // 4. 拼接文件名逻辑
        // 如果有渠道(flavor)，优先使用渠道名，否则使用项目名
        var baseName = "DEVICE-INFO"

        val apkName = "${baseName}-${vName}-${buildTypeName}-${time}"
        val finalFileName = apkName.uppercase() + ".apk"

        // 5. 核心修改：设置输出文件名
        variant.outputs.forEach { output ->
            if (output is VariantOutputImpl) {
                output.outputFileName.set(finalFileName)
            }
        }
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}