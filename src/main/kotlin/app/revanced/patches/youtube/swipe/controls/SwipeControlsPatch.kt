package app.revanced.patches.youtube.swipe.controls

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.youtube.swipe.controls.fingerprints.FullScreenEngagementOverlayFingerprint
import app.revanced.patches.youtube.swipe.controls.fingerprints.HDRBrightnessFingerprint
import app.revanced.patches.youtube.swipe.controls.fingerprints.SwipeToSwitchVideoFingerprint
import app.revanced.patches.youtube.swipe.controls.fingerprints.WatchPanelGesturesFingerprint
import app.revanced.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.integrations.Constants.INTEGRATIONS_PATH
import app.revanced.patches.youtube.utils.integrations.Constants.SWIPE_PATH
import app.revanced.patches.youtube.utils.lockmodestate.LockModeStateHookPatch
import app.revanced.patches.youtube.utils.mainactivity.MainActivityResolvePatch
import app.revanced.patches.youtube.utils.mainactivity.MainActivityResolvePatch.mainActivityMutableClass
import app.revanced.patches.youtube.utils.playertype.PlayerTypeHookPatch
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.FullScreenEngagementOverlay
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.patches.youtube.utils.settings.SettingsPatch.contexts
import app.revanced.util.ResourceGroup
import app.revanced.util.copyResources
import app.revanced.util.getWideLiteralInstructionIndex
import app.revanced.util.literalInstructionBooleanHook
import app.revanced.util.patch.BaseBytecodePatch
import app.revanced.util.resultOrThrow
import app.revanced.util.transformMethods
import app.revanced.util.traverseClassHierarchy
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod

@Suppress("unused")
object SwipeControlsPatch : BaseBytecodePatch(
    name = "Swipe controls",
    description = "Adds options for controlling volume and brightness with swiping, " +
        "and whether to enter fullscreen when swiping down below the player.",
    dependencies = setOf(
        LockModeStateHookPatch::class,
        MainActivityResolvePatch::class,
        PlayerTypeHookPatch::class,
        SettingsPatch::class,
        SharedResourceIdPatch::class
    ),
    compatiblePackages = COMPATIBLE_PACKAGE,
    fingerprints = setOf(
        FullScreenEngagementOverlayFingerprint,
        HDRBrightnessFingerprint,
        SwipeToSwitchVideoFingerprint,
        WatchPanelGesturesFingerprint
    )
) {
    private const val INTEGRATIONS_SWIPE_CONTROLS_HOST_ACTIVITY_CLASS_DESCRIPTOR =
        "$INTEGRATIONS_PATH/swipecontrols/SwipeControlsHostActivity;"

    private const val INTEGRATIONS_SWIPE_CONTROLS_PATCH_CLASS_DESCRIPTOR =
        "$SWIPE_PATH/SwipeControlsPatch;"

    override fun execute(context: BytecodeContext) {

        // region patch for swipe controls patch

        val hostActivityClass =
            context.findClass(INTEGRATIONS_SWIPE_CONTROLS_HOST_ACTIVITY_CLASS_DESCRIPTOR)!!.mutableClass
        val mainActivityClass = mainActivityMutableClass

        // inject the wrapper class from integrations into the class hierarchy of MainActivity (WatchWhileActivity)
        hostActivityClass.setSuperClass(mainActivityClass.superclass)
        mainActivityClass.setSuperClass(hostActivityClass.type)

        // ensure all classes and methods in the hierarchy are non-final, so we can override them in integrations
        context.traverseClassHierarchy(mainActivityClass) {
            accessFlags = accessFlags and AccessFlags.FINAL.value.inv()
            transformMethods {
                ImmutableMethod(
                    definingClass,
                    name,
                    parameters,
                    returnType,
                    accessFlags and AccessFlags.FINAL.value.inv(),
                    annotations,
                    hiddenApiRestrictions,
                    implementation
                ).toMutable()
            }
        }

        FullScreenEngagementOverlayFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val viewIndex = getWideLiteralInstructionIndex(FullScreenEngagementOverlay) + 3
                val viewRegister = getInstruction<OneRegisterInstruction>(viewIndex).registerA

                addInstruction(
                    viewIndex + 1,
                    "invoke-static {v$viewRegister}, $INTEGRATIONS_SWIPE_CONTROLS_PATCH_CLASS_DESCRIPTOR->setFullscreenEngagementOverlayView(Landroid/view/View;)V"
                )
            }
        }

        // endregion

        var settingArray = arrayOf(
            "PREFERENCE_SCREEN: SWIPE_CONTROLS"
        )

        // region patch for disable HDR auto brightness

        // Since it does not support all versions,
        // add settings only if the patch is successful.
        HDRBrightnessFingerprint.result?.mutableMethod?.apply {
            addInstructionsWithLabels(
                0, """
                    invoke-static {}, $INTEGRATIONS_SWIPE_CONTROLS_PATCH_CLASS_DESCRIPTOR->disableHDRAutoBrightness()Z
                    move-result v0
                    if-eqz v0, :default
                    return-void
                    """, ExternalLabel("default", getInstruction(0))
            )

            settingArray += "PREFERENCE_CATEGORY: SWIPE_CONTROLS_EXPERIMENTAL_FLAGS"
            settingArray += "SETTINGS: DISABLE_HDR_BRIGHTNESS"
        }

        // endregion

        // region patch for enable swipe to switch video

        // Since it does not support all versions,
        // add settings only if the patch is successful.
        SwipeToSwitchVideoFingerprint.result?.let {
            SwipeToSwitchVideoFingerprint.literalInstructionBooleanHook(
                45631116,
                "$INTEGRATIONS_SWIPE_CONTROLS_PATCH_CLASS_DESCRIPTOR->enableSwipeToSwitchVideo()Z"
            )

            settingArray += "PREFERENCE_CATEGORY: SWIPE_CONTROLS_EXPERIMENTAL_FLAGS"
            settingArray += "SETTINGS: ENABLE_SWIPE_TO_SWITCH_VIDEO"
        }

        // endregion

        // region patch for enable watch panel gestures

        // Since it does not support all versions,
        // add settings only if the patch is successful.
        WatchPanelGesturesFingerprint.result?.let {
            WatchPanelGesturesFingerprint.literalInstructionBooleanHook(
                45372793,
                "$INTEGRATIONS_SWIPE_CONTROLS_PATCH_CLASS_DESCRIPTOR->enableWatchPanelGestures()Z"
            )

            settingArray += "PREFERENCE_CATEGORY: SWIPE_CONTROLS_EXPERIMENTAL_FLAGS"
            settingArray += "SETTINGS: ENABLE_WATCH_PANEL_GESTURES"
        }

        // endregion

        // region copy resources

        contexts.copyResources(
            "youtube/swipecontrols",
            ResourceGroup(
                "drawable",
                "ic_sc_brightness_auto.xml",
                "ic_sc_brightness_manual.xml",
                "ic_sc_volume_mute.xml",
                "ic_sc_volume_normal.xml"
            )
        )

        // endregion

        /**
         * Add settings
         */
        SettingsPatch.addPreference(settingArray)

        SettingsPatch.updatePatchStatus(this)
    }
}