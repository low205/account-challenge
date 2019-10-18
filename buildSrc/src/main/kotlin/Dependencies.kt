object Kotlin {
    const val ktlintVersion = "0.29.0"
    const val ktlint = "com.github.shyiko:ktlint:$ktlintVersion"
    const val test = "org.jetbrains.kotlin:kotlin-test"
    const val testCommon = "org.jetbrains.kotlin:kotlin-test-common"
    const val testAnnotationsCommon = "org.jetbrains.kotlin:kotlin-test-annotations-common"
}

object Ktor {
    const val ktorVersion = "1.2.4"
    const val netty = "io.ktor:ktor-server-netty:$ktorVersion"
    const val core = "io.ktor:ktor-server-core:$ktorVersion"
    const val hostCommon = "io.ktor:ktor-server-host-common:$ktorVersion"
    const val jackson = "io.ktor:ktor-jackson:$ktorVersion"
    const val testHost = "io.ktor:ktor-server-test-host:$ktorVersion"
}

object Jackson {
    const val jacksonVersion = "2.9.9"
    const val time = "com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersion"
}

object Utils {
    const val hashids = "org.hashids:hashids:1.0.3"
}

object Kotlinx {
    const val kotlinxVersion = "1.3.2"
    const val coroutinesTest = "org.jetbrains.kotlinx:kotlinx-coroutines-test:$kotlinxVersion"
}

object Logging {
    const val logback = "ch.qos.logback:logback-classic:1.2.1"
}
