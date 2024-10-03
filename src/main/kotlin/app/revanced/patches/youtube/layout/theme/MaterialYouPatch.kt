package app.revanced.patches.youtube.layout.theme

import app.revanced.patcher.data.ResourceContext
import app.revanced.patches.youtube.layout.theme.BaseThemePatch.isMonetPatchIncluded
import app.revanced.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.settings.ResourceUtils.updatePatchStatusTheme
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.ResourceGroup
import app.revanced.util.copyResources
import app.revanced.util.copyXmlNode
import app.revanced.util.patch.BaseResourcePatch

@Suppress("DEPRECATION", "unused")
object MaterialYouPatch : BaseResourcePatch(
    name = "MaterialYou",
    description = "Applies the MaterialYou theme for Android 12+ devices.",
    dependencies = setOf(
        BaseThemePatch::class,
        SettingsPatch::class,
    ),
    compatiblePackages = COMPATIBLE_PACKAGE,
    use = false
) {
    override fun execute(context: ResourceContext) {

        arrayOf(
            ResourceGroup(
                "drawable-night-v31",
                "new_content_dot_background.xml"
            ),
            ResourceGroup(
                "drawable-v31",
                "new_content_count_background.xml",
                "new_content_dot_background.xml"
            ),
            ResourceGroup(
                "layout-v31",
                "new_content_count.xml"
            )
        ).forEach {
            context["res/${it.resourceDirectoryName}"].mkdirs()
            context.copyResources("youtube/materialyou", it)
        }

        context.copyXmlNode("youtube/materialyou/host", "values-v31/colors.xml", "resources")

        context.updatePatchStatusTheme("MaterialYou")

        isMonetPatchIncluded = true

    }
}
