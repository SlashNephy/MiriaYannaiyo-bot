dependencies {
    api(kotlin("stdlib-jdk8"))
    
    api("jp.nephy:jsonkt:5.0.0-eap-23")
    api("org.jetbrains.kotlinx:kotlinx-serialization-runtime:0.14.0")
    api("io.ktor:ktor-client-core-jvm:1.3.1")
    api("org.litote.kmongo:kmongo-coroutine:3.12.2") {
        exclude("org.jetbrains.kotlin")
    }
    api("ch.qos.logback:logback-classic:1.2.3")
    
    api(kotlin("reflect"))

    api("io.github.microutils:kotlin-logging:1.7.8")
    api("io.ktor:ktor-client-apache:1.3.1")
}
