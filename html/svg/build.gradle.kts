import org.jetbrains.compose.gradle.standardConf

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}


kotlin {
    jvm()
    js(IR) {
        browser() {
            testTask {
                useKarma {
                    standardConf()
                }
            }
        }
        binaries.executable()
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(compose.runtime)
            }
        }

        val jsMain by getting {
            dependencies {
                api(project(":html-core"))
            }
        }

        val jsTest by getting {
            languageSettings {
                optIn("org.jetbrains.compose.web.testutils.ComposeWebExperimentalTestsApi")
            }
            dependencies {
                implementation(project(":html-test-utils"))
                implementation(kotlin("test-js"))
                implementation(libs.kotlinx.coroutines.core)
            }
        }

        val jvmMain by getting {
            dependencies {
                api(project(":html-core"))
            }
        }

        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test-junit"))
            }
        }
    }
}
