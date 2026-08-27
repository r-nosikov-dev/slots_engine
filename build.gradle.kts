plugins {
    java
    id("org.springframework.boot") version "3.5.5" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
}

allprojects {
    group = "com.slotengine"
    version = "1.0.0"

    repositories {
        mavenCentral()
    }

    val buildRoot = System.getenv("SLOT_BUILD_DIR")
    if (!buildRoot.isNullOrBlank()) {
        layout.buildDirectory.set(file("$buildRoot/${project.name}"))
    }
}

subprojects {
    apply(plugin = "java")

    configure<JavaPluginExtension> {
        withJavadocJar()
        withSourcesJar()
    }

    tasks.withType<JavaCompile>().configureEach {
        options.release.set(21)
        options.encoding = "UTF-8"
        options.compilerArgs.addAll(listOf("-parameters", "-Xlint:unchecked"))
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
        testLogging {
            events("passed", "skipped", "failed")
            exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        }
    }

    dependencies {
        "testImplementation"("org.junit.jupiter:junit-jupiter:5.12.2")
        "testImplementation"("org.assertj:assertj-core:3.27.3")
        "testRuntimeOnly"("org.junit.platform:junit-platform-launcher")
    }
}
