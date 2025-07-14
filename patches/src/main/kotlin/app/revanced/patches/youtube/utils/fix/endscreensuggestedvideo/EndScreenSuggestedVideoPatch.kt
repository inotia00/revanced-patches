package app.revanced.patches.youtube.utils.fix.endscreensuggestedvideo

import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.youtube.utils.extension.Constants.PLAYER_CLASS_DESCRIPTOR
import app.revanced.util.fingerprint.matchOrThrow
import app.revanced.util.fingerprint.methodOrThrow
import app.revanced.util.getReference
import app.revanced.util.getWalkerMethod
import app.revanced.util.indexOfFirstInstructionOrThrow
import app.revanced.util.indexOfFirstInstructionReversedOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

val endScreenSuggestedVideoPatch = bytecodePatch(
    description = "endScreenSuggestedVideoPatch"
) {
    execute {

        /**
         * The reasons why this patch is classified as a patch that fixes a 'bug' are as follows:
         * 1. In YouTube v18.29.38, the end screen suggested video was only shown when the autoplay setting was turned on.
         * 2. Starting from YouTube v18.35.36, the end screen suggested video is shown regardless of whether autoplay setting was turned on or off.
         *
         * This patch changes the end screen suggested video to be shown only when the autoplay setting is turned on.
         * Automatically closing the end screen suggested video is not appropriate as it will disable the autoplay behavior.
         */
        removeOnLayoutChangeListenerFingerprint.matchOrThrow().let {
            val walkerIndex =
                it.getWalkerMethod(it.patternMatch!!.endIndex)

            walkerIndex.apply {
                val autoNavStatusMethodName =
                    autoNavStatusFingerprint.methodOrThrow(autoNavConstructorFingerprint).name
                val invokeIndex =
                    indexOfFirstInstructionOrThrow {
                        val reference = getReference<MethodReference>()
                        reference?.returnType == "Z" &&
                                reference.parameterTypes.isEmpty() &&
                                reference.name == autoNavStatusMethodName
                    }
                val iGetObjectIndex =
                    indexOfFirstInstructionReversedOrThrow(invokeIndex, Opcode.IGET_OBJECT)

                val invokeReference = getInstruction<ReferenceInstruction>(invokeIndex).reference
                val iGetObjectReference =
                    getInstruction<ReferenceInstruction>(iGetObjectIndex).reference
                val opcodeName = getInstruction(invokeIndex).opcode.name

                addInstructionsWithLabels(
                    0,
                    """
                        invoke-static {}, $PLAYER_CLASS_DESCRIPTOR->hideEndScreenSuggestedVideo()Z
                        move-result v0
                        if-eqz v0, :show_end_screen_suggested_video

                        iget-object v0, p0, $iGetObjectReference

                        # This reference checks whether autoplay is turned on.
                        $opcodeName {v0}, $invokeReference
                        move-result v0

                        # Hide end screen suggested video only when autoplay is turned off.
                        if-nez v0, :show_end_screen_suggested_video
                        return-void
                        """,
                    ExternalLabel(
                        "show_end_screen_suggested_video",
                        getInstruction(0)
                    )
                )
            }
        }
    }
}
