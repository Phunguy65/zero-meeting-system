import org.gradle.accessors.dm.LibrariesForLibs
import org.gradle.kotlin.dsl.the

plugins {
    id("io.github.phunguy65.zms.plugin.spotless")
    id("io.github.phunguy65.zms.plugin.jvm.base")
    id("io.github.phunguy65.zms.plugin.service.base")
}

group = "io.github.phunguy65.zms.services"
version = "0.0.1-SNAPSHOT"
description = "user-management"

val libs = the<LibrariesForLibs>()

dependencies {
    implementation(libs.shared)
    implementation(libs.uuid.creator)
    implementation(libs.spring.boot.starter.flyway)
    implementation(libs.cloudevents.kafka)
    implementation(libs.jackson.databind.nullable)
    implementation(libs.firebase.admin)
    runtimeOnly(libs.flyway.database.postgresql)
}

hibernate {
    enhancement {
        enableAssociationManagement = false
        classNames.addAll(
            "io.github.phunguy65.zms.usermanagement.infrastructure.persistence.UserJpaEntity",
            "io.github.phunguy65.zms.usermanagement.infrastructure.persistence.RefreshTokenJpaEntity",
            "io.github.phunguy65.zms.usermanagement.infrastructure.persistence.OutboxEventEntity",
        )
    }
}
