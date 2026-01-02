plugins {
    id("java")
}

group = "dev.quark"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

subprojects {
    apply(plugin = "java-library")
    apply(plugin = "maven-publish")

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(23))
        }
        withSourcesJar()
        withJavadocJar()
    }

    extensions.configure<PublishingExtension>("publishing") {
        publications {
            register<MavenPublication>("maven") {
                from(components["java"])
            }
        }
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:6.0.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    implementation("com.fasterxml.jackson.core:jackson-databind:2.20.1")
    implementation("org.bouncycastle:bcprov-jdk15on:1.70")
}

tasks.test {
    useJUnitPlatform()
}