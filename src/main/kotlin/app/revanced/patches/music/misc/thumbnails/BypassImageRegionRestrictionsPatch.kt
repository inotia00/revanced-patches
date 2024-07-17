package app.revanced.patches.music.misc.thumbnails

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patches.music.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.music.utils.imageurlhook.CronetImageUrlHookPatch
import app.revanced.patches.music.utils.settings.CategoryType
import app.revanced.patches.music.utils.settings.SettingsPatch
import app.revanced.util.patch.BaseBytecodePatch

@Suppress("unused")
object BypassImageRegionRestrictionsPatch : BaseBytecodePatch(
    name = "Bypass image region restrictions",
    description = "Adds an option to use a different host for static images, " +
            "so that images blocked in some countries can be received.",
    dependencies = setOf(
        CronetImageUrlHookPatch::class,
        SettingsPatch::class
    ),
    compatiblePackages = COMPATIBLE_PACKAGE
) {
    override fun execute(context: BytecodeContext) {
        CronetImageUrlHookPatch.addImageUrlHook()

        SettingsPatch.addSwitchPreference(
            CategoryType.MISC,
            "revanced_bypass_image_region_restrictions",
            "false"
        )
    }
}
