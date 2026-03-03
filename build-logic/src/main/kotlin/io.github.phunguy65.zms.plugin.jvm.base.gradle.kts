import org.gradle.accessors.dm.LibrariesForLibs

plugins {
    `java-base`
}
group = "io.github.phunguy65.ttbs.conventions"
version = "0.0.1-SNAPSHOT"

val libs = the<LibrariesForLibs>()

java {
    toolchain {
        languageVersion =
            JavaLanguageVersion.of(
                libs.versions.java
                    .get()
                    .toInt(),
            )
    }
}
