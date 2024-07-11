package app.revanced.patches.youtube.utils.playercontrols

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.annotation.Patch
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patches.youtube.utils.fingerprints.PlayerButtonsResourcesFingerprint
import app.revanced.patches.youtube.utils.fingerprints.PlayerButtonsVisibilityFingerprint
import app.revanced.patches.youtube.utils.fingerprints.YouTubeControlsOverlayFingerprint
import app.revanced.patches.youtube.utils.integrations.Constants.UTILS_PATH
import app.revanced.patches.youtube.utils.playercontrols.fingerprints.BottomControlsInflateFingerprint
import app.revanced.patches.youtube.utils.playercontrols.fingerprints.ControlsLayoutInflateFingerprint
import app.revanced.patches.youtube.utils.playercontrols.fingerprints.MotionEventFingerprint
import app.revanced.patches.youtube.utils.playercontrols.fingerprints.PlayerControlsVisibilityEntityModelFingerprint
import app.revanced.patches.youtube.utils.playercontrols.fingerprints.PlayerControlsVisibilityFingerprint
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch
import app.revanced.util.getTargetIndexOrThrow
import app.revanced.util.getTargetIndexWithMethodReferenceNameOrThrow
import app.revanced.util.resultOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@Patch(
    dependencies = [
        PlayerControlsVisibilityHookPatch::class,
        SharedResourceIdPatch::class
    ]
)
object PlayerControlsPatch : BytecodePatch(
    setOf(
        PlayerButtonsResourcesFingerprint,
        PlayerControlsVisibilityEntityModelFingerprint,
        BottomControlsInflateFingerprint,
        ControlsLayoutInflateFingerprint,
        YouTubeControlsOverlayFingerprint
    )
) {
    private const val INTEGRATIONS_CLASS_DESCRIPTOR =
        "$UTILS_PATH/PlayerControlsPatch;"

    private lateinit var changeVisibilityMethod: MutableMethod
    private lateinit var changeVisibilityNegatedImmediatelyMethod: MutableMethod
    private lateinit var initializeBottomControlButtonMethod: MutableMethod
    private lateinit var initializeTopControlButtonMethod: MutableMethod

    override fun execute(context: BytecodeContext) {

        // region patch for hook visibility of play control buttons (e.g. pause, play button, etc)

        PlayerButtonsVisibilityFingerprint.resolve(
            context,
            PlayerButtonsResourcesFingerprint.resultOrThrow().mutableClass
        )
        PlayerButtonsVisibilityFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val viewIndex = getTargetIndexOrThrow(Opcode.INVOKE_INTERFACE)
                val viewRegister = getInstruction<FiveRegisterInstruction>(viewIndex).registerD

                addInstruction(
                    viewIndex + 1,
                    "invoke-static {p1, p2, v$viewRegister}, $INTEGRATIONS_CLASS_DESCRIPTOR->changeVisibility(ZZLandroid/view/View;)V"
                )
            }
        }

        // endregion

        // region patch for hook visibility of play controls layout

        PlayerControlsVisibilityFingerprint.resolve(
            context,
            YouTubeControlsOverlayFingerprint.resultOrThrow().mutableClass
        )
        PlayerControlsVisibilityFingerprint.resultOrThrow().mutableMethod.addInstruction(
            0,
            "invoke-static {p1}, $INTEGRATIONS_CLASS_DESCRIPTOR->changeVisibility(Z)V"
        )

        // endregion

        // region patch for detecting motion events in play controls layout

        MotionEventFingerprint.resolve(
            context,
            YouTubeControlsOverlayFingerprint.resultOrThrow().mutableClass
        )
        MotionEventFingerprint.resultOrThrow().mutableMethod.apply {
            val insertIndex = getTargetIndexWithMethodReferenceNameOrThrow("setTranslationY") + 1

            addInstruction(
                insertIndex,
                "invoke-static {}, $INTEGRATIONS_CLASS_DESCRIPTOR->changeVisibilityNegatedImmediate()V"
            )
        }

        // endregion

        // region patch initialize of overlay button or SponsorBlock button

        mapOf(
            BottomControlsInflateFingerprint to "initializeBottomControlButton",
            ControlsLayoutInflateFingerprint to "initializeTopControlButton"
        ).forEach { (fingerprint, methodName) ->
            fingerprint.resultOrThrow().let {
                it.mutableMethod.apply {
                    val endIndex = it.scanResult.patternScanResult!!.endIndex
                    val viewRegister = getInstruction<OneRegisterInstruction>(endIndex).registerA

                    addInstruction(
                        endIndex + 1,
                        "invoke-static {v$viewRegister}, $INTEGRATIONS_CLASS_DESCRIPTOR->$methodName(Landroid/view/View;)V"
                    )
                }
            }
        }

        // endregion

        // region set methods to inject into integration

        val playerControlsMutableClass =
            context.findClass(INTEGRATIONS_CLASS_DESCRIPTOR)!!.mutableClass

        changeVisibilityMethod =
            playerControlsMutableClass.methods.single { method ->
                method.name == "changeVisibility"
                        && method.parameters == listOf("Z", "Z")
            }

        changeVisibilityNegatedImmediatelyMethod =
            playerControlsMutableClass.methods.single { method ->
                method.name == "changeVisibilityNegatedImmediately"
            }

        initializeBottomControlButtonMethod =
            playerControlsMutableClass.methods.single { method ->
                method.name == "initializeBottomControlButton"
            }

        initializeTopControlButtonMethod =
            playerControlsMutableClass.methods.single { method ->
                method.name == "initializeTopControlButton"
            }

        // endregion

    }

    private fun MutableMethod.initializeHook(classDescriptor: String) =
        addInstruction(
            0,
            "invoke-static {p0}, $classDescriptor->initialize(Landroid/view/View;)V"
        )

    private fun changeVisibilityHook(classDescriptor: String) =
        changeVisibilityMethod.addInstruction(
            0,
            "invoke-static {p0, p1}, $classDescriptor->changeVisibility(ZZ)V"
        )

    private fun changeVisibilityNegatedImmediateHook(classDescriptor: String) =
        changeVisibilityNegatedImmediatelyMethod.addInstruction(
            0,
            "invoke-static {}, $classDescriptor->changeVisibilityNegatedImmediate()V"
        )

    internal fun hookBottomControlButton(classDescriptor: String) {
        initializeBottomControlButtonMethod.initializeHook(classDescriptor)
        changeVisibilityHook(classDescriptor)
        changeVisibilityNegatedImmediateHook(classDescriptor)
    }

    internal fun hookTopControlButton(classDescriptor: String) {
        initializeTopControlButtonMethod.initializeHook(classDescriptor)
        changeVisibilityHook(classDescriptor)
        changeVisibilityNegatedImmediateHook(classDescriptor)
    }
}