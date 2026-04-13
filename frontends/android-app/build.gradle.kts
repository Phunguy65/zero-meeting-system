import com.diffplug.spotless.extra.wtp.EclipseWtpFormatterStep

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.hilt.android) apply false
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.navigation.safe.args) apply false
    kotlin("jvm") version libs.versions.kotlin.get() apply false
    alias(libs.plugins.spotless)
}

group = "io.github.phunguy65.zms.app"

spotless {
    format("xml") {
        target("**/*.xml")
        targetExclude("**/build/**", "**/generated/**", "**/.idea/**", "**/.gradle/**")
        eclipseWtp(EclipseWtpFormatterStep.XML)
        trimTrailingWhitespace()
        leadingTabsToSpaces(4)
        endWithNewline()
    }
}
