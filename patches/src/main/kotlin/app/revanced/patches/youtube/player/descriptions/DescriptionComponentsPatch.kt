package app.revanced.patches.youtube.player.descriptions

import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.extensions.InstructionExtensions.replaceInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.shared.litho.addLithoFilter
import app.revanced.patches.shared.litho.lithoFilterPatch
import app.revanced.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.engagement.engagementPanelHookPatch
import app.revanced.patches.youtube.utils.extension.Constants.COMPONENTS_PATH
import app.revanced.patches.youtube.utils.extension.Constants.PLAYER_CLASS_DESCRIPTOR
import app.revanced.patches.youtube.utils.patch.PatchList.DESCRIPTION_COMPONENTS
import app.revanced.patches.youtube.utils.playertype.playerTypeHookPatch
import app.revanced.patches.youtube.utils.playservice.versionCheckPatch
import app.revanced.patches.youtube.utils.recyclerview.recyclerViewTreeObserverHook
import app.revanced.patches.youtube.utils.recyclerview.recyclerViewTreeObserverPatch
import app.revanced.patches.youtube.utils.resourceid.sharedResourceIdPatch
import app.revanced.patches.youtube.utils.rollingNumberTextViewAnimationUpdateFingerprint
import app.revanced.patches.youtube.utils.rollingNumberTextViewFingerprint
import app.revanced.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.revanced.patches.youtube.utils.settings.settingsPatch
import app.revanced.util.fingerprint.matchOrThrow
import app.revanced.util.fingerprint.methodOrThrow
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstructionOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val FILTER_CLASS_DESCRIPTOR =
    "$COMPONENTS_PATH/DescriptionsFilter;"

@Suppress("unused")
val descriptionComponentsPatch = bytecodePatch(
    DESCRIPTION_COMPONENTS.title,
    DESCRIPTION_COMPONENTS.summary,
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(
        settingsPatch,
        lithoFilterPatch,
        playerTypeHookPatch,
        recyclerViewTreeObserverPatch,
        engagementPanelHookPatch,
        sharedResourceIdPatch,
        versionCheckPatch,
    )

    execute {

        var settingArray = arrayOf(
            "PREFERENCE_SCREEN: PLAYER",
            "SETTINGS: DESCRIPTION_COMPONENTS"
        )

        // region patch for disable rolling number animation
        rollingNumberTextViewAnimationUpdateFingerprint.matchOrThrow(
            rollingNumberTextViewFingerprint
        ).let {
            it.method.apply {
                val freeRegister = implementation!!.registerCount - parameters.size - 2
                val imageSpanIndex = it.patternMatch!!.startIndex
                val setTextIndex = indexOfFirstInstructionOrThrow {
                    opcode == Opcode.INVOKE_VIRTUAL &&
                            getReference<MethodReference>()?.name == "setText"
                }
                addInstruction(setTextIndex, "nop")
                addInstructionsWithLabels(
                    imageSpanIndex, """
                        invoke-static {}, $PLAYER_CLASS_DESCRIPTOR->disableRollingNumberAnimations()Z
                        move-result v$freeRegister
                        if-nez v$freeRegister, :disable_animations
                        """, ExternalLabel("disable_animations", getInstruction(setTextIndex))
                )
            }
        }
        // endregion

        // region patch for disable video description interaction and expand video description

        // since these patches are still A/B tested, they are classified as 'Experimental flags'.
        textViewComponentFingerprint.methodOrThrow().apply {
            val insertIndex = indexOfTextIsSelectableInstruction(this)
            val insertInstruction = getInstruction<FiveRegisterInstruction>(insertIndex)

            replaceInstruction(
                insertIndex,
                "invoke-static {v${insertInstruction.registerC}, v${insertInstruction.registerD}}, " +
                        "$PLAYER_CLASS_DESCRIPTOR->disableVideoDescriptionInteraction(Landroid/widget/TextView;Z)V"
            )
        }

        recyclerViewTreeObserverHook("$PLAYER_CLASS_DESCRIPTOR->onVideoDescriptionCreate(Landroid/support/v7/widget/RecyclerView;)V")

        // endregion

        addLithoFilter(FILTER_CLASS_DESCRIPTOR)

        // region add settings

        addPreference(settingArray, DESCRIPTION_COMPONENTS)

        // endregion

    }
}
