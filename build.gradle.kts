plugins {
    id("org.jetbrains.kotlin.jvm") version "1.5.21"
    id("org.jetbrains.kotlin.kapt") version "1.5.21"
    id("org.jetbrains.kotlin.plugin.allopen") version "1.5.21"
    id("com.github.johnrengelman.shadow") version "7.1.0"
    id("io.micronaut.application") version "2.0.8"
}

version = "0.1"
group = "com.kite.kmessenger"

val kotlinVersion= project.properties["kotlinVersion"]
repositories {
    mavenCentral()
}

micronaut {
    runtime("netty")
    testRuntime("junit5")
    processing {
        incremental(true)
        annotations("com.kite.kmessenger.*")
    }
}

dependencies {
    kapt("io.micronaut:micronaut-http-validation")
    implementation("io.micronaut:micronaut-http-client")
    implementation("io.micronaut:micronaut-runtime")
    implementation("io.micronaut.kotlin:micronaut-kotlin-extension-functions")
    implementation("io.micronaut.kotlin:micronaut-kotlin-runtime")
    runtimeOnly("io.micronaut.reactor:micronaut-reactor:2.0.0")
    implementation("io.projectreactor:reactor-core:3.4.12")


    implementation("javax.annotation:javax.annotation-api")
    implementation("org.apache.logging.log4j:log4j-core:2.17.0")
    implementation("org.jetbrains.kotlin:kotlin-reflect:${kotlinVersion}")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:${kotlinVersion}")
    runtimeOnly("org.apache.logging.log4j:log4j-api:2.17.0")
    runtimeOnly("org.apache.logging.log4j:log4j-slf4j-impl:2.17.0")
    implementation("io.micronaut:micronaut-validation")
    implementation("io.micronaut.cassandra:micronaut-cassandra:4.0.0")

    runtimeOnly("com.fasterxml.jackson.module:jackson-module-kotlin")
    testImplementation("org.awaitility:awaitility-kotlin:4.1.1")
    testImplementation(group = "org.apache.logging.log4j", name = "log4j-core", version = "2.17.0", classifier = "tests")
    testImplementation("org.testcontainers:testcontainers:1.16.3")
    testImplementation("org.testcontainers:junit-jupiter:1.16.3")

    testImplementation("org.assertj:assertj-core:3.22.0")


}


application {
    mainClass.set("com.kite.kmessenger.ApplicationKt")
}
java {
    sourceCompatibility = JavaVersion.toVersion("11")
}

tasks {
    compileKotlin {
        kotlinOptions {
            jvmTarget = "11"
        }
    }
    compileTestKotlin {
        kotlinOptions {
            jvmTarget = "11"
        }
    }


}
