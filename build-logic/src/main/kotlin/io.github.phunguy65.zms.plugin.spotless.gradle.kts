import org.gradle.accessors.dm.LibrariesForLibs
import org.gradle.kotlin.dsl.the

plugins {
    id("com.diffplug.spotless")
}

group = "io.github.phunguy65.ttbs.conventions"
version = "0.0.1-SNAPSHOT"

val libs = the<LibrariesForLibs>()

spotless {
    java {
        target("**/src/**/*.java")
        targetExclude("**/build/**", "**/generated/**", "**/third-party/**")
        palantirJavaFormat(libs.versions.palantirJavaFormat.get()).style("AOSP")
        formatAnnotations()
        trimTrailingWhitespace()
        endWithNewline()
        importOrder()
        removeUnusedImports()
        licenseHeader("")
    }
    kotlinGradle {
        target("**/*.gradle.kts")
        targetExclude("**/build/**", "**/generated/**", "**/gradle/**", "**/bin/**", "**/third-party/**")
        ktlint(libs.versions.ktlint.get())
        trimTrailingWhitespace()
        endWithNewline()
    }
}
