package app.revanced.patches.youtube.general.chapter

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.youtube.general.chapter.fingerprints.TimelineMarkerArrayFingerprint
import app.revanced.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.integrations.Constants.GENERAL_CLASS_DESCRIPTOR
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.patch.BaseBytecodePatch
import app.revanced.util.resultOrThrow

@Suppress("unused")
object DisableVideoChapterPatch : BaseBytecodePatch(
    name = "Disable video chapter",
    description = "Adds an option to disable video chapter.",
    dependencies = setOf(SettingsPatch::class),
    compatiblePackages = COMPATIBLE_PACKAGE,
    fingerprints = setOf(TimelineMarkerArrayFingerprint)
) {
    override fun execute(context: BytecodeContext) {

        TimelineMarkerArrayFingerprint.resultOrThrow().mutableMethod.addInstructionsWithLabels(
            0,
            """
                invoke-static {}, $PLAYER_CLASS_DESCRIPTOR->disableVideoChapter()Z
                move-result v0
                if-eqz v0, :show_chapter

                # Create an empty array
                const/4 v0, 0x0
                new-array p1, v0, [Lcom/google/android/libraries/youtube/player/features/overlay/timebar/TimelineMarker;
                return-object p1
            """,
            ExternalLabel("show_chapter", getInstruction(0)),
        )

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE_SCREEN: GENERAL",
                "SETTINGS: DISABLE_VIDEO_CHAPTER"
            )
        )

        SettingsPatch.updatePatchStatus(this)
    }
}