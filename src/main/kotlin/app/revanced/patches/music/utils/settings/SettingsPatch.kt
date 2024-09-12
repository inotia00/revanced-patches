package app.revanced.patches.music.utils.settings

import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.options.PatchOption.PatchExtensions.stringPatchOption
import app.revanced.patches.music.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.music.utils.fix.accessibility.AccessibilityNodeInfoPatch
import app.revanced.patches.music.utils.settings.ResourceUtils.addPreferenceCategory
import app.revanced.patches.music.utils.settings.ResourceUtils.addPreferenceWithIntent
import app.revanced.patches.music.utils.settings.ResourceUtils.addRVXSettingsPreference
import app.revanced.patches.music.utils.settings.ResourceUtils.addSwitchPreference
import app.revanced.patches.music.utils.settings.ResourceUtils.sortPreferenceCategory
import app.revanced.patches.shared.elements.StringsElementsUtils.removeStringsElements
import app.revanced.util.copyXmlNode
import app.revanced.util.patch.BaseResourcePatch
import app.revanced.util.valueOrThrow
import org.w3c.dom.Element
import java.io.Closeable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@Suppress("DEPRECATION", "unused")
object SettingsPatch : BaseResourcePatch(
    name = "Settings for YouTube Music",
    description = "Applies mandatory patches to implement ReVanced Extended settings into the application.",
    dependencies = setOf(
        AccessibilityNodeInfoPatch::class,
        SettingsBytecodePatch::class
    ),
    compatiblePackages = COMPATIBLE_PACKAGE,
    requiresIntegrations = true
), Closeable {
    private const val DEFAULT_NAME = "ReVanced Extended"

    private val RVXSettingsMenuName = stringPatchOption(
        key = "RVXSettingsMenuName",
        default = DEFAULT_NAME,
        title = "RVX settings menu name",
        description = "The name of the RVX settings menu.",
        required = true
    )

    private lateinit var customName: String

    lateinit var contexts: ResourceContext
    internal var upward0636 = false
    internal var upward0642 = false

    override fun execute(context: ResourceContext) {

        /**
         * check patch options
         */
        customName = RVXSettingsMenuName
            .valueOrThrow()

        /**
         * set resource context
         */
        contexts = context

        /**
         * set version info
         */
        setVersionInfo()

        /**
         * copy strings
         */
        context.copyXmlNode("music/settings/host", "values/strings.xml", "resources")

        /**
         * hide divider
         */
        val styleFile = context["res/values/styles.xml"]

        styleFile.writeText(
            styleFile.readText()
                .replace(
                    "allowDividerAbove\">true",
                    "allowDividerAbove\">false"
                ).replace(
                    "allowDividerBelow\">true",
                    "allowDividerBelow\">false"
                )
        )

        /**
         * Copy arrays
         */
        contexts.copyXmlNode("music/settings/host", "values/arrays.xml", "resources")

        /**
         * Copy colors
         */
        context.xmlEditor["res/values/colors.xml"].use { editor ->
            val resourcesNode = editor.file.getElementsByTagName("resources").item(0) as Element

            for (i in 0 until resourcesNode.childNodes.length) {
                val node = resourcesNode.childNodes.item(i) as? Element ?: continue

                node.textContent = when (node.getAttribute("name")) {
                    "material_deep_teal_500" -> "@android:color/white"

                    else -> continue
                }
            }
        }

        context.addRVXSettingsPreference()
    }

    private fun setVersionInfo() {
        val threadCount = Runtime.getRuntime().availableProcessors()
        val threadPoolExecutor = Executors.newFixedThreadPool(threadCount)

        val resourceXmlFile = contexts["res/values/integers.xml"].readBytes()

        for (threadIndex in 0 until threadCount) {
            threadPoolExecutor.execute thread@{
                contexts.xmlEditor[resourceXmlFile.inputStream()].use { editor ->
                    val resources = editor.file.documentElement.childNodes
                    val resourcesLength = resources.length
                    val jobSize = resourcesLength / threadCount

                    val batchStart = jobSize * threadIndex
                    val batchEnd = jobSize * (threadIndex + 1)
                    element@ for (i in batchStart until batchEnd) {
                        if (i >= resourcesLength) return@thread

                        val node = resources.item(i)
                        if (node !is Element) continue

                        if (node.nodeName != "integer" || !node.getAttribute("name")
                                .startsWith("google_play_services_version")
                        ) continue

                        val playServicesVersion = node.textContent.toInt()

                        upward0636 = 240399000 <= playServicesVersion
                        upward0642 = 240999000 <= playServicesVersion

                        break
                    }
                }
            }
        }

        threadPoolExecutor
            .also { it.shutdown() }
            .awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS)
    }

    internal fun addSwitchPreference(
        category: CategoryType,
        key: String,
        defaultValue: String
    ) = addSwitchPreference(category, key, defaultValue, "")

    internal fun addSwitchPreference(
        category: CategoryType,
        key: String,
        defaultValue: String,
        setSummary: Boolean
    ) = addSwitchPreference(category, key, defaultValue, "", setSummary)

    internal fun addSwitchPreference(
        category: CategoryType,
        key: String,
        defaultValue: String,
        dependencyKey: String
    ) = addSwitchPreference(category, key, defaultValue, dependencyKey, true)

    internal fun addSwitchPreference(
        category: CategoryType,
        key: String,
        defaultValue: String,
        dependencyKey: String,
        setSummary: Boolean
    ) {
        val categoryValue = category.value
        contexts.addPreferenceCategory(categoryValue)
        contexts.addSwitchPreference(categoryValue, key, defaultValue, dependencyKey, setSummary)
    }

    internal fun addPreferenceWithIntent(
        category: CategoryType,
        key: String
    ) = addPreferenceWithIntent(category, key, "")

    internal fun addPreferenceWithIntent(
        category: CategoryType,
        key: String,
        dependencyKey: String
    ) {
        val categoryValue = category.value
        contexts.addPreferenceCategory(categoryValue)
        contexts.addPreferenceWithIntent(categoryValue, key, dependencyKey)
    }

    override fun close() {
        /**
         * change RVX settings menu name
         * since it must be invoked after the Translations patch, it must be the last in the order.
         */
        if (customName != DEFAULT_NAME) {
            contexts.removeStringsElements(
                arrayOf("revanced_extended_settings_title")
            )
            contexts.xmlEditor["res/values/strings.xml"].use { editor ->
                val document = editor.file

                mapOf(
                    "revanced_extended_settings_title" to customName
                ).forEach { (k, v) ->
                    val stringElement = document.createElement("string")

                    stringElement.setAttribute("name", k)
                    stringElement.textContent = v

                    document.getElementsByTagName("resources").item(0)
                        .appendChild(stringElement)
                }
            }
        }

        /**
         * add import export settings
         */
        addPreferenceWithIntent(
            CategoryType.MISC,
            "revanced_extended_settings_import_export"
        )

        /**
         * sort preference
         */
        CategoryType.entries.sorted().forEach {
            contexts.sortPreferenceCategory(it.value)
        }
    }
}
