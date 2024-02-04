import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.plugin.use.resolve.internal.ArtifactRepositoriesPluginResolver

plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    `maven-publish`
    alias(libs.plugins.shadow)
}

group = "eu.darkcube.system"
version = properties["version"].toString()

kotlin {
    jvmToolchain(8)
}

repositories {
    gradlePluginPortal()
}

val embed: Configuration by configurations.creating {
    isCanBeConsumed = false
}
configurations.implementation.configure {
    extendsFrom(embed)
}

dependencies {
    embed(libs.gson)
    embed(rootProject.project("data"))
}

tasks {
    named<ShadowJar>("shadowJar") {
        isEnableRelocation = true
        relocationPrefix = "eu.darkcube.system.minecraftutils.relocate"
        configurations = listOf(embed)
        minimize()
    }
}

publishing {
    publications {
        register<MavenPublication>("plugin") {
            artifactId = "minecraft-utils-plugin"
            shadow.component(this)
        }
        register<MavenPublication>("pluginMarker") {
            val projectVersion = version.toString()
            val projectGroup = group.toString()
            val pluginId = "minecraft-utils"
            artifactId = pluginId + ArtifactRepositoriesPluginResolver.PLUGIN_MARKER_SUFFIX
            groupId = pluginId
            pom.withXml {
                val root = asElement()
                val document = root.ownerDocument
                val dependencies = root.appendChild(document.createElement("dependencies"))
                val dependency = dependencies.appendChild(document.createElement("dependency"))
                dependency.appendChild(document.createElement("groupId")).textContent = projectGroup
                dependency.appendChild(document.createElement("artifactId")).textContent = "minecraft-utils-plugin"
                dependency.appendChild(document.createElement("version")).textContent = projectVersion
            }
        }
    }
}

gradlePlugin {
    isAutomatedPublishing = false
    plugins {
        register("MinecraftUtils") {
            id = "minecraft-utils"
            implementationClass = "eu.darkcube.system.minecraftutils.plugin.MinecraftUtilsPlugin"
        }
    }
}