plugins {
    `base-library-plugin`
    application
    id("com.bmuschko.docker-java-application")
}

application {
    mainClassName = "io.ktor.server.netty.EngineMain"
}

docker {
    javaApplication {
        baseImage.set("openjdk:12.0.2-jdk-oraclelinux7")
        ports.set(listOf(8080))
        tag.set("challenge/accounting:$version")
        jvmArgs.set(listOf(
            "-XX:InitialRAMPercentage=20",
            "-XX:MaxRAMPercentage=85"
        ))
    }
}

dependencies {
    compile(Logging.logback)
    compile(Ktor.netty)
    compile(Ktor.core)
    compile(Ktor.hostCommon)
    compile(Ktor.jackson)
    compile(Jackson.time)
    compile(Utils.hashids)

    testCompile(Kotlinx.coroutinesTest)
    testCompile(Ktor.testHost)
}
