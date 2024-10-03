package app.revanced.patches.youtube.utils.settings

import app.revanced.patcher.data.ResourceContext
import app.revanced.util.doRecursively
import app.revanced.util.insertNode
import org.w3c.dom.Element

@Suppress("DEPRECATION", "MemberVisibilityCanBePrivate")
object ResourceUtils {

    const val TARGET_PREFERENCE_PATH = "res/xml/revanced_prefs.xml"

    const val YOUTUBE_SETTINGS_PATH = "res/xml/settings_fragment.xml"

    var youtubePackageName = "com.google.android.youtube"

    private var iconType = "default"
    fun getIconType() = iconType

    fun ResourceContext.updatePackageName(
        fromPackageName: String,
        toPackageName: String
    ) {
        youtubePackageName = toPackageName

        val prefs = this[YOUTUBE_SETTINGS_PATH]

        prefs.writeText(
            prefs.readText()
                .replace(
                    "android:targetPackage=\"$fromPackageName",
                    "android:targetPackage=\"$toPackageName"
                )
        )
    }

    fun ResourceContext.updateGmsCorePackageName(
        fromPackageName: String,
        toPackageName: String
    ) {
        val prefs = this[TARGET_PREFERENCE_PATH]

        prefs.writeText(
            prefs.readText()
                .replace(
                    "android:targetPackage=\"$fromPackageName",
                    "android:targetPackage=\"$toPackageName"
                )
        )
    }

    fun ResourceContext.addPreference(settingArray: Array<String>) {
        val prefs = this[TARGET_PREFERENCE_PATH]

        settingArray.forEach preferenceLoop@{ preference ->
            prefs.writeText(
                prefs.readText()
                    .replace("<!-- $preference", "")
                    .replace("$preference -->", "")
            )
        }
    }

    fun ResourceContext.updatePatchStatus(patchTitle: String) {
        updatePatchStatusSettings(patchTitle, "@string/revanced_patches_included")
    }

    fun ResourceContext.updatePatchStatusIcon(iconName: String) {
        iconType = iconName
        updatePatchStatusSettings("Icon", "@string/revanced_icon_$iconName")
    }

    fun ResourceContext.updatePatchStatusLabel(appName: String) {
        updatePatchStatusSettings("Label", appName)
    }

    fun ResourceContext.updatePatchStatusTheme(themeName: String) {
        updatePatchStatusSettings("Theme", themeName)
    }

    fun ResourceContext.updatePatchStatusSettings(
        patchTitle: String,
        updateText: String
    ) {
        this.xmlEditor[TARGET_PREFERENCE_PATH].use { editor ->
            editor.file.doRecursively loop@{
                if (it !is Element) return@loop

                it.getAttributeNode("android:title")?.let { attribute ->
                    if (attribute.textContent == patchTitle) {
                        it.getAttributeNode("android:summary").textContent = updateText
                    }
                }
            }
        }
    }

    fun ResourceContext.addPreferenceFragment(key: String, insertKey: String) {
        val targetClass =
            "com.google.android.apps.youtube.app.settings.videoquality.VideoQualitySettingsActivity"

        this.xmlEditor[YOUTUBE_SETTINGS_PATH].use { editor ->
            with(editor.file) {
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
                                            youtubePackageName
                                        )
                                        intentNode.setAttribute("android:data", key + "_intent")
                                        intentNode.setAttribute("android:targetClass", targetClass)
                                    }
                                )
                            }
                            node.setAttribute("app:iconSpaceReserved", "true")
                        }

                        "true" -> {
                            attributeNode.textContent = "false"
                        }
                    }
                }
            }
        }
    }
}
