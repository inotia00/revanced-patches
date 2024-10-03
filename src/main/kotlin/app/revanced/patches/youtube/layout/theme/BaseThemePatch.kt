package app.revanced.patches.youtube.layout.theme

import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.ResourcePatch
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patches.shared.drawable.DrawableColorPatch
import app.revanced.patches.youtube.utils.integrations.Constants.UTILS_PATH
import org.w3c.dom.Element

@Patch(dependencies = [DrawableColorPatch::class])
@Suppress("DEPRECATION")
object BaseThemePatch : ResourcePatch() {
    private const val SPLASH_SCREEN_COLOR_NAME = "splashScreenColor"
    private const val SPLASH_SCREEN_COLOR_ATTRIBUTE = "?attr/$SPLASH_SCREEN_COLOR_NAME"

    override fun execute(context: ResourceContext) {

        DrawableColorPatch.injectCall("$UTILS_PATH/DrawableColorPatch;->getColor(I)I")

        // edit the resource files to change the splash screen color
        val attrsResourceFile = "res/values/attrs.xml"
        val stylesResourceFiles =
            listOf("values", "values-v31").map { valuesPath ->
                "res/$valuesPath/styles.xml"
            }.toTypedArray()

        context.xmlEditor[attrsResourceFile].use { editor ->
            val file = editor.file

            (file.getElementsByTagName("resources").item(0) as Element).appendChild(
                file.createElement("attr").apply {
                    setAttribute("format", "reference")
                    setAttribute("name", SPLASH_SCREEN_COLOR_NAME)
                }
            )
        }

        stylesResourceFiles.forEachIndexed { pathIndex, stylesPath ->
            context.xmlEditor[stylesPath].use { editor ->
                val file = editor.file

                val childNodes =
                    (file.getElementsByTagName("resources").item(0) as Element).childNodes

                for (i in 0 until childNodes.length) {
                    val node = childNodes.item(i) as? Element ?: continue
                    val nodeAttributeName = node.getAttribute("name")

                    file.createElement("item").apply {
                        setAttribute(
                            "name",
                            when (pathIndex) {
                                0 -> "splashScreenColor"
                                1 -> "android:windowSplashScreenBackground"
                                else -> "null"
                            }
                        )

                        appendChild(
                            file.createTextNode(
                                when (pathIndex) {
                                    0 -> when (nodeAttributeName) {
                                        "Base.Theme.YouTube.Launcher.Dark" -> "@color/yt_black1"
                                        "Base.Theme.YouTube.Launcher.Light" -> "@color/yt_white1"
                                        else -> "null"
                                    }

                                    1 -> when (nodeAttributeName) {
                                        "Base.Theme.YouTube.Launcher" -> SPLASH_SCREEN_COLOR_ATTRIBUTE
                                        else -> "null"
                                    }

                                    else -> "null"
                                }
                            )
                        )

                        if (this.textContent != "null")
                            node.appendChild(this)
                    }
                }
            }
        }

        val splashScreenResourceFiles =
            listOf("drawable", "drawable-sw600dp").map { quantumLaunchScreenPath ->
                "res/$quantumLaunchScreenPath/quantum_launchscreen_youtube.xml"
            }.toTypedArray()

        splashScreenResourceFiles.forEach editSplashScreen@{ resourceFile ->
            context.xmlEditor[resourceFile].use { editor ->
                val document = editor.file

                val layerList = document.getElementsByTagName("layer-list").item(0) as Element

                val childNodes = layerList.childNodes
                for (i in 0 until childNodes.length) {
                    val node = childNodes.item(i)
                    if (node is Element && node.hasAttribute("android:drawable")) {
                        node.setAttribute("android:drawable", SPLASH_SCREEN_COLOR_ATTRIBUTE)
                        return@editSplashScreen
                    }
                }

                throw PatchException("Failed to modify launch screen")
            }
        }

    }

    internal var isMonetPatchIncluded: Boolean = false
}