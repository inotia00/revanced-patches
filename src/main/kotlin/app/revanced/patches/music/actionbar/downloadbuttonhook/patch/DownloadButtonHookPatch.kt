package app.revanced.patches.music.actionbar.downloadbuttonhook.patch

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patcher.patch.annotation.CompatiblePackage
import app.revanced.patches.music.utils.actionbarhook.patch.ActionBarHookPatch
import app.revanced.patches.music.utils.intenthook.patch.IntentHookPatch
import app.revanced.patches.music.utils.settings.resource.patch.SettingsPatch
import app.revanced.patches.music.video.information.patch.VideoInformationPatch
import app.revanced.util.enum.CategoryType

@Patch(
    name = "Hook download button",
    description = "Replaces the offline download button with an external download button.",
    compatiblePackages = [
        CompatiblePackage(
            "com.google.android.apps.youtube.music",
            [
                "6.15.52",
                "6.20.51",
                "6.21.51"
            ]
        )
    ],
    dependencies = [
        ActionBarHookPatch::class,
        IntentHookPatch::class,
        SettingsPatch::class,
        VideoInformationPatch::class
    ]
)
@Suppress("unused")
object DownloadButtonHookPatch : BytecodePatch() {
    override fun execute(context: BytecodeContext) {

        SettingsPatch.addMusicPreference(
            CategoryType.ACTION_BAR,
            "revanced_hook_action_bar_download",
            "false"
        )
        SettingsPatch.addMusicPreferenceWithIntent(
            CategoryType.ACTION_BAR,
            "revanced_external_downloader_package_name",
            "revanced_hook_action_bar_download"
        )

    }
}
