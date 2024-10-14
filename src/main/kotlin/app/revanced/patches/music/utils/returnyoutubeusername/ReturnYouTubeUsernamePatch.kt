package app.revanced.patches.music.utils.returnyoutubeusername

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patches.music.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.music.utils.settings.CategoryType
import app.revanced.patches.music.utils.settings.SettingsPatch
import app.revanced.patches.shared.returnyoutubeusername.BaseReturnYouTubeUsernamePatch
import app.revanced.util.patch.BaseBytecodePatch

@Suppress("unused")
object ReturnYouTubeUsernamePatch : BaseBytecodePatch(
    name = "Return YouTube Usernames",
    description = "Adds an option to replace YouTube handles with usernames in comments using YouTube Data API v3.",
    dependencies = setOf(
        BaseReturnYouTubeUsernamePatch::class,
        SettingsPatch::class,
    ),
    compatiblePackages = COMPATIBLE_PACKAGE,
    use = false
) {
    override fun execute(context: BytecodeContext) {

        SettingsPatch.addSwitchPreference(
            CategoryType.RETURN_YOUTUBE_USERNAME,
            "revanced_return_youtube_username_enabled",
            "false"
        )
        SettingsPatch.addPreferenceWithIntent(
            CategoryType.RETURN_YOUTUBE_USERNAME,
            "revanced_return_youtube_username_youtube_data_api_v3_developer_key",
            "revanced_return_youtube_username_enabled"
        )
        if (SettingsPatch.upward0627) {
            SettingsPatch.addPreferenceWithIntent(
                CategoryType.RETURN_YOUTUBE_USERNAME,
                "revanced_return_youtube_username_youtube_data_api_v3_about"
            )
        }
    }
}
