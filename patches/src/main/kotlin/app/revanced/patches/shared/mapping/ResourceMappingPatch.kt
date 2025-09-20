package app.revanced.patches.shared.mapping

import app.revanced.patcher.patch.resourcePatch
import org.w3c.dom.Element

data class ResourceElement(val type: String, val name: String, val id: Long)

private lateinit var resourceMappings: MutableMap<String, ResourceElement>

private fun setResourceId(type: String, name: String, id: Long) {
    resourceMappings[type + name] = ResourceElement(type, name, id)
}

fun getResourceId(resourceType: ResourceType, name: String) =
    getResourceId(resourceType.value, name)

/**
 * @return A resource id of the given resource type and name.
 */
fun getResourceId(type: String, name: String) = resourceMappings[type + name]?.id
    ?: -1L

val resourceMappingPatch = resourcePatch(
    description = "resourceMappingPatch"
) {
    execute {
        document(get("res/values/public.xml").inputStream()).use { document ->
            val resources = document.documentElement.childNodes
            val resourcesLength = resources.length
            resourceMappings = HashMap<String, ResourceElement>(2 * resourcesLength)

            for (i in 0 until resourcesLength) {
                val node = resources.item(i) as? Element ?: continue
                if (node.nodeName != "public") continue

                val nameAttribute = node.getAttribute("name")
                if (nameAttribute.startsWith("APKTOOL")) continue

                val typeAttribute = node.getAttribute("type")
                val id = node.getAttribute("id").substring(2).toLong(16)

                setResourceId(typeAttribute, nameAttribute, id)
            }
        }
    }
}

enum class ResourceType(val value: String) {
    ATTR("attr"),
    BOOL("bool"),
    COLOR("color"),
    DIMEN("dimen"),
    DRAWABLE("drawable"),
    ID("id"),
    INTEGER("integer"),
    LAYOUT("layout"),
    RAW("raw"),
    STRING("string"),
    STYLE("style"),
    XML("xml")
}
