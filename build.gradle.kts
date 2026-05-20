plugins {
    java
    application
}

group = "org.gulash.demo"
version = "1.0-SNAPSHOT"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // JUnit 5: современный фреймворк для тестирования.
    // Используется для демонстрации работы Lock в проверяемых сценариях.
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")

    // SLF4J + Logback: стандарт логирования.
    // Позволяет наглядно видеть порядок выполнения потоков.
    implementation("org.slf4j:slf4j-api:2.0.12")
    implementation("ch.qos.logback:logback-classic:1.5.3")
}

application {
    mainClass.set("org.gulash.demo.reentrantlock.Main")
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = true
    }
}
