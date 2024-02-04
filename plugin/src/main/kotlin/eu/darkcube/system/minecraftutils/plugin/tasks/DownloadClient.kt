package eu.darkcube.system.minecraftutils.plugin.tasks

import eu.darkcube.system.minecraftutils.plugin.VersionManifest
import org.gradle.api.DefaultTask
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.property
import org.slf4j.LoggerFactory
import java.net.URI
import javax.inject.Inject

abstract class DownloadClient @Inject constructor(objects: ObjectFactory, layout: ProjectLayout, providerFactory: ProviderFactory) : DefaultTask() {
    private val logger = LoggerFactory.getLogger("DownloadClient")

    @InputFile
    val versionManifestFile: RegularFileProperty = objects.fileProperty()

    @Input
    val wantedVersion: Property<String> = objects.property()

    @OutputFile
    val clientJar: RegularFileProperty = objects.fileProperty()

    init {
        wantedVersion.convention("latest")
        wantedVersion.finalizeValueOnRead()
        clientJar.convention(layout.file(providerFactory.provider { temporaryDir.resolve("client.jar") }))
        clientJar.finalizeValueOnRead()
    }

    @TaskAction
    fun run() {
        val manifests = VersionManifest.parse(versionManifestFile.get().asFile.bufferedReader().use { it.readText() })
        val unresolved = manifests[wantedVersion.get()]
        if (unresolved == null) {
            logger.error("Unknown version: " + wantedVersion.get())
            return
        }
        val manifest = unresolved.resolve().join()
        val url = URI.create(manifest.clientUrl()).toURL()
        logger.info("Downloading client from $url")
        val connection = url.openConnection()
        connection.getInputStream().use { inputStream ->
            clientJar.get().asFile.outputStream().use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }
    }
}