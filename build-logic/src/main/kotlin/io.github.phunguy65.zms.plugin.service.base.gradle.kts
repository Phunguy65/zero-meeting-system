import com.google.protobuf.gradle.id
import org.gradle.accessors.dm.LibrariesForLibs
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.the


plugins {
    id("io.github.phunguy65.zms.plugin.jvm.base")
    id("io.github.phunguy65.zms.plugin.spotless")
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    id("org.hibernate.orm")
    id("org.graalvm.buildtools.native")
    id("com.google.protobuf")
    java
}

group = "io.github.phunguy65.zms"
version = "0.0.1-SNAPSHOT"

val libs = the<LibrariesForLibs>()
dependencies {
    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.spring.boot.starter.amqp)
    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.spring.boot.starter.integration)
    implementation(libs.spring.boot.starter.security)
    implementation(libs.spring.boot.starter.webmvc)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.spring.rabbit.stream)
    implementation(libs.spring.integration.amqp)
    implementation(libs.spring.integration.http)
    implementation(libs.spring.integration.jpa)
    implementation(libs.spring.security.messaging)
    implementation(libs.jjwt.api)
    compileOnly(libs.jspecify)
    runtimeOnly(libs.jjwt.impl)
    runtimeOnly(libs.jjwt.jackson)
    developmentOnly(libs.spring.boot.devtools)
    runtimeOnly(libs.postgresql)
    runtimeOnly(libs.bouncycastle)
    annotationProcessor(libs.spring.boot.configuration.processor)
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.spring.boot.starter.security.test)
    testImplementation(libs.spring.integration.test)
    testRuntimeOnly(libs.junit.platform.launcher)
    testRuntimeOnly(libs.h2)
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

hibernate {
    enhancement {
        enableLazyInitialization = true
        enableDirtyTracking = true
        enableAssociationManagement = false
    }
}
protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:${libs.versions.protoc.get()}"
    }
    plugins {
        id("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:${libs.versions.protocGenGrpcJava.get()}"
        }
    }
    generateProtoTasks {
        all().forEach {
            it.plugins {
                id("grpc") {
                    option("@generated=omit")
                }
            }
        }
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
