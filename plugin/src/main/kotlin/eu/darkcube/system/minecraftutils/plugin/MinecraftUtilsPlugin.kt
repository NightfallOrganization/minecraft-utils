package eu.darkcube.system.minecraftutils.plugin

import eu.darkcube.system.minecraftutils.plugin.tasks.DownloadClient
import eu.darkcube.system.minecraftutils.plugin.tasks.DownloadManifest
import eu.darkcube.system.minecraftutils.plugin.tasks.UnzipClientAssets
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.register

class MinecraftUtilsPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val downloadMinecraftManifest = project.tasks.register<DownloadManifest>("downloadMinecraftManifest") {
            outputs.upToDateWhen { project.gradle.startParameter.isOffline }
        }
        val downloadMinecraftClientJar = project.tasks.register<DownloadClient>("downloadMinecraftClientJar") {
            dependsOn(downloadMinecraftManifest)
            versionManifestFile.convention(downloadMinecraftManifest.get().versionManifestFile)
        }
        project.tasks.register<UnzipClientAssets>("unzipMinecraftClientAssets") {
            minecraftClientJar.set(downloadMinecraftClientJar.flatMap { it.clientJar })
        }
    }
}