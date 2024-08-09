package app.revanced.patches.music.misc.backgroundplayback

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patches.music.misc.backgroundplayback.fingerprints.BackgroundPlaybackManagerFingerprint
import app.revanced.patches.music.misc.backgroundplayback.fingerprints.DataSavingSettingsFragmentFingerprint
import app.revanced.patches.music.misc.backgroundplayback.fingerprints.KidsBackgroundPlaybackPolicyControllerFingerprint
import app.revanced.patches.music.misc.backgroundplayback.fingerprints.MusicBrowserServiceFingerprint
import app.revanced.patches.music.misc.backgroundplayback.fingerprints.PodCastConfigFingerprint
import app.revanced.patches.music.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.util.getStartsWithStringInstructionIndex
import app.revanced.util.getStringInstructionIndex
import app.revanced.util.getWalkerMethod
import app.revanced.util.patch.BaseBytecodePatch
import app.revanced.util.resultOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction

@Suppress("unused")
object BackgroundPlaybackPatch : BaseBytecodePatch(
    name = "Remove background playback restrictions",
    description = "Removes restrictions on background playback, including playing kids videos in the background.",
    compatiblePackages = COMPATIBLE_PACKAGE,
    fingerprints = setOf(
        BackgroundPlaybackManagerFingerprint,
        DataSavingSettingsFragmentFingerprint,
        KidsBackgroundPlaybackPolicyControllerFingerprint,
        MusicBrowserServiceFingerprint,
        PodCastConfigFingerprint,
    )
) {
    override fun execute(context: BytecodeContext) {

        // region patch for background play

        BackgroundPlaybackManagerFingerprint.resultOrThrow().mutableMethod.addInstructions(
            0, """
                const/4 v0, 0x1
                return v0
                """
        )

        // endregion

        // region patch for exclusive audio playback

        // don't play music video
        MusicBrowserServiceFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val targetIndex =
                    getStartsWithStringInstructionIndex("MBS: Return empty root for client: %s")

                for (index in targetIndex downTo 0) {
                    if (getInstruction(index).opcode != Opcode.INVOKE_VIRTUAL) continue

                    val targetReference = getInstruction<ReferenceInstruction>(index).reference

                    if (!targetReference.toString().endsWith("()Z")) continue

                    val walkerMethod = getWalkerMethod(context, index)

                    walkerMethod.addInstructions(
                        0, """
                            const/4 v0, 0x1
                            return v0
                            """
                    )
                    break
                }
            }
        }

        // don't play podcast videos
        // enable by default from YouTube Music 7.05.52+

        val podCastConfigFingerprintResult = PodCastConfigFingerprint.result
        val dataSavingSettingsFragmentFingerprintResult =
            DataSavingSettingsFragmentFingerprint.result

        val isPatchingOldVersion =
            podCastConfigFingerprintResult != null
                    && dataSavingSettingsFragmentFingerprintResult != null

        if (isPatchingOldVersion) {
            podCastConfigFingerprintResult!!.mutableMethod.apply {
                val insertIndex = implementation!!.instructions.size - 1
                val targetRegister = getInstruction<OneRegisterInstruction>(insertIndex).registerA

                addInstruction(
                    insertIndex,
                    "const/4 v$targetRegister, 0x1"
                )
            }

            dataSavingSettingsFragmentFingerprintResult!!.mutableMethod.apply {
                val insertIndex = getStringInstructionIndex("pref_key_dont_play_nma_video") + 4
                val targetRegister = getInstruction<FiveRegisterInstruction>(insertIndex).registerD

                addInstruction(
                    insertIndex,
                    "const/4 v$targetRegister, 0x1"
                )
            }
        }

        // endregion

        // region patch for minimized playback

        KidsBackgroundPlaybackPolicyControllerFingerprint.resultOrThrow().mutableMethod.addInstruction(
            0, "return-void"
        )

        // endregion

    }
}
