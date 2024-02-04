package eu.darkcube.system.minecraftutils.plugin.util

import com.google.gson.Gson
import com.google.gson.JsonObject
import java.awt.image.BufferedImage
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import javax.imageio.ImageIO
import kotlin.io.path.bufferedReader

class FontData {
    companion object {
        private val default: ResourceLocation = ResourceLocation("minecraft", "default")

        @Throws(IOException::class)
        fun load(assets: Path): FontData {
            val gson = Gson()
            val fontData = FontData()
            append(gson, assets, default, fontData)
            return fontData
        }

        @Throws(IOException::class)
        private fun append(gson: Gson, assets: Path, path: ResourceLocation, fontData: FontData) {
            val data = assets.resolve(path.namespace).resolve("font").resolve(path.path + ".json").bufferedReader().use { it.readText() }
            val json = gson.fromJson(data, JsonObject::class.java)
            val providers = json["providers"].getAsJsonArray()
            for (element in providers) {
                val providerJson = element.getAsJsonObject()
                val provider = Provider(providerJson)
                if (provider.type() == "reference") {
                    val id = provider.string("id")
                    val loc: ResourceLocation = ResourceLocation.fromString(id)
                    append(gson, assets, loc, fontData)
                } else {
                    fontData.providers.add(provider)
                }
            }
        }
    }

    val providers: MutableList<Provider> = ArrayList()


    override fun toString(): String {
        return "FontData{providers=$providers}"
    }

    data class Provider(val json: JsonObject) {
        fun type(): String {
            return string("type")
        }

        fun string(key: String?): String {
            return json[key].asString
        }

        override fun toString(): String {
            return json.toString()
        }

        companion object {
            fun space(provider: Provider, removeKeys: MutableMap<Int, Float>, widths: MutableMap<Int, Float>) {
                val advances = provider.json["advances"].getAsJsonObject()
                for (key in advances.keySet()) {
                    val advance = advances[key].asFloat
                    val cp = key.codePointAt(0)
                    removeKeys.remove(cp)
                    widths[cp] = advance
                }
            }

            @Throws(IOException::class)
            fun bitmap(assets: Path, provider: Provider, removeKeys: MutableMap<Int, Float>, widths: MutableMap<Int, Float>) {
                val loc: ResourceLocation = ResourceLocation.fromString(provider.string("file"))
                val path: Path = assets.resolve(loc.namespace).resolve("textures").resolve(loc.path)
                val inputStream = Files.newInputStream(path)
                val image = ImageIO.read(inputStream)
                inputStream.close()
                var displayHeight = 8
                if (provider.json.has("height")) displayHeight = provider.json["height"].asInt
                val charsArray = provider.json["chars"].getAsJsonArray()
                val codepointMap = arrayOfNulls<IntArray>(charsArray.size())
                var y = 0
                for (element in charsArray) {
                    val characters = element.asString
                    codepointMap[y++] = characters.codePoints().toArray()
                }
                val bitmapWidth = image.width
                val bitmapHeight = image.height
                val glyphWidth = bitmapWidth / codepointMap[0]!!.size
                val glyphHeight = bitmapHeight / codepointMap.size
                val scale = displayHeight / glyphHeight.toFloat()
                y = 0
                while (y < codepointMap.size) {
                    for (x in codepointMap[y]!!.indices) {
                        val actualWidth = actualWidth(image, glyphWidth, glyphHeight, x, y)
                        val advance = (0.5 + (actualWidth.toFloat() * scale).toDouble()).toInt() + 1
                        if (codepointMap[y]!![x] == 32) {
                            if (removeKeys.containsKey(32)) continue
                        }
                        widths[codepointMap[y]!![x]] = advance.toFloat()
                        removeKeys.remove(codepointMap[y]!![x])
                    }
                    y++
                }
            }

            private fun actualWidth(image: BufferedImage, glyphWidth: Int, glyphHeight: Int, xIndex: Int, yIndex: Int): Int {
                var i: Int = glyphWidth - 1
                while (i >= 0) {
                    val j = xIndex * glyphWidth + i
                    for (k in 0 until glyphHeight) {
                        val l = yIndex * glyphHeight + k
                        val pixel = image.getRGB(j, l)
                        val alpha = pixel shr 24 and 0xff
                        if (alpha != 0) {
                            return i + 1
                        }
                    }
                    --i
                }
                return i + 1
            }
        }
    }

}