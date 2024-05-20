package app.revanced.patches.youtube.layout.playerbuttonbg

import app.revanced.patcher.data.ResourceContext
import app.revanced.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.doRecursively
import app.revanced.util.patch.BaseResourcePatch
import org.w3c.dom.Element

@Suppress("unused")
object ForcePlayerButtonBackgroundPatch : BaseResourcePatch(
    name = "Force hide player buttons background",
    description = "Hide the dark background surrounding the video player controls at compile time.",
    dependencies = setOf(SettingsPatch::class),
    compatiblePackages = COMPATIBLE_PACKAGE,
    use = false
) {
    override fun execute(context: ResourceContext) {

        context.document["res/drawable/player_button_circle_background.xml"].use { editor ->
            editor.doRecursively { node ->
                arrayOf("color").forEach replacement@{ replacement ->
                    if (node !is Element) return@replacement

                    node.getAttributeNode("android:$replacement")?.let { attribute ->
                        attribute.textContent = "@android:color/transparent"
                    }
                }
            }
        }

        SettingsPatch.updatePatchStatus(this)
    }
}
