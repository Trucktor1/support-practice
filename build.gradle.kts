plugins {
    java
    jacoco
    kotlin("jvm") version "2.1.21"
    id("org.springframework.boot") version "3.5.7"
    id("io.spring.dependency-management") version "1.1.7"
    id("com.diffplug.spotless") version "7.0.4"
}

group = "ru.practice"

version = "1.0.0"

repositories { mavenCentral() }

java { toolchain { languageVersion = JavaLanguageVersion.of(21) } }

kotlin { jvmToolchain(21) }

extra["testcontainers.version"] = "1.21.4"

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.14")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation(kotlin("reflect"))
    runtimeOnly("org.postgresql:postgresql")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.testcontainers:postgresql:1.21.4")
    testImplementation("org.testcontainers:junit-jupiter:1.21.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test { useJUnitPlatform { excludeTags("integration", "e2e") } }

val integrationTest by
    tasks.registering(Test::class) {
        testClassesDirs = sourceSets.test.get().output.classesDirs
        classpath = sourceSets.test.get().runtimeClasspath
        useJUnitPlatform { includeTags("integration", "e2e") }
        shouldRunAfter(tasks.test)
    }

tasks.check { dependsOn(integrationTest) }

tasks.jacocoTestReport {
    dependsOn(tasks.test, integrationTest)
    executionData(fileTree(layout.buildDirectory.dir("jacoco")) { include("*.exec") })
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

spotless {
    java { googleJavaFormat("1.25.2") }
    kotlin { ktfmt("0.54").kotlinlangStyle() }
    kotlinGradle { ktfmt("0.54").kotlinlangStyle() }
}

tasks.bootJar { archiveFileName.set("app.jar") }

// Load Mockito instrumentation at JVM startup to avoid dynamic attachment restrictions.
val mockitoAgent by configurations.creating

dependencies { mockitoAgent("org.mockito:mockito-core") { isTransitive = false } }

tasks.withType<Test>().configureEach {
    jvmArgs("-javaagent:" + mockitoAgent.singleFile.absolutePath)
}
