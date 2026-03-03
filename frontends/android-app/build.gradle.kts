plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.hilt.android) apply false
    alias(libs.plugins.ksp) apply false
    kotlin("jvm") version libs.versions.kotlin.get() apply false
}

group = "io.github.phunguy65.zms.app"
