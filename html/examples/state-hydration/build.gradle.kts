import org.jetbrains.compose.gradle.standardConf
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("multiplatform")
    kotlin("plugin.spring") version "2.3.20"
    kotlin("plugin.jpa") version "2.3.20"
    kotlin("plugin.serialization") version "2.3.20"
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.springframework.boot") version "4.1.0"
    id("io.spring.dependency-management") version "1.1.7"
}

kotlin {
    jvm {
        mainRun {
            mainClass.set("com.example.MainKt")
        }
        compilerOptions {
            jvmTarget = JvmTarget.JVM_23
            freeCompilerArgs.add("-Xjsr305=strict")
        }
    }
    js(IR) {
        browser {
            binaries.executable()
            testTask {
                useKarma {
                    standardConf()
                }
            }
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":html-core"))
                implementation(compose.runtime)
                implementation(libs.kotlinx.coroutines.core)
                implementation(ktorLibs.client.core)
                implementation(ktorLibs.client.contentNegotiation)
                implementation(ktorLibs.serialization.kotlinx.json)

                implementation(libs.kotlinx.serialization.json)
            }
        }

        val jsMain by getting {
            dependencies {
                implementation(ktorLibs.client.js)
            }
        }

        val jsTest by getting {
            dependencies {
                implementation(kotlin("test-js"))
            }
        }

        val jvmMain by getting {
            dependencies {
                implementation("org.springframework.boot:spring-boot-starter-webmvc")
                implementation("tools.jackson.module:jackson-module-kotlin")
                implementation("org.springframework.boot:spring-boot-starter-data-jpa")
                runtimeOnly("com.h2database:h2")
                implementation("org.jetbrains.kotlin:kotlin-reflect")
                implementation("org.springframework.boot:spring-boot-devtools")
                implementation(ktorLibs.client.java)
            }
        }

        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test-junit"))
                implementation("org.springframework.boot:spring-boot-starter-test")
            }
        }
    }
}
