package app.revanced.patches.youtube.misc.downloadactions

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.youtube.misc.downloadactions.fingerprints.*
import app.revanced.patches.youtube.utils.compatibility.Constants
import app.revanced.patches.youtube.utils.integrations.Constants.INTEGRATIONS_PATH
import app.revanced.patches.youtube.utils.integrations.Constants.MISC_PATH
import app.revanced.patches.youtube.utils.mainactivity.MainActivityResolvePatch
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstructionOrThrow
import app.revanced.util.patch.BaseBytecodePatch
import app.revanced.util.resultOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction35c
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

@Suppress("unused")
object HookDownloadActionsPatch : BaseBytecodePatch(
    name = "Hook download actions",
    description = "Adds options to show the download playlist button and hook the download actions.",
    dependencies = setOf(
        MainActivityResolvePatch::class,
        SharedResourceIdPatch::class,
        SettingsPatch::class
    ),
    compatiblePackages = Constants.COMPATIBLE_PACKAGE,
    fingerprints = setOf(
        OfflineVideoEndpointFingerprint,
        PiPPlaybackFingerprint,
        AccessibilityOfflineButtonSyncFingerprint,
        DownloadPlaylistButtonOnClickFingerprint
    )
) {
    private const val INTEGRATIONS_DOWNLOAD_PLAYLIST_BUTTON_CLASS_DESCRIPTOR =
        "$MISC_PATH/HookDownloadAction;"

    private const val INTEGRATIONS_VIDEO_UTILS_CLASS_DESCRIPTOR =
        "$INTEGRATIONS_PATH/utils/VideoUtils;"

    override fun execute(context: BytecodeContext) {

        // region Patch for hook download actions

        OfflineVideoEndpointFingerprint.resultOrThrow().mutableMethod.apply {
            addInstructionsWithLabels(
                0, """
                    invoke-static/range {p3 .. p3}, $INTEGRATIONS_VIDEO_UTILS_CLASS_DESCRIPTOR->inAppDownloadButtonOnClick(Ljava/lang/String;)Z
                    move-result v0
                    if-eqz v0, :show_native_downloader
                    return-void
                    """, ExternalLabel("show_native_downloader", getInstruction(0))
            )
        }

        PiPPlaybackFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val insertIndex = it.scanResult.patternScanResult!!.endIndex
                val insertRegister = getInstruction<OneRegisterInstruction>(insertIndex).registerA

                addInstructions(
                    insertIndex, """
                        invoke-static {v$insertRegister}, $INTEGRATIONS_VIDEO_UTILS_CLASS_DESCRIPTOR->getExternalDownloaderLaunchedState(Z)Z
                        move-result v$insertRegister
                        """
                )
            }
        }

        // endregion

        // region Force show the playlist download button

        AccessibilityOfflineButtonSyncFingerprint.resultOrThrow().let { parentResult ->
            SetPlaylistDownloadButtonVisibilityFingerprint.also {
                it.resolve(
                    context,
                    parentResult.classDef
                )
            }.resultOrThrow().let { setVisibilityMethod ->
                setVisibilityMethod.mutableMethod.apply {
                    // Find the index of if-nez
                    val insertIndex = setVisibilityMethod.scanResult.patternScanResult!!.startIndex + 2
                    // Get register values used in if-nez
                    val insertRegister = getInstruction<OneRegisterInstruction>(insertIndex).registerA

                    // Add instructions just above the index of if-nez
                    addInstructions(
                        insertIndex,
                        """
                        invoke-static {}, $INTEGRATIONS_DOWNLOAD_PLAYLIST_BUTTON_CLASS_DESCRIPTOR->isPlaylistDownloadButtonHooked()Z
                        move-result v$insertRegister
                        """
                    )
                }
            }
        }

        // endregion

        // region Hook Download Playlist Button OnClick method

        DownloadPlaylistButtonOnClickFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {

                // region Get the index of the instruction that initializes the onClickListener

                val onClickListenerInitializeIndex = indexOfFirstInstructionOrThrow {
                    val reference = ((this as? ReferenceInstruction)?.reference as? MethodReference)

                    opcode == Opcode.INVOKE_VIRTUAL_RANGE
                            && reference?.parameterTypes?.first() == "Ljava/lang/String;"
                }

                // endregion

                // region Get the class that contains the onClick method

                val onClickListenerInitializeReference =
                    getInstruction<ReferenceInstruction>(onClickListenerInitializeIndex).reference


                val onClickClass = context.findClass(
                    (onClickListenerInitializeReference as MethodReference).returnType
                )!!.mutableClass

                // endregion

                onClickClass.methods.find { method -> method.name == "onClick" }?.apply {

                    // region Get the index of playlist id

                    val insertIndex = implementation!!.instructions.indexOfFirst { instruction ->
                        instruction.opcode == Opcode.INVOKE_STATIC
                                && instruction.getReference<MethodReference>()?.name == "isEmpty"
                    }

                    val insertRegister = getInstruction<Instruction35c>(insertIndex).registerC

                    // endregion

                    addInstructions(
                        insertIndex,
                        """
                        invoke-static {v$insertRegister}, $INTEGRATIONS_DOWNLOAD_PLAYLIST_BUTTON_CLASS_DESCRIPTOR->startPlaylistDownloadActivity(Ljava/lang/String;)Ljava/lang/String;
                        move-result-object v$insertRegister
                        """.trimIndent()
                    )
                }
            }
        }

        // endregion

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE_SCREEN: GENERAL",
                "SETTINGS: HOOK_DOWNLOAD_ACTIONS"
            )
        )

        SettingsPatch.updatePatchStatus(this)
    }
}
