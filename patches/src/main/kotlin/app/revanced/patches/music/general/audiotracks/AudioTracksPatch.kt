package app.revanced.patches.music.general.audiotracks

import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patches.music.utils.audiotracks.audioTracksHookPatch
import app.revanced.patches.music.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.music.utils.patch.PatchList.DISABLE_FORCED_AUTO_AUDIO_TRACKS
import app.revanced.patches.music.utils.settings.CategoryType
import app.revanced.patches.music.utils.settings.ResourceUtils.updatePatchStatus
import app.revanced.patches.music.utils.settings.addSwitchPreference
import app.revanced.patches.music.utils.settings.settingsPatch
import app.revanced.patches.shared.audiotracks.disableForcedAudioTracks

@Suppress("unused")
val audioTracksPatch = bytecodePatch(
    DISABLE_FORCED_AUTO_AUDIO_TRACKS.title,
    DISABLE_FORCED_AUTO_AUDIO_TRACKS.summary,
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(
        settingsPatch,
        audioTracksHookPatch,
    )

    execute {
        disableForcedAudioTracks()

        addSwitchPreference(
            CategoryType.GENERAL,
            "revanced_disable_auto_audio_tracks",
            "true"
        )

        updatePatchStatus(DISABLE_FORCED_AUTO_AUDIO_TRACKS)

    }
}