package app.revanced.patches.youtube.layout.branding.icon

import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.patch.options.PatchOption.PatchExtensions.booleanPatchOption
import app.revanced.patcher.patch.options.PatchOption.PatchExtensions.stringPatchOption
import app.revanced.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.settings.ResourceUtils.updatePatchStatusIcon
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.ResourceGroup
import app.revanced.util.Utils.trimIndentMultiline
import app.revanced.util.copyFile
import app.revanced.util.copyResources
import app.revanced.util.copyXmlNode
import app.revanced.util.getResourceGroup
import app.revanced.util.patch.BaseResourcePatch
import app.revanced.util.underBarOrThrow

@Suppress("unused")
object CustomBrandingIconPatch : BaseResourcePatch(
    name = "Custom branding icon for YouTube",
    description = "Changes the YouTube app icon to the icon specified in options.json.",
    dependencies = setOf(SettingsPatch::class),
    compatiblePackages = COMPATIBLE_PACKAGE,
) {
    private const val DEFAULT_ICON = "revancify_blue"

    private val availableIcon = mapOf(
        "AFN Blue" to "afn_blue",
        "AFN Red" to "afn_red",
        "MMT" to "mmt",
        "Revancify Blue" to DEFAULT_ICON,
        "Revancify Red" to "revancify_red",
        "YouTube" to "youtube",
        "YouTube (Minimal header)" to "youtube_minimal_header"
    )

    private val sizeArray = arrayOf(
        "xxxhdpi",
        "xxhdpi",
        "xhdpi",
        "hdpi",
        "mdpi"
    )

    private val drawableDirectories = sizeArray.map { "drawable-$it" }

    private val mipmapDirectories = sizeArray.map { "mipmap-$it" }

    private val launcherIconResourceFileNames = arrayOf(
        "adaptiveproduct_youtube_background_color_108",
        "adaptiveproduct_youtube_foreground_color_108",
        "ic_launcher",
        "ic_launcher_round"
    ).map { "$it.png" }.toTypedArray()

    private val splashIconResourceFileNames = arrayOf(
        "product_logo_youtube_color_24",
        "product_logo_youtube_color_36",
        "product_logo_youtube_color_144",
        "product_logo_youtube_color_192"
    ).map { "$it.png" }.toTypedArray()

    private val oldSplashAnimationResourceFileNames = arrayOf(
        "\$\$avd_anim__1__0",
        "\$\$avd_anim__1__1",
        "\$\$avd_anim__2__0",
        "\$\$avd_anim__2__1",
        "\$\$avd_anim__3__0",
        "\$\$avd_anim__3__1",
        "\$avd_anim__0",
        "\$avd_anim__1",
        "\$avd_anim__2",
        "\$avd_anim__3",
        "\$avd_anim__4",
        "avd_anim"
    ).map { "$it.xml" }.toTypedArray()

    private val launcherIconResourceGroups =
        mipmapDirectories.getResourceGroup(launcherIconResourceFileNames)

    private val splashIconResourceGroups =
        drawableDirectories.getResourceGroup(splashIconResourceFileNames)

    private val oldSplashAnimationResourceGroups =
        listOf("drawable").getResourceGroup(oldSplashAnimationResourceFileNames)

    // region patch option

    val AppIcon = stringPatchOption(
        key = "AppIcon",
        default = DEFAULT_ICON,
        values = availableIcon,
        title = "App icon",
        description = """
            The icon to apply to the app.
            
            If a path to a folder is provided, the folder must contain the following folders:

            ${mipmapDirectories.joinToString("\n") { "- $it" }}

            Each of these folders must contain the following files:

            ${launcherIconResourceFileNames.joinToString("\n") { "- $it" }}
            """.trimIndentMultiline(),
        required = true
    )

    private val ChangeSplashIcon by booleanPatchOption(
        key = "ChangeSplashIcon",
        default = true,
        title = "Change splash icons",
        description = "Apply the custom branding icon to the splash screen.",
        required = true
    )

    private val RestoreOldSplashAnimation by booleanPatchOption(
        key = "RestoreOldSplashAnimation",
        default = true,
        title = "Restore old splash animation",
        description = "Restore the old style splash animation.",
        required = true
    )

    // endregion

    override fun execute(context: ResourceContext) {

        // Check patch options first.
        val appIcon = AppIcon
            .underBarOrThrow()

        val appIconResourcePath = "youtube/branding/$appIcon"

        // Check if a custom path is used in the patch options.
        if (!availableIcon.containsValue(appIcon)) {
            val copiedFiles = context.copyFile(
                launcherIconResourceGroups,
                appIcon,
                "WARNING: Invalid app icon path: $appIcon. Does not apply patches."
            )
            if (copiedFiles)
                context.updatePatchStatusIcon("custom")
        } else {
            // Change launcher icon.
            launcherIconResourceGroups.let { resourceGroups ->
                resourceGroups.forEach {
                    context.copyResources("$appIconResourcePath/launcher", it)
                }
            }

            // Change monochrome icon.
            arrayOf(
                ResourceGroup(
                    "drawable",
                    "adaptive_monochrome_ic_youtube_launcher.xml"
                )
            ).forEach { resourceGroup ->
                context.copyResources("$appIconResourcePath/monochrome", resourceGroup)
            }

            // Change splash icon.
            if (ChangeSplashIcon == true) {
                splashIconResourceGroups.let { resourceGroups ->
                    resourceGroups.forEach {
                        context.copyResources("$appIconResourcePath/splash", it)
                    }
                }
            }

            // Change splash screen.
            if (RestoreOldSplashAnimation == true) {
                oldSplashAnimationResourceGroups.let { resourceGroups ->
                    resourceGroups.forEach {
                        context.copyResources("$appIconResourcePath/splash", it)
                    }
                }

                context.copyXmlNode(
                    "$appIconResourcePath/splash",
                    "values-v31/styles.xml",
                    "resources"
                )
            }

            context.updatePatchStatusIcon(appIcon)
        }
    }
}
