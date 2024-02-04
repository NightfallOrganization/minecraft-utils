package eu.darkcube.system.minecraftutils.plugin.tasks

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.net.URI
import javax.inject.Inject

abstract class DownloadManifest @Inject constructor(objects: ObjectFactory) : DefaultTask() {
    companion object {
        const val MANIFEST_URL = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json"
    }

    @OutputFile
    val versionManifestFile: RegularFileProperty = objects.fileProperty()

    init {
        this.versionManifestFile.convention { temporaryDir.resolve("version_manifest_v2.json") }
        this.outputs.upToDateWhen { false }
    }

    @TaskAction
    fun run() {
        val uri = URI.create(MANIFEST_URL)
        val url = uri.toURL()
        val connection = url.openConnection()
        val file = versionManifestFile.get()

        val json =
            Gson().fromJson(connection.getInputStream().bufferedReader().use { it.readText() }, JsonObject::class.java)
        val data = GsonBuilder().setPrettyPrinting().create().toJson(json)
        file.asFile.outputStream().bufferedWriter().use {
            it.write(data)
        }
    }
}