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
}
