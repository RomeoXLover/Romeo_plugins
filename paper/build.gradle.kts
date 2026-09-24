plugins {
    `java-library`
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(8)
}

dependencies {
    implementation(project(":common"))
    compileOnly("org.spigotmc:spigot-api:1.8.8-R0.1-SNAPSHOT")
    implementation("net.kyori:adventure-api:4.17.0")
    implementation("net.kyori:adventure-text-serializer-legacy:4.17.0")
}

tasks.jar {
    archiveBaseName.set("romeo-paper")
    archiveVersion.set(project.version.toString())
}

tasks.shadowJar {
    archiveClassifier.set("")
    archiveVersion.set(project.version.toString())
    mergeServiceFiles()
}

tasks.build {
    dependsOn(tasks.shadowJar)
}
