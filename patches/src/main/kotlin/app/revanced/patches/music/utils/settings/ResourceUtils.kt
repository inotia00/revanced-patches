package app.revanced.patches.music.utils.settings

import app.revanced.patcher.patch.ResourcePatchContext
import app.revanced.patches.music.utils.compatibility.Constants.YOUTUBE_MUSIC_PACKAGE_NAME
import app.revanced.patches.music.utils.patch.PatchList
import app.revanced.util.adoptChild
import app.revanced.util.cloneNodes
import app.revanced.util.doRecursively
import app.revanced.util.findElementByAttributeValueOrThrow
import app.revanced.util.insertNode
import org.w3c.dom.Element

internal object ResourceUtils {
    private lateinit var context: ResourcePatchContext

    fun setContext(context: ResourcePatchContext) {
        this.context = context
    }

    private const val RVX_SETTINGS_KEY = "revanced_settings"

    const val SETTINGS_HEADER_PATH = "res/xml/settings_headers.xml"

    const val PREFERENCE_SCREEN_TAG_NAME =
        "PreferenceScreen"

    const val PREFERENCE_CATEGORY_TAG_NAME =
        "com.google.android.apps.youtube.music.ui.preference.PreferenceCategoryCompat"

    const val SWITCH_PREFERENCE_TAG_NAME =
        "com.google.android.apps.youtube.music.ui.preference.SwitchCompatPreference"

    const val ACTIVITY_HOOK_TARGET_CLASS =
        "com.google.android.gms.common.api.GoogleApiActivity"

    var gmsCorePackageName = "app.revanced.android.gms"
    var musicPackageName = YOUTUBE_MUSIC_PACKAGE_NAME

    private var iconType = "default"
    fun getIconType() = iconType

    fun setIconType(iconName: String) {
        iconType = iconName
    }

    private fun isIncludedCategory(category: String): Boolean {
        CategoryType.entries.forEach { preference ->
            if (category == preference.value)
                return preference.added
        }
        return false
    }

    private fun replacePackageName() = context.apply {
        val xmlFile = get(SETTINGS_HEADER_PATH)
        xmlFile.writeText(
            xmlFile.readText()
                .replace(
                    "\"com.google.android.apps.youtube.music\"",
                    "\"" + musicPackageName + "\""
                )
        )
    }


    private fun setPreferenceCategory(newCategory: String) {
        CategoryType.entries.forEach { preference ->
            if (newCategory == preference.value)
                preference.added = true
        }
    }

    fun updatePackageName(
        newGmsCorePackage: String,
        newMusicPackage: String,
    ) {
        gmsCorePackageName = newGmsCorePackage
        musicPackageName = newMusicPackage
        replacePackageName()
    }

    fun updatePatchStatus(patch: PatchList) {
        patch.included = true
    }

    fun addPreferenceCategory(category: String) {
        context.document(SETTINGS_HEADER_PATH).use { document ->
            val tags = document.getElementsByTagName(PREFERENCE_SCREEN_TAG_NAME)
            List(tags.length) { tags.item(it) as Element }
                .filter { it.getAttribute("android:key").contains(RVX_SETTINGS_KEY) }
                .forEach {
                    if (!isIncludedCategory(category)) {
                        it.adoptChild(PREFERENCE_SCREEN_TAG_NAME) {
                            setAttribute(
                                "android:title",
                                "@string/revanced_preference_screen_$category" + "_title"
                            )
                            setAttribute("android:key", "revanced_preference_screen_$category")
                        }
                        setPreferenceCategory(category)
                    }
                }
        }
    }

    fun addPreferenceCategoryUnderPreferenceScreen(
        preferenceScreenKey: String,
        category: String
    ) {
        context.document(SETTINGS_HEADER_PATH).use { document ->
            val tags = document.getElementsByTagName(PREFERENCE_SCREEN_TAG_NAME)
            List(tags.length) { tags.item(it) as Element }
                .filter { it.getAttribute("android:key").contains(preferenceScreenKey) }
                .forEach {
                    it.adoptChild(PREFERENCE_CATEGORY_TAG_NAME) {
                        setAttribute("android:title", "@string/$category")
                        setAttribute("android:key", category)
                    }
                }
        }
    }

    fun sortPreferenceCategory(
        category: String
    ) {
        context.document(SETTINGS_HEADER_PATH).use { document ->
            document.doRecursively node@{
                if (it !is Element) return@node

                it.getAttributeNode("android:key")?.let { attribute ->
                    if (attribute.textContent == "revanced_preference_screen_$category") {
                        it.cloneNodes(it.parentNode)
                    }
                }
            }
        }
        replacePackageName()
    }

    fun addGmsCorePreference(
        category: String,
        key: String,
        packageName: String,
        targetClassName: String
    ) {
        context.document(SETTINGS_HEADER_PATH).use { document ->
            val tags = document.getElementsByTagName(PREFERENCE_SCREEN_TAG_NAME)
            List(tags.length) { tags.item(it) as Element }
                .filter {
                    it.getAttribute("android:key").contains("revanced_preference_screen_$category")
                }
                .forEach {
                    it.adoptChild("Preference") {
                        setAttribute("android:title", "@string/$key" + "_title")
                        setAttribute("android:summary", "@string/$key" + "_summary")
                        this.adoptChild("intent") {
                            setAttribute("android:targetPackage", packageName)
                            setAttribute("android:data", key)
                            setAttribute("android:targetClass", targetClassName)
                        }
                    }
                }
        }
    }

