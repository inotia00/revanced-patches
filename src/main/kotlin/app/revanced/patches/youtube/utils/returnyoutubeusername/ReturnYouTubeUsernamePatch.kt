package app.revanced.patches.youtube.utils.returnyoutubeusername

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patches.shared.returnyoutubeusername.BaseReturnYouTubeUsernamePatch
import app.revanced.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.patch.BaseBytecodePatch

@Suppress("unused")
object ReturnYouTubeUsernamePatch : BaseBytecodePatch(
    name = "Return YouTube Username",
    description = "Adds an option to replace YouTube handles with usernames in comments using YouTube Data API v3.",
    dependencies = setOf(
        BaseReturnYouTubeUsernamePatch::class,
        SettingsPatch::class,
    ),
    compatiblePackages = COMPATIBLE_PACKAGE,
    use = false
) {
    override fun execute(context: BytecodeContext) {

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE_SCREEN: RETURN_YOUTUBE_USERNAME"
            )
        )

        SettingsPatch.updatePatchStatus(this)

    }
}
