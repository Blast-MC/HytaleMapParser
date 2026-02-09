plugins {
    id("java")
    id("com.gradleup.shadow") version "8.3.0"
}

group = "tech.blastmc.hytale"
version = "0.1.0"
val javaVersion = 25

repositories {
    mavenCentral()
}

dependencies {
    compileOnly(files("libs/HytaleServer.jar"))
    implementation("io.github.classgraph:classgraph:4.8.171")
    implementation("org.apache.commons:commons-compress:1.26.1")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(javaVersion)
    }

    withSourcesJar()
}

tasks {
    jar {
        archiveClassifier.set("plain")
    }

    assemble {
        dependsOn(shadowJar)
    }

    shadowJar {
        archiveClassifier.set("")
    }
}

tasks.named<ProcessResources>("processResources") {
    var replaceProperties = mapOf(
        "plugin_group" to findProperty("plugin_group"),
        "plugin_maven_group" to project.group,
        "plugin_name" to project.name,
        "plugin_version" to project.version,
        "server_version" to findProperty("server_version"),

        "plugin_description" to findProperty("plugin_description"),
        "plugin_website" to findProperty("plugin_website"),

        "plugin_main_entrypoint" to findProperty("plugin_main_entrypoint"),
        "plugin_author" to findProperty("plugin_author")
    )

    filesMatching("manifest.json") {
        expand(replaceProperties)
    }

    inputs.properties(replaceProperties)
}

tasks.withType<Jar> {
    manifest {
        attributes["Specification-Title"] = rootProject.name
        attributes["Specification-Version"] = version
        attributes["Implementation-Title"] = project.name
        attributes["Implementation-Version"] =
            providers.environmentVariable("COMMIT_SHA_SHORT")
                .map { "${version}-${it}" }
                .getOrElse(version.toString())
    }
}

subprojects {
    apply { plugin("java") }

    dependencies {
        compileOnly(project(":"))
    }
}