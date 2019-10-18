plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    jcenter()
    gradlePluginPortal()
}

apply {
    from("${rootDir.parentFile}/properties.gradle.kts")
}

val kotlinVersion: String by extra
val detektVersion: String by extra
val shadowVersion: String by extra
val dockerPluginVersion: String by extra

dependencies {
    compile(kotlin("gradle-plugin", version = kotlinVersion))
    compile(kotlin("stdlib-jdk8", version = kotlinVersion))
    compile(kotlin("reflect", version = kotlinVersion))
    compile("io.gitlab.arturbosch.detekt:detekt-gradle-plugin:$detektVersion")
    compile("com.github.jengelman.gradle.plugins:shadow:$shadowVersion")
    compile("com.bmuschko:gradle-docker-plugin:$dockerPluginVersion")
}