    fun addPreferenceFragment(
        key: String,
        insertKey: String,
        targetClass: String,
    ) = context.apply {
        document(SETTINGS_HEADER_PATH).use { document ->
            with(document) {
                val processedKeys = mutableSetOf<String>() // To track processed keys

                doRecursively loop@{ node ->
                    if (node !is Element) return@loop // Skip if not an element

                    val attributeNode = node.getAttributeNode("android:key")
                        ?: return@loop // Skip if no key attribute
                    val currentKey = attributeNode.textContent

                    // Check if the current key has already been processed
                    if (processedKeys.contains(currentKey)) {
                        return@loop // Skip if already processed
                    } else {
                        processedKeys.add(currentKey) // Add the current key to processedKeys
                    }

                    when (currentKey) {
                        insertKey -> {
                            node.insertNode("Preference", node) {
                                setAttribute("android:key", "${key}_key")
                                setAttribute("android:title", "@string/${key}_title")
                                this.appendChild(
                                    ownerDocument.createElement("intent").also { intentNode ->
                                        intentNode.setAttribute(
                                            "android:targetPackage",
                                            "com.google.android.apps.youtube.music"
                                        )
                                        intentNode.setAttribute("android:data", key + "_intent")
                                        intentNode.setAttribute("android:targetClass", targetClass)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Modify the manifest to enhance TargetActivity behavior:
        // 1. Add a data intent filter with MIME type "text/plain".
        //    Some devices crash if undeclared data is passed to an intent,
        //    and this change appears to fix the issue.
        // 2. Add android:configChanges="orientation|screenSize|keyboardHidden".
        //    This prevents the activity from being recreated on configuration changes
        //    (e.g., screen rotation), preserving its current state and fragment.
        document("AndroidManifest.xml").use { document ->
            val activityElement = document.childNodes.findElementByAttributeValueOrThrow(
                "android:name",
                targetClass,
            )

            if (!activityElement.hasAttribute("android:configChanges")) {
                activityElement.setAttribute(
                    "android:configChanges",
                    "keyboardHidden|orientation|screenSize"
                )
            }

            val mimeType = document.createElement("data")
            mimeType.setAttribute("android:mimeType", "text/plain")

            val intentFilter = document.createElement("intent-filter")
            intentFilter.appendChild(mimeType)

            activityElement.appendChild(intentFilter)
        }
    }

    fun addSwitchPreference(
        category: String,
        key: String,
        defaultValue: String,
        dependencyKey: String,
        setSummary: Boolean
    ) {
        context.document(SETTINGS_HEADER_PATH).use { document ->
            val tags = document.getElementsByTagName(PREFERENCE_SCREEN_TAG_NAME)
            List(tags.length) { tags.item(it) as Element }
                .filter {
                    it.getAttribute("android:key").contains("revanced_preference_screen_$category")
                }
                .forEach {
                    it.adoptChild(SWITCH_PREFERENCE_TAG_NAME) {
                        setAttribute("android:title", "@string/$key" + "_title")
                        if (setSummary) {
                            val resources = context.get("res/values/strings.xml").readText()
                            val hasOn = resources.contains("name=\"${key}_summary_on\"")
                            val hasOff = resources.contains("name=\"${key}_summary_off\"")
                            when {
                                hasOn && hasOff -> {
                                    setAttribute("android:summaryOn", "@string/${key}_summary_on")
                                    setAttribute("android:summaryOff", "@string/${key}_summary_off")
                                }
                                resources.contains("name=\"${key}_summary\"") -> {
                                    setAttribute("android:summaryOn", "@string/${key}_summary")
                                    setAttribute("android:summaryOff", "@string/${key}_summary")
                                }
                            }
                        }
                        setAttribute("android:key", key)
                        setAttribute("android:defaultValue", defaultValue)
                        if (dependencyKey != "") {
                            setAttribute("android:dependency", dependencyKey)
                        }
                    }
                }
        }
    }

    fun addPreferenceWithIntent(
        category: String,
        key: String,
        dependencyKey: String
    ) {
        context.document(SETTINGS_HEADER_PATH).use { document ->
            val tags = document.getElementsByTagName(PREFERENCE_SCREEN_TAG_NAME)
            List(tags.length) { tags.item(it) as Element }
                .filter {
                    it.getAttribute("android:key").contains("revanced_preference_screen_$category")
                }
                .forEach {
                    it.adoptChild("Preference") {
                        setAttribute("android:title", "@string/$key" + "_title")
                        setAttribute("android:summary", "@string/$key" + "_summary")
                        setAttribute("android:key", key)
                        if (dependencyKey.isNotEmpty()) {
                            setAttribute("android:dependency", dependencyKey)
                        }
                        this.adoptChild("intent") {
                            setAttribute("android:targetPackage", musicPackageName)
                            setAttribute("android:data", key)
                            setAttribute("android:targetClass", ACTIVITY_HOOK_TARGET_CLASS)
                        }
                    }
                }
        }
    }

    fun addRVXSettingsPreference(insertKey: String) {
        context.document(SETTINGS_HEADER_PATH).use { document ->
            document.doRecursively node@{
                if (it !is Element) return@node

                it.getAttributeNode("android:key")?.let { attribute ->
                    if (attribute.textContent == insertKey && it.getAttributeNode(
                            "app:allowDividerBelow"
                        ).textContent == "false"
                    ) {
                        it.insertNode(PREFERENCE_SCREEN_TAG_NAME, it) {
                            setAttribute(
                                "android:title",
                                "@string/revanced_settings_title"
                            )
                            setAttribute("android:key", "revanced_settings")
                            setAttribute("app:allowDividerAbove", "false")
                        }
                        it.getAttributeNode("app:allowDividerBelow").textContent = "true"
                        return@node
                    }
                }
            }

            document.doRecursively node@{
                if (it !is Element) return@node

                it.getAttributeNode("app:allowDividerBelow")?.let { attribute ->
                    if (attribute.textContent == "true") {
                        attribute.textContent = "false"
                    }
                }
            }
        }
    }
}