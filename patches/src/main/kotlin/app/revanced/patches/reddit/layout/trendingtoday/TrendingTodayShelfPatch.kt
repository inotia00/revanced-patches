package app.revanced.patches.reddit.layout.trendingtoday

import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.reddit.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.reddit.utils.extension.Constants.PATCHES_PATH
import app.revanced.patches.reddit.utils.patch.PatchList.HIDE_TRENDING_TODAY_SHELF
import app.revanced.patches.reddit.utils.settings.settingsPatch
import app.revanced.patches.reddit.utils.settings.updatePatchStatus
import app.revanced.util.fingerprint.matchOrThrow
import app.revanced.util.fingerprint.methodOrThrow
import app.revanced.util.indexOfFirstInstructionReversedOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction

private const val EXTENSION_CLASS_DESCRIPTOR =
    "$PATCHES_PATH/TrendingTodayShelfPatch;"

@Suppress("unused")
val trendingTodayShelfPatch = bytecodePatch(
    HIDE_TRENDING_TODAY_SHELF.title,
    HIDE_TRENDING_TODAY_SHELF.summary,
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(settingsPatch)

    execute {

        // region patch for hide trending today title.

        trendingTodayTitleFingerprint.matchOrThrow().let {
            it.method.apply {
                val stringIndex = it.stringMatches!!.first().index
                val relativeIndex =
                    indexOfFirstInstructionReversedOrThrow(stringIndex, Opcode.AND_INT_LIT8)
                val insertIndex = indexOfFirstInstructionReversedOrThrow(
                    relativeIndex + 1,
                    Opcode.MOVE_OBJECT_FROM16
                )
                val insertRegister = getInstruction<TwoRegisterInstruction>(insertIndex).registerA
                val jumpOpcode = if (returnType == "V") Opcode.RETURN_VOID else Opcode.SGET_OBJECT
                val jumpIndex = indexOfFirstInstructionReversedOrThrow(jumpOpcode)

                addInstructionsWithLabels(
                    insertIndex, """
                        invoke-static {}, $EXTENSION_CLASS_DESCRIPTOR->hideTrendingTodayShelf()Z
                        move-result v$insertRegister
                        if-nez v$insertRegister, :hidden
                        """, ExternalLabel("hidden", getInstruction(jumpIndex))
                )
            }
        }

        // endregion

        // region patch for hide trending today contents.

        trendingTodayItemFingerprint.methodOrThrow().addInstructionsWithLabels(
            0, """
                invoke-static {}, $EXTENSION_CLASS_DESCRIPTOR->hideTrendingTodayShelf()Z
                move-result v0
                if-eqz v0, :ignore
                return-void
                :ignore
                nop
                """
        )

        // endregion

        updatePatchStatus(
            "enableTrendingTodayShelf",
            HIDE_TRENDING_TODAY_SHELF
        )
    }
}
