package app.revanced.patches.youtube.alternative.thumbnails

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.imageurlhook.CronetImageUrlHookPatch
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.patch.BaseBytecodePatch

@Suppress("unused")
object BypassImageRegionRestrictionsPatch : BaseBytecodePatch(
    name = "Bypass image region restrictions",
    description = "Adds an option to use a different host for static images, " +
            "so that images blocked in some countries can be received.",
    dependencies = setOf(
        CronetImageUrlHookPatch::class,
        SettingsPatch::class,
    ),
    compatiblePackages = COMPATIBLE_PACKAGE
) {
    override fun execute(context: BytecodeContext) {
        CronetImageUrlHookPatch.addImageUrlHook()

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE_SCREEN: ALTERNATIVE_THUMBNAILS",
                "SETTINGS: BYPASS_IMAGE_REGION_RESTRICTIONS"
            )
        )

        SettingsPatch.updatePatchStatus(this)
    }
}
