package eu.darkcube.system.minecraftutils.plugin.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.*
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import javax.inject.Inject

abstract class UnzipClientAssets @Inject constructor(objects: ObjectFactory, private val files: FileSystemOperations, layout: ProjectLayout, providerFactory: ProviderFactory, private val archiveOperations: ArchiveOperations) : DefaultTask() {
    @InputFile
    val minecraftClientJar: RegularFileProperty = objects.fileProperty()

    @OutputDirectory
    val destinationDirectory: DirectoryProperty = objects.directoryProperty()

    init {
        destinationDirectory.convention(layout.dir(providerFactory.provider {
            temporaryDir.resolve("assets")
        }))
    }

    @TaskAction
    fun run() {
        files.copy {
            from(archiveOperations.zipTree(minecraftClientJar))
            include("assets/**")
            eachFile {
                relativePath = RelativePath(true, *relativePath.segments.drop(1).toTypedArray())
            }
            into(destinationDirectory)
        }
    }
}