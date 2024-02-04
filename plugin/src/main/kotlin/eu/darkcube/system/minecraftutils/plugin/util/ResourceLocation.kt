package eu.darkcube.system.minecraftutils.plugin.util

data class ResourceLocation(val namespace: String, val path: String) {
    companion object {
        fun fromString(string: String): ResourceLocation {
            var a = string.split(":")
            if (a.size == 1) a = listOf("minecraft", a[0])
            return ResourceLocation(a[0], a[1])
        }
    }
}
