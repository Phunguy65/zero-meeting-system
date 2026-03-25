plugins {
    id("io.github.phunguy65.zms.plugin.spotless")
    id("io.github.phunguy65.zms.plugin.jvm.base")
    id("io.github.phunguy65.zms.plugin.service.base")
}

group = "io.github.phunguy65.zms.services"
version = "0.0.1-SNAPSHOT"
description = "meeting-management"

dependencies {
    implementation(libs.shared)
    implementation(libs.uuid.creator)
    implementation(libs.spring.boot.starter.flyway)
    implementation(libs.cloudevents.kafka)
    implementation(libs.jackson.databind.nullable)
    implementation(libs.livekit.server)
    implementation(libs.spring.boot.starter.security)
    implementation(libs.bouncycastle)
    runtimeOnly(libs.flyway.database.postgresql)
    testImplementation(testFixtures(libs.shared))
}
