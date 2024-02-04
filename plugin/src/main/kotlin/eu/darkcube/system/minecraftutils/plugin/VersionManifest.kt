package eu.darkcube.system.minecraftutils.plugin

import com.google.gson.Gson
import com.google.gson.JsonObject
import java.net.URI
import java.util.concurrent.CompletableFuture

open class VersionManifest(val id: String, val url: String) {
    companion object {
        fun parse(data: String): Map<String, VersionManifest> {
            val manifests = HashMap<String, VersionManifest>()
            val json = Gson().fromJson(data, JsonObject::class.java)
            val arrayVersions = json.getAsJsonArray("versions")
            for (element in arrayVersions) {
                val o = element.asJsonObject
                val id = o.get("id").asString
                val versionUrl = o.get("url").asString
                manifests[id] = VersionManifest(id, versionUrl)
            }
            val objectLatest = json.getAsJsonObject("latest")
            val latestSnapshot = objectLatest.get("snapshot").asString
            val latestRelease = objectLatest.get("release").asString

            manifests[latestRelease]?.let {
                manifests["latest"] = it
                manifests["latestRelease"] = it
            }
            manifests[latestSnapshot]?.let {
                manifests["latestSnapshot"] = it
            }
            return manifests
        }
    }

    fun resolve(): CompletableFuture<Resolved> {
        return CompletableFuture.supplyAsync {
            val uri = URI.create(this.url)
            val url = uri.toURL()
            val connection = url.openConnection()
            val inputStream = connection.getInputStream()
            val data = inputStream.bufferedReader().use { it.readText() }
            val json = Gson().fromJson(data, JsonObject::class.java)
            Resolved(this.id, this.url, json)
        }
    }

    class Resolved(id: String, url: String, val json: JsonObject) : VersionManifest(id, url) {
        fun clientUrl(): String {
            return json.getAsJsonObject("downloads").getAsJsonObject("client").get("url").asString
        }
    }
}