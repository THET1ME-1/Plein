import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    // Версию AGP не указываем: она уже пришла из корневого проекта.
    id("com.android.test")
    alias(libs.plugins.baselineprofile)
}

/**
 * Модуль записи baseline profile.
 *
 * Собственного приложения не имеет: он запускает Plein на подключённом
 * телефоне, проходит по домашнему экрану и записывает, какой код при этом
 * исполняется. Результат ложится в app/src/main/generated и едет в каждой
 * сборке.
 *
 * Запись: ./gradlew :app:generateReleaseBaselineProfile
 * Нужен подключённый телефон или запущенный эмулятор.
 */
android {
    namespace = "app.plein.baseline"
    compileSdk = 37

    defaultConfig {
        minSdk = 28
        targetSdk = 37
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    targetProjectPath = ":app"
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

baselineProfile {
    useConnectedDevices = true
}

dependencies {
    implementation(libs.androidx.benchmark.macro)
    implementation(libs.androidx.uiautomator)
    implementation(libs.androidx.junit)
    implementation(libs.junit)
}
