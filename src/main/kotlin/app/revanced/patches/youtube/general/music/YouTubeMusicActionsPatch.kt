package app.revanced.patches.youtube.general.music

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.extensions.InstructionExtensions.replaceInstruction
import app.revanced.patches.youtube.general.music.fingerprints.AppDeepLinkFingerprint
import app.revanced.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.gms.GmsCoreSupportResourcePatch.PackageNameYouTubeMusic
import app.revanced.patches.youtube.utils.integrations.Constants.GENERAL_PATH
import app.revanced.patches.youtube.utils.settings.SettingsBytecodePatch
import app.revanced.patches.youtube.utils.settings.SettingsPatch
import app.revanced.util.addEntryValues
import app.revanced.util.findMethodOrThrow
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstructionOrThrow
import app.revanced.util.patch.BaseBytecodePatch
import app.revanced.util.resultOrThrow
import app.revanced.util.valueOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import java.io.Closeable

@Suppress("unused")
object YouTubeMusicActionsPatch : BaseBytecodePatch(
    name = "Hook YouTube Music actions",
    description = "Adds support for opening music in RVX Music using the in-app YouTube Music button.",
    dependencies = setOf(SettingsPatch::class),
    compatiblePackages = COMPATIBLE_PACKAGE,
    fingerprints = setOf(AppDeepLinkFingerprint)
), Closeable {
    private const val INTEGRATIONS_CLASS_DESCRIPTOR =
        "$GENERAL_PATH/YouTubeMusicActionsPatch;"

    override fun execute(context: BytecodeContext) {

        AppDeepLinkFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val packageNameIndex = it.scanResult.patternScanResult!!.startIndex
                val packageNameField =
                    getInstruction<ReferenceInstruction>(packageNameIndex).reference.toString()

                implementation!!.instructions
                    .withIndex()
                    .filter { (_, instruction) ->
                        instruction.opcode == Opcode.IGET_OBJECT &&
                                instruction.getReference<FieldReference>()
                                    ?.toString() == packageNameField
                    }
                    .map { (index, _) -> index }
                    .reversed()
                    .forEach { index ->
                        val register = getInstruction<TwoRegisterInstruction>(index).registerA

                        addInstructions(
                            index + 1, """
                                invoke-static {v$register}, $INTEGRATIONS_CLASS_DESCRIPTOR->overridePackageName(Ljava/lang/String;)Ljava/lang/String;
                                move-result-object v$register
                                """
                        )
                    }
            }
        }

        /**
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE_SCREEN: GENERAL",
                "SETTINGS: HOOK_BUTTONS",
                "SETTINGS: HOOK_YOUTUBE_MUSIC_ACTIONS"
            )
        )

        SettingsPatch.updatePatchStatus(this)
    }

    override fun close() {
        if (SettingsPatch.containsPatch("GmsCore support")) {
            val musicPackageName = PackageNameYouTubeMusic.valueOrThrow()
            SettingsPatch.contexts.addEntryValues(
                "revanced_third_party_youtube_music_label",
                "RVX Music"
            )
            SettingsPatch.contexts.addEntryValues(
                "revanced_third_party_youtube_music_package_name",
                musicPackageName
            )

            SettingsBytecodePatch.contexts.findMethodOrThrow(INTEGRATIONS_CLASS_DESCRIPTOR) {
                name == "getRVXMusicPackageName"
            }.apply {
                val replaceIndex = indexOfFirstInstructionOrThrow(Opcode.CONST_STRING)
                val replaceRegister =
                    getInstruction<OneRegisterInstruction>(replaceIndex).registerA

                replaceInstruction(
                    replaceIndex,
                    "const-string v$replaceRegister, \"$musicPackageName\""
                )
            }
        }

    }
}