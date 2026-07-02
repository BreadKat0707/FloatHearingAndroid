import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

// 读取版本属性（可通过 -PversionName / -PversionCode / -PisRelease 覆盖）
val releaseVersionName: String = findProperty("versionName")?.toString()
    ?: providers.gradleProperty("fh.versionName").orNull
    ?: "1.0.0"

val baseVersionCode: Int? = findProperty("versionCode")?.toString()?.toIntOrNull()
    ?: providers.gradleProperty("fh.versionCode").orNull?.toIntOrNull()

val isReleaseBuild: Boolean = findProperty("isRelease")?.toString()?.toBooleanStrictOrNull()
    ?: gradle.startParameter.taskNames.any { it.contains("Release", ignoreCase = true) }

val isCiBuild: Boolean = providers.environmentVariable("CI").orNull == "true"
    || providers.environmentVariable("GITHUB_ACTIONS").orNull == "true"

val gitCommitShort: String = providers.exec {
    commandLine("git", "rev-parse", "--short", "HEAD")
    isIgnoreExitValue = true
}.standardOutput.asText.orNull?.trim() ?: "unknown"

fun computeVersionCode(): Int {
    return baseVersionCode ?: run {
        val now = LocalDate.now()
        val datePrefix = now.format(DateTimeFormatter.ofPattern("yyMMdd"))
        val counterFile = layout.projectDirectory.file(".release-counter.properties").asFile
        val props = if (counterFile.exists()) Properties().apply {
            counterFile.inputStream().use { load(it) }
        } else Properties()

        val lastDate = props.getProperty("lastDate", "")
        val lastCounter = props.getProperty("counter", "0")?.toIntOrNull() ?: 0

        val (newDate, newCounter) = if (lastDate == datePrefix) {
            datePrefix to (lastCounter + 1)
        } else {
            datePrefix to 1
        }

        // 仅在实际发布构建时写入计数器，避免 debug 构建污染当天计数
        if (isReleaseBuild) {
            props.setProperty("lastDate", newDate)
            props.setProperty("counter", newCounter.toString())
            counterFile.parentFile?.mkdirs()
            counterFile.outputStream().use { props.store(it, "FloatHearing release counter") }
        }

        "${newDate}${String.format("%02d", newCounter)}".toInt()
    }
}

fun computeVersionName(): String {
    return when {
        isCiBuild -> "$releaseVersionName-dev-$gitCommitShort"
        isReleaseBuild -> releaseVersionName
        else -> "$releaseVersionName-debug"
    }
}

val resolvedVersionCode = computeVersionCode()
val resolvedVersionName = computeVersionName()

android {
    namespace = "cn.lemondrop.fhreborn"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "cn.lemondrop.fhreborn"
        minSdk = 31
        targetSdk = 36
        versionCode = resolvedVersionCode
        versionName = resolvedVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            storeFile = file("fh-release.keystore")
            storePassword = System.getenv("STORE_PASSWORD") ?: ""
            keyAlias = System.getenv("KEY_ALIAS") ?: ""
            keyPassword = System.getenv("KEY_PASSWORD") ?: ""
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            applicationIdSuffix = ".debug"
            isDebuggable = true
        }
    }

    // 输出 APK 命名（AGP 9.x 推荐通过 androidComponents.onVariants 修改）
    androidComponents {
        onVariants { variant ->
            val isVariantRelease = variant.buildType == "release"
            variant.outputs.forEach { output ->
                val fileName = buildString {
                    append(defaultConfig.applicationId)
                    append("_")
                    when {
                        isCiBuild -> {
                            append(resolvedVersionName)
                            append("_dev_")
                            append(gitCommitShort)
                        }
                        isVariantRelease -> {
                            append("release_")
                            append(resolvedVersionName)
                        }
                        else -> {
                            append("debug_")
                            append(resolvedVersionName)
                        }
                    }
                    append("_")
                    append(resolvedVersionCode)
                    append("_")
                    append("universal")
                    append(".apk")
                }
                output.outputFileName.set(fileName)
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll(
            "-opt-in=androidx.compose.foundation.layout.ExperimentalLayoutApi"
        )
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation("androidx.compose.material3:material3-window-size-class")
    implementation(libs.androidx.compose.animation)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // Navigation
    implementation(libs.navigation.compose)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Media3
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.session)

    // DataStore
    implementation(libs.datastore.preferences)

    // Coroutines
    implementation(libs.coroutines.android)

    // Lucide icons00
    implementation(libs.lucide)

    implementation("io.github.compose-fluent:fluent:v0.1.0")


    // Clover UI
    implementation("cn.lemondrop.clover:clover-ui:0.1.0-SNAPSHOT")

    // Haze - Material Design 毛玻璃效果
    implementation("dev.chrisbanes.haze:haze:1.7.2")

    //Accompanist Lyric
    implementation("com.mocharealm.accompanist:lyrics-ui:1.0.19")
    implementation("com.mocharealm.accompanist:lyrics-core:0.4.7")

    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
