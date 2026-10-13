 plugins {
    id("java-library")
    id("com.gradleup.shadow") version "9.3.1"
    id("run-hytale")
    kotlin("jvm") version "2.4.10"
}

group = findProperty("pluginGroup") as String? ?: "it.cava99.towny3d"
version = findProperty("pluginVersion") as String? ?: "1.0.0"
description = findProperty("pluginDescription") as String? ?: "Towny3D for Hytale - 3D land claim plugin with spatial indexing"

repositories {
    mavenLocal()
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
    maven("https://repo.codemc.io/repository/creatorfromhell/") {
        name = "VaultUnlocked"
    }
    maven {
        name = "hytale-pre-release"
        url = uri("https://maven.hytale.com/pre-release")
    }
}

dependencies {
    // Hytale Server API (provided by server at runtime)
    compileOnly("com.hypixel.hytale:Server:+")

    // Cassaforte API (service registry for economy/permissions/chat)
    compileOnly("com.github.Filocava99:Cassaforte:v0.1.2")

    // VaultUnlocked API (alternative economy plugin support)
    compileOnly("net.cfh.vault:VaultUnlocked:2.18.3")

    // Kotlin stdlib (will be shadowed into JAR)
    implementation(kotlin("stdlib"))
    implementation(kotlin("reflect"))

    // Common dependencies (will be bundled in JAR)
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("org.jetbrains:annotations:24.1.0")

    // Caffeine Cache
    implementation("com.github.ben-manes.caffeine:caffeine:3.1.8")

    // Kotlin Coroutines (will be shadowed into JAR)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.8.0")

    // Test dependencies
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.0")
    testImplementation("io.mockk:mockk:1.13.9")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

// Configure server testing
runHytale {
    // TODO: Update this URL when Hytale server is available
    // Using Paper server as placeholder for testing the runServer functionality
    jarUrl = "https://fill-data.papermc.io/v1/objects/d5f47f6393aa647759f101f02231fa8200e5bccd36081a3ee8b6a5fd96739057/paper-1.21.10-115.jar"
    
    // Configure local server arguments
    assetsZip = "Assets.zip"
    bindAddress = "0.0.0.0:25565"
    modsDirectory = "./build/libs"
}

tasks {
    // Configure Java compilation
    compileJava {
        options.encoding = Charsets.UTF_8.name()
        options.release = 25
    }

    // Configure Kotlin compilation to match Java target
    compileKotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_25)
        }
    }
    
    // Configure resource processing
    processResources {
        filteringCharset = Charsets.UTF_8.name()
        
        // Replace placeholders in manifest.json
        val props = mapOf(
            "group" to project.group,
            "version" to project.version,
            "description" to project.description
        )
        inputs.properties(props)
        
        filesMatching("manifest.json") {
            expand(props)
        }
    }
    
    // Configure ShadowJar (bundle dependencies)
    shadowJar {
        archiveBaseName.set(rootProject.name)
        archiveClassifier.set("")

        // Relocate dependencies to avoid conflicts
        relocate("com.google.gson", "it.cava99.towny3d.libs.gson")
        relocate("com.github.benmanes.caffeine", "it.cava99.towny3d.libs.caffeine")

        // Relocate Kotlin standard library and runtime
        relocate("kotlin", "it.cava99.towny3d.libs.kotlin")
        relocate("kotlinx", "it.cava99.towny3d.libs.kotlinx")
        relocate("org.jetbrains", "it.cava99.towny3d.libs.jetbrains")

        // Exclude unnecessary metadata to reduce JAR size
        exclude("META-INF/*.kotlin_module")
        exclude("META-INF/LICENSE")
        exclude("META-INF/NOTICE")
        exclude("META-INF/INDEX.LIST")
        exclude("META-INF/io.netty.versions.properties")
        exclude("**/*.proto")
        exclude("**/module-info.class")

        // Don't minimize - Caffeine uses reflection for internal classes (SSMSAR, etc.)
        // and minimization will remove these classes causing ClassNotFoundException
        // minimize()
    }
    
    // Configure tests
    test {
        useJUnitPlatform()
    }
    
    // Make build depend on shadowJar
    build {
        dependsOn(shadowJar)
    }
}

// Configure Java toolchain
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}
