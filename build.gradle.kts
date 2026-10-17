plugins {
    kotlin("jvm") version "2.1.0"
}

group = "com.KDI"
version = "12A-enchant.v10"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")
}

kotlin {
    jvmToolchain(25)
}