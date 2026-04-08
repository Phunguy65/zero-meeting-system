import org.gradle.api.tasks.testing.Test
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.openapiGenerator)
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
    apiPackage.set("io.github.phunguy65.zms.sdk.api")
    modelPackage.set("io.github.phunguy65.zms.sdk.model")
    invokerPackage.set("io.github.phunguy65.zms.sdk.invoker")
    configOptions.set(
        mapOf(
            "dateLibrary" to "java8",
            "serializationLibrary" to "gson",
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

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    sourceSets {
        named("main") {
            java.srcDir("$generatedDir/src/main/java")
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
    implementation(libs.retrofit.gson)
    implementation(libs.swagger.annotations)
    implementation(libs.jsr305)
    implementation(libs.gson)
    implementation(libs.gson.fire)
    implementation(libs.retrofit.converter.scalars)
    implementation(libs.javax.annotation.api)
    implementation(libs.jackson.databind.nullable)
    ksp(libs.hilt.android.compiler)
    testImplementation(libs.junit4)
    androidTestImplementation(libs.androidx.testExt.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

tasks.named("preBuild") {
    dependsOn(tasks.named("openApiGenerate"))
}

tasks.named("openApiGenerate") {
    doFirst {
        delete(generatedDir)
    }
}
