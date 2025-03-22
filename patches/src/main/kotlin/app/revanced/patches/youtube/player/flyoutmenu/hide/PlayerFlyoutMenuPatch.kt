package app.revanced.patches.youtube.player.flyoutmenu.hide

import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.shared.litho.addLithoFilter
import app.revanced.patches.shared.litho.lithoFilterPatch
import app.revanced.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.extension.Constants.COMPONENTS_PATH
import app.revanced.patches.youtube.utils.extension.Constants.PLAYER_CLASS_DESCRIPTOR
import app.revanced.patches.youtube.utils.patch.PatchList.HIDE_PLAYER_FLYOUT_MENU
import app.revanced.patches.youtube.utils.playertype.playerTypeHookPatch
import app.revanced.patches.youtube.utils.playservice.is_18_39_or_greater
import app.revanced.patches.youtube.utils.playservice.is_19_30_or_greater
import app.revanced.patches.youtube.utils.playservice.versionCheckPatch
import app.revanced.patches.youtube.utils.qualityMenuViewInflateFingerprint
import app.revanced.patches.youtube.utils.resourceid.bottomSheetFooterText
import app.revanced.patches.youtube.utils.resourceid.sharedResourceIdPatch
import app.revanced.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.revanced.patches.youtube.utils.settings.settingsPatch
import app.revanced.util.REGISTER_TEMPLATE_REPLACEMENT
import app.revanced.util.fingerprint.injectLiteralInstructionBooleanCall
import app.revanced.util.fingerprint.injectLiteralInstructionViewCall
import app.revanced.util.fingerprint.methodOrThrow
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstructionOrThrow
import app.revanced.util.indexOfFirstInstructionReversedOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val PANELS_FILTER_CLASS_DESCRIPTOR =
    "$COMPONENTS_PATH/PlayerFlyoutMenuFilter;"

@Suppress("unused")
val playerFlyoutMenuPatch = bytecodePatch(
    HIDE_PLAYER_FLYOUT_MENU.title,
    HIDE_PLAYER_FLYOUT_MENU.summary,
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(
        settingsPatch,
        lithoFilterPatch,
        playerTypeHookPatch,
        sharedResourceIdPatch,
        versionCheckPatch
    )

    execute {
        var settingArray = arrayOf(
            "PREFERENCE_SCREEN: PLAYER",
            "PREFERENCE_SCREENS: FLYOUT_MENU",
            "SETTINGS: HIDE_PLAYER_FLYOUT_MENU"
        )

        // region hide player flyout menu header, footer (non-litho)

        mapOf(
            advancedQualityBottomSheetFingerprint to "hidePlayerFlyoutMenuQualityFooter",
            captionsBottomSheetFingerprint to "hidePlayerFlyoutMenuCaptionsFooter",
            qualityMenuViewInflateFingerprint to "hidePlayerFlyoutMenuQualityFooter"
        ).forEach { (fingerprint, name) ->
            val smaliInstruction = """
                    invoke-static {v$REGISTER_TEMPLATE_REPLACEMENT}, $PLAYER_CLASS_DESCRIPTOR->$name(Landroid/view/View;)V
                    """
            fingerprint.injectLiteralInstructionViewCall(bottomSheetFooterText, smaliInstruction)
        }

        arrayOf(
            advancedQualityBottomSheetFingerprint,
            qualityMenuViewInflateFingerprint
        ).forEach { fingerprint ->
            fingerprint.methodOrThrow().apply {
                val insertIndex = indexOfFirstInstructionOrThrow {
                    opcode == Opcode.INVOKE_VIRTUAL &&
                            getReference<MethodReference>()?.name == "addHeaderView"
                }
                val insertRegister = getInstruction<FiveRegisterInstruction>(insertIndex).registerD

                addInstructions(
                    insertIndex, """
                        invoke-static {v$insertRegister}, $PLAYER_CLASS_DESCRIPTOR->hidePlayerFlyoutMenuQualityHeader(Landroid/view/View;)Landroid/view/View;
                        move-result-object v$insertRegister
                        """
                )
            }
        }

        // endregion

        // region patch for hide '1080p Premium' label

        videoQualityArrayFingerprint.methodOrThrow().apply {
            val qualityLabelIndex = indexOfQualityLabelInstruction(this) + 1
            val qualityLabelRegister =
                getInstruction<OneRegisterInstruction>(qualityLabelIndex).registerA
            val jumpIndex = indexOfFirstInstructionReversedOrThrow(qualityLabelIndex) {
                opcode == Opcode.INVOKE_INTERFACE &&
                        getReference<MethodReference>()?.name == "hasNext"
            }

            addInstructionsWithLabels(
                qualityLabelIndex + 1, """
                    invoke-static {v$qualityLabelRegister}, $PLAYER_CLASS_DESCRIPTOR->hidePlayerFlyoutMenuEnhancedBitrate(Ljava/lang/String;)Ljava/lang/String;
                    move-result-object v$qualityLabelRegister
                    if-eqz v$qualityLabelRegister, :jump
                    """, ExternalLabel("jump", getInstruction(jumpIndex))
            )
        }

        // endregion

        // region patch for hide pip mode menu

        if (is_18_39_or_greater) {
            pipModeConfigFingerprint.injectLiteralInstructionBooleanCall(
                45427407L,
                "$PLAYER_CLASS_DESCRIPTOR->hidePiPModeMenu(Z)Z"
            )
            settingArray += "SETTINGS: HIDE_PIP_MODE_MENU"
        }

        // endregion

        // region patch for hide sleep timer menu

        if (is_19_30_or_greater) {
            // Sleep timer menu in Additional settings (deprecated)
            // TODO: A patch will be implemented to assign this deprecated menu to another action.
            // mapOf(
            //     sleepTimerConstructorFingerprint to SLEEP_TIMER_CONSTRUCTOR_FEATURE_FLAG,
            //     sleepTimerFingerprint to SLEEP_TIMER_FEATURE_FLAG
            // ).forEach { (fingerprint, literal) ->
            //     fingerprint.injectLiteralInstructionBooleanCall(
            //         literal,
            //         "$PLAYER_CLASS_DESCRIPTOR->hideDeprecatedSleepTimerMenu(Z)Z"
            //     )
            // }
            settingArray += "SETTINGS: HIDE_SLEEP_TIMER_MENU"
        }

        // endregion

        addLithoFilter(PANELS_FILTER_CLASS_DESCRIPTOR)

        // region add settings

        addPreference(settingArray, HIDE_PLAYER_FLYOUT_MENU)

        // endregion

    }
}
