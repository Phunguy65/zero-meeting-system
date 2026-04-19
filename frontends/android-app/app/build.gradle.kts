import org.gradle.api.tasks.testing.Test
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.google.services)
    alias(libs.plugins.openapiGenerator)
    alias(libs.plugins.navigation.safe.args)
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.fromTarget(libs.versions.java.get()))
    }
    java {
        toolchain {
            languageVersion.set(
                JavaLanguageVersion.of(
                    libs.versions.java
                        .get()
                        .toInt(),
                ),
            )
        }
    }
}

val unifiedSpec = rootProject.file("../../openapi/unified-openapi.yaml").absolutePath
val generatedDir =
    layout.buildDirectory
        .dir("generated/openapi")
        .get()
        .asFile.absolutePath

openApiGenerate {
    generatorName.set("java")
    library.set("retrofit2")
    inputSpec.set(unifiedSpec)
    outputDir.set(generatedDir)
    ignoreFileOverride.set(rootProject.file("app/.openapi-generator-ignore").absolutePath)
    apiPackage.set("io.github.phunguy65.zms.data.remote.api")
    modelPackage.set("io.github.phunguy65.zms.data.remote.dto")
    invokerPackage.set("io.github.phunguy65.zms.data.remote.client")
    configOptions.set(
        mapOf(
            "dateLibrary" to "java8",
            "serializationLibrary" to "jackson",
            "openApiNullable" to "false",
        ),
    )
    generateApiTests.set(false)
    generateModelTests.set(false)
}

android {
    namespace = "io.github.phunguy65.zms.frontends"
    compileSdk {
        version =
            release(
                libs.versions.androidCompileSdk
                    .get()
                    .toInt(),
            )
    }

    defaultConfig {
        applicationId = "io.github.phunguy65.zms.androidApp"
        minSdk =
            libs.versions.androidMinSdk
                .get()
                .toInt()
        targetSdk =
            libs.versions.androidTargetSdk
                .get()
                .toInt()
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        debug {
            buildConfigField("String", "API_BASE_URL", "\"http://10.0.2.2:8080\"")
        }
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            buildConfigField("String", "API_BASE_URL", "\"https://api.example.com\"")
        }
    }

    sourceSets {
        named("main") {
            java.directories.add(("$generatedDir/src/main/java"))
        }
    }
}

dependencies {
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.hilt.android)
    implementation(platform(libs.okhttp.bom))
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.jackson)
    implementation(libs.swagger.annotations)
    implementation(libs.jsr305)
    implementation(libs.jackson.databind)
    implementation(libs.jackson.datatype.jsr310)
    implementation(libs.retrofit.converter.scalars)
    implementation(libs.javax.annotation.api)
    implementation(libs.androidx.navigation.fragment)
    implementation(libs.androidx.navigation.ui)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.storage)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.google.id)
    implementation(libs.androidx.security.crypto)
    implementation(libs.glide)
    implementation(libs.lottie)
    implementation(libs.androidveil)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.swiperefreshlayout)
    implementation(libs.kizitonwose.calendar.view)
    annotationProcessor(libs.hilt.android.compiler)
    testImplementation(libs.junit4)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.inline)
    testImplementation(libs.androidx.arch.core.testing)
    androidTestImplementation(libs.androidx.testExt.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

tasks.named("preBuild") {
    dependsOn(tasks.named("openApiGenerate"))
}

tasks.named("openApiGenerate") {
    notCompatibleWithConfigurationCache(
        "openapi-generator-gradle-plugin does not support configuration cache",
    )
    doFirst {
        delete(generatedDir)
    }
}
