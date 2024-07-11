package app.revanced.patches.youtube.utils.sponsorblock

import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.options.PatchOption.PatchExtensions.booleanPatchOption
import app.revanced.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.ResourceGroup
import app.revanced.util.copyResources
import app.revanced.util.copyXmlNode
import app.revanced.util.inputStreamFromBundledResource
import app.revanced.util.patch.BaseResourcePatch

@Suppress("DEPRECATION", "unused")
object SponsorBlockPatch : BaseResourcePatch(
    name = "SponsorBlock",
    description = "Adds options to enable and configure SponsorBlock, which can skip undesired video segments, such as sponsored content.",
    dependencies = setOf(
        SettingsPatch::class,
        SponsorBlockBytecodePatch::class
    ),
    compatiblePackages = COMPATIBLE_PACKAGE
) {
    private val OutlineIcon by booleanPatchOption(
        key = "OutlineIcon",
        default = false,
        title = "Outline icons",
        description = "Apply the outline icon.",
        required = true
    )

    override fun execute(context: ResourceContext) {
        /**
         * merge SponsorBlock drawables to main drawables
         */
        arrayOf(
            ResourceGroup(
                "layout",
                "revanced_sb_inline_sponsor_overlay.xml",
                "revanced_sb_skip_sponsor_button.xml"
            ),
            ResourceGroup(
                "drawable",
                "revanced_sb_new_segment_background.xml",
                "revanced_sb_skip_sponsor_button_background.xml"
            )
        ).forEach { resourceGroup ->
            context.copyResources("youtube/sponsorblock/shared", resourceGroup)
        }

        if (OutlineIcon == true) {
            arrayOf(
                ResourceGroup(
                    "layout",
                    "revanced_sb_new_segment.xml"
                ),
                ResourceGroup(
                    "drawable",
                    "revanced_sb_adjust.xml",
                    "revanced_sb_backward.xml",
                    "revanced_sb_compare.xml",
                    "revanced_sb_edit.xml",
                    "revanced_sb_forward.xml",
                    "revanced_sb_logo.xml",
                    "revanced_sb_publish.xml",
                    "revanced_sb_voting.xml"
                )
            ).forEach { resourceGroup ->
                context.copyResources("youtube/sponsorblock/outline", resourceGroup)
            }
        } else {
            arrayOf(
                ResourceGroup(
                    "layout",
                    "revanced_sb_new_segment.xml"
                ),
                ResourceGroup(
                    "drawable",
                    "revanced_sb_adjust.xml",
                    "revanced_sb_compare.xml",
                    "revanced_sb_edit.xml",
                    "revanced_sb_logo.xml",
                    "revanced_sb_publish.xml",
                    "revanced_sb_voting.xml"
                )
            ).forEach { resourceGroup ->
                context.copyResources("youtube/sponsorblock/default", resourceGroup)
            }
        }

        /**
         * merge xml nodes from the host to their real xml files
         */
        // copy nodes from host resources to their real xml files
        var modifiedControlsLayout = false

        inputStreamFromBundledResource(
            "youtube/sponsorblock",
            "shared/host/layout/youtube_controls_layout.xml",
        )?.let { hostingResourceStream ->
            val editor = context.xmlEditor["res/layout/youtube_controls_layout.xml"]

            // voting button id from the voting button view from the youtube_controls_layout.xml host file
            val votingButtonId = "@+id/revanced_sb_voting_button"

            "RelativeLayout".copyXmlNode(
                context.xmlEditor[hostingResourceStream],
                editor
            ).also {
                val document = editor.file
                val children = document.getElementsByTagName("RelativeLayout").item(0).childNodes

                // Replace the startOf with the voting button view so that the button does not overlap
                for (i in 1 until children.length) {
                    val view = children.item(i)

                    val playerVideoHeading = view.hasAttributes() &&
                            view.attributes.getNamedItem("android:id").nodeValue.endsWith("player_video_heading")

                    // Replace the attribute for a specific node only
                    if (!playerVideoHeading) continue

                    view.attributes.getNamedItem("android:layout_toStartOf").nodeValue =
                        votingButtonId

                    modifiedControlsLayout = true
                    break
                }
            }.close()
        }

        if (!modifiedControlsLayout) throw PatchException("Could not modify controls layout")

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE_SCREEN: SPONSOR_BLOCK"
            )
        )

        SettingsPatch.updatePatchStatus(this)

    }
}
