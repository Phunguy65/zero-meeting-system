import org.gradle.api.tasks.testing.Test

plugins {
    id("io.github.phunguy65.zms.plugin.spotless")
    id("io.github.phunguy65.zms.plugin.jvm.base")
    id("io.github.phunguy65.zms.plugin.service.base")
}

group = "io.github.phunguy65.zms.services"
version = "0.0.1-SNAPSHOT"
description = "chat-management"

dependencies {
    implementation(libs.shared)
    implementation(libs.spring.boot.starter.data.mongodb)
    implementation(libs.spring.kafka)
    implementation(libs.cloudevents.kafka)
    implementation(libs.jjwt.api)
    implementation(libs.livekit.server)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.spring.boot.starter.security)
    runtimeOnly(libs.jjwt.impl)
    runtimeOnly(libs.jjwt.jackson)
    testImplementation(testFixtures(libs.shared))
    testImplementation(libs.spring.boot.data.mongodb.test)
    testImplementation(libs.testcontainers.mongodb)
    testImplementation(libs.archunit.junit5)
    testRuntimeOnly("com.h2database:h2")
}

val testTask = tasks.named<Test>("test")

tasks.register<Test>("generateOpenApiDocsFromTests") {
    group = "openapi"
    description = "Generate the chat-management OpenAPI spec via SpringBootTest"
    testClassesDirs = testTask.get().testClassesDirs
    classpath = testTask.get().classpath
    useJUnitPlatform()
    filter {
        includeTestsMatching("*OpenApiGenerationTest")
    }
    outputs.file(layout.buildDirectory.file("openapi/openapi.yaml"))
    shouldRunAfter(testTask)
}
