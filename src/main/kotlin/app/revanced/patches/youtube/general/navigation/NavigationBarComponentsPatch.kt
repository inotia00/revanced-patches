package app.revanced.patches.youtube.general.navigation

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patches.youtube.general.navigation.fingerprints.AutoMotiveFingerprint
import app.revanced.patches.youtube.general.navigation.fingerprints.PivotBarChangedFingerprint
import app.revanced.patches.youtube.general.navigation.fingerprints.PivotBarSetTextFingerprint
import app.revanced.patches.youtube.general.navigation.fingerprints.PivotBarStyleFingerprint
import app.revanced.patches.youtube.general.navigation.fingerprints.TranslucentNavigationBarFingerprint
import app.revanced.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.integrations.Constants.GENERAL_CLASS_DESCRIPTOR
import app.revanced.patches.youtube.utils.navigation.NavigationBarHookPatch
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstructionOrThrow
import app.revanced.util.indexOfFirstStringInstructionOrThrow
import app.revanced.util.injectLiteralInstructionBooleanCall
import app.revanced.util.patch.BaseBytecodePatch
import app.revanced.util.resultOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

@Suppress("unused")
object NavigationBarComponentsPatch : BaseBytecodePatch(
    name = "Navigation bar components",
    description = "Adds options to hide or change components related to the navigation bar.",
    dependencies = setOf(
        SettingsPatch::class,
        SharedResourceIdPatch::class,
        NavigationBarHookPatch::class
    ),
    compatiblePackages = COMPATIBLE_PACKAGE,
    fingerprints = setOf(
        AutoMotiveFingerprint,
        PivotBarChangedFingerprint,
        PivotBarSetTextFingerprint,
        PivotBarStyleFingerprint,
        TranslucentNavigationBarFingerprint
    )
) {
    override fun execute(context: BytecodeContext) {

        var settingArray = arrayOf(
            "PREFERENCE_SCREEN: GENERAL",
            "SETTINGS: HIDE_NAVIGATION_COMPONENTS"
        )

        // region patch for enable translucent navigation bar

        if (SettingsPatch.upward1923) {
            TranslucentNavigationBarFingerprint.injectLiteralInstructionBooleanCall(
                45630927,
                "$GENERAL_CLASS_DESCRIPTOR->enableTranslucentNavigationBar()Z"
            )

            settingArray += "SETTINGS: TRANSLUCENT_NAVIGATION_BAR"
        }

        // endregion

        // region patch for enable narrow navigation buttons

        arrayOf(
            PivotBarChangedFingerprint,
            PivotBarStyleFingerprint
        ).forEach { fingerprint ->
            fingerprint.resultOrThrow().let {
                it.mutableMethod.apply {
                    val targetIndex = it.scanResult.patternScanResult!!.startIndex + 1
                    val register = getInstruction<OneRegisterInstruction>(targetIndex).registerA

                    addInstructions(
                        targetIndex + 1, """
                            invoke-static {v$register}, $GENERAL_CLASS_DESCRIPTOR->enableNarrowNavigationButton(Z)Z
                            move-result v$register
                            """
                    )
                }
            }
        }

        // endregion

        // region patch for hide navigation bar

        NavigationBarHookPatch.addBottomBarContainerHook("$GENERAL_CLASS_DESCRIPTOR->hideNavigationBar(Landroid/view/View;)V")

        // endregion

        // region patch for hide navigation buttons

        AutoMotiveFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val insertIndex = indexOfFirstStringInstructionOrThrow("Android Automotive") - 1
                val insertRegister = getInstruction<OneRegisterInstruction>(insertIndex).registerA

                addInstructions(
                    insertIndex, """
                        invoke-static {v$insertRegister}, $GENERAL_CLASS_DESCRIPTOR->switchCreateWithNotificationButton(Z)Z
                        move-result v$insertRegister
                        """
                )
            }
        }

        // endregion

        // region patch for hide navigation label

        PivotBarSetTextFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val targetIndex = indexOfFirstInstructionOrThrow {
                    opcode == Opcode.INVOKE_VIRTUAL &&
                            getReference<MethodReference>()?.name == "setText"
                }
                val targetRegister = getInstruction<FiveRegisterInstruction>(targetIndex).registerC

                addInstruction(
                    targetIndex,
                    "invoke-static {v$targetRegister}, $GENERAL_CLASS_DESCRIPTOR->hideNavigationLabel(Landroid/widget/TextView;)V"
                )
            }
        }

        // endregion


        // Hook navigation button created, in order to hide them.
        NavigationBarHookPatch.hookNavigationButtonCreated(GENERAL_CLASS_DESCRIPTOR)

        /**
         * Add settings
         */
        SettingsPatch.addPreference(settingArray)

        SettingsPatch.updatePatchStatus(this)
    }
}
