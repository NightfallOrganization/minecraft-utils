package eu.darkcube.system.minecraftutils.plugin.tasks

import eu.darkcube.system.minecraftutils.util.GlyphsData
import eu.darkcube.system.minecraftutils.plugin.util.FontData
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.setProperty
import java.io.InputStream
import java.net.URI
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes
import java.util.zip.ZipInputStream
import javax.inject.Inject
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.deleteRecursively
import kotlin.io.path.outputStream

abstract class GenerateGlyphWidthData @Inject constructor(objects: ObjectFactory) : DefaultTask() {
    @InputFiles
    val assetsDirectory: DirectoryProperty = objects.directoryProperty()

    @OutputFile
    val outputGlyphWidthsFile: RegularFileProperty = objects.fileProperty()

    @Input
    val resourcePacks: SetProperty<String> = objects.setProperty()

    init {
        outputGlyphWidthsFile.convention { temporaryDir.resolve("glyphWidths.bin") }
    }

    @OptIn(ExperimentalPathApi::class)
    @TaskAction
    fun run() {
        val assets = this.assetsDirectory.asFile.get().toPath()
        val fontData = FontData.load(assets)
        val mergedAssets = temporaryDir.resolve("mergedAssets").toPath()
        mergedAssets.deleteRecursively()
        copyFolder(assets, mergedAssets)
        val resourcePacksDirectory = temporaryDir.resolve("resourcePacks").toPath()

        for (resourcePackString in this.resourcePacks.get()) {
            val resourcePackUrl = try {
                URI.create(resourcePackString).toURL()
            } catch (_: Throwable) {
                URI.create("file:///$resourcePackString").toURL()
            }
            val unzipped = resourcePackUrl.openConnection().getInputStream().use {
                val resourcePackUnzipped = resourcePacksDirectory.resolve(resourcePackString.hashCode().toString())
                resourcePackUnzipped.deleteRecursively()
                unzip(resourcePackUnzipped, it)
            }
            val unzippedAssets = unzipped.resolve("assets")
            if (!Files.exists(unzippedAssets)) {
                continue
            }
            val data = FontData.load(unzippedAssets)
            fontData.providers.addAll(data.providers)
            copyFolder(unzippedAssets, mergedAssets, StandardCopyOption.REPLACE_EXISTING)
        }

        val outputPath = outputGlyphWidthsFile.asFile.get().toPath()
        Files.createDirectories(outputPath.parent)

        val glyphsData = GlyphsData()

        for (provider in fontData.providers) {
            if (provider.type() == "space") {
                FontData.Provider.space(provider, glyphsData.bitmapWidths, glyphsData.spaceWidths)
            } else if (provider.type() == "bitmap") {
                FontData.Provider.bitmap(mergedAssets, provider, glyphsData.spaceWidths, glyphsData.bitmapWidths)
            } else {
                logger.error("Unsupported provider: " + provider.type())
            }
        }

        outputPath.outputStream().buffered().use {
            glyphsData.save(it)
        }
    }

    private fun copyFolder(source: Path, target: Path, vararg options: CopyOption) {
        Files.walkFileTree(source, object : SimpleFileVisitor<Path>() {
            override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult {
                Files.createDirectories(target.resolve(source.relativize(dir).toString()))
                return FileVisitResult.CONTINUE
            }

            override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                Files.copy(file, target.resolve(source.relativize(file).toString()), *options)
                return FileVisitResult.CONTINUE
            }
        })
    }

    private fun unzip(into: Path, inputStream: InputStream): Path {
        val zip = ZipInputStream(inputStream.buffered(), Charsets.UTF_8)
        zip.use {
            while (true) {
                val entry = it.nextEntry ?: break
                if (!entry.name.startsWith("assets")) continue
                val path = into.resolve(entry.name)
                val parent = path.parent
                if (!entry.isDirectory) {
                    if (!Files.exists(parent)) Files.createDirectories(parent)
                    Files.copy(it, path)
                } else {
                    Files.createDirectories(path)
                }
            }
        }
        return into
    }
}