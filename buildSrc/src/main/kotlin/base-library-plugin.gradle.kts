import io.gitlab.arturbosch.detekt.detekt
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    java
    kotlin("jvm")
    id("io.gitlab.arturbosch.detekt")
}

repositories {
    mavenLocal()
    jcenter()
    maven {
        setUrl("https://kotlin.bintray.com/ktor")
    }
}

tasks {
    withType<JavaCompile> {
        sourceCompatibility = JavaVersion.VERSION_12.toString()
        targetCompatibility = JavaVersion.VERSION_12.toString()
        options.encoding = "UTF-8"
    }
}

apply {
    from("${rootProject.rootDir}/properties.gradle.kts")
}

val ktlint: Configuration = configurations.create("ktlint")

val kotlinVersion: String by extra

dependencies {
    compile(kotlin("stdlib-jdk8", kotlinVersion))
    compile(kotlin("reflect", kotlinVersion))

    ktlint(Kotlin.ktlint)
}

tasks {
    withType<KotlinCompile> {
        kotlinOptions {
            jvmTarget = JavaVersion.VERSION_12.toString()
            freeCompilerArgs += listOf(
                "-java-parameters",
                "-Xjsr305=strict",
                "-progressive")
        }
    }

    val ktlint by creating(JavaExec::class) {
        group = "verification"
        description = "Check Kotlin code style."
        classpath = configurations["ktlint"]
        main = "com.github.shyiko.ktlint.Main"
        args = listOf("src/**/*.kt")
    }

    "check" {
        dependsOn(ktlint)
    }

    create("ktlintFormat", JavaExec::class) {
        group = "formatting"
        description = "Fix Kotlin code style deviations."
        classpath = configurations["ktlint"]
        main = "com.github.shyiko.ktlint.Main"
        args = listOf("-F", "src/**/*.kt")
    }
}

val detektVersion: String by project

detekt {
    toolVersion = detektVersion
    input = files("$rootDir/src/main/kotlin")
    config = files("$rootDir/detekt-config.yml")
    filters = ".*/resources/.*,.*/build/.*"
}
