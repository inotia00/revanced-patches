package app.revanced.patches.music.general.components

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.extensions.InstructionExtensions.replaceInstruction
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.music.general.components.fingerprints.ChipCloudFingerprint
import app.revanced.patches.music.general.components.fingerprints.ContentPillFingerprint
import app.revanced.patches.music.general.components.fingerprints.FloatingButtonFingerprint
import app.revanced.patches.music.general.components.fingerprints.FloatingButtonParentFingerprint
import app.revanced.patches.music.general.components.fingerprints.HistoryMenuItemFingerprint
import app.revanced.patches.music.general.components.fingerprints.HistoryMenuItemOfflineTabFingerprint
import app.revanced.patches.music.general.components.fingerprints.MediaRouteButtonFingerprint
import app.revanced.patches.music.general.components.fingerprints.PlayerOverlayChipFingerprint
import app.revanced.patches.music.general.components.fingerprints.SearchBarFingerprint
import app.revanced.patches.music.general.components.fingerprints.SearchBarParentFingerprint
import app.revanced.patches.music.general.components.fingerprints.SoundSearchFingerprint
import app.revanced.patches.music.general.components.fingerprints.TasteBuilderConstructorFingerprint
import app.revanced.patches.music.general.components.fingerprints.TasteBuilderSyntheticFingerprint
import app.revanced.patches.music.general.components.fingerprints.TooltipContentViewFingerprint
import app.revanced.patches.music.general.components.fingerprints.TopBarMenuItemImageViewFingerprint
import app.revanced.patches.music.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.music.utils.integrations.Constants.COMPONENTS_PATH
import app.revanced.patches.music.utils.integrations.Constants.GENERAL_CLASS_DESCRIPTOR
import app.revanced.patches.music.utils.resourceid.SharedResourceIdPatch
import app.revanced.patches.music.utils.resourceid.SharedResourceIdPatch.MusicTasteBuilderShelf
import app.revanced.patches.music.utils.resourceid.SharedResourceIdPatch.PlayerOverlayChip
import app.revanced.patches.music.utils.resourceid.SharedResourceIdPatch.TopBarMenuItemImageView
import app.revanced.patches.music.utils.settings.CategoryType
import app.revanced.patches.music.utils.settings.SettingsPatch
import app.revanced.patches.shared.litho.LithoFilterPatch
import app.revanced.patches.shared.settingmenu.SettingsMenuPatch
import app.revanced.util.alsoResolve
import app.revanced.util.indexOfFirstInstructionOrThrow
import app.revanced.util.indexOfFirstWideLiteralInstructionValueOrThrow
import app.revanced.util.injectLiteralInstructionBooleanCall
import app.revanced.util.patch.BaseBytecodePatch
import app.revanced.util.resultOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@Suppress("unused")
object LayoutComponentsPatch : BaseBytecodePatch(
    name = "Hide layout components",
    description = "Adds options to hide general layout components.",
    dependencies = setOf(
        LithoFilterPatch::class,
        SharedResourceIdPatch::class,
        SettingsMenuPatch::class,
        SettingsPatch::class
    ),
    compatiblePackages = COMPATIBLE_PACKAGE,
    fingerprints = setOf(
        ChipCloudFingerprint,
        ContentPillFingerprint,
        FloatingButtonParentFingerprint,
        HistoryMenuItemFingerprint,
        HistoryMenuItemOfflineTabFingerprint,
        MediaRouteButtonFingerprint,
        PlayerOverlayChipFingerprint,
        SearchBarParentFingerprint,
        SoundSearchFingerprint,
        TasteBuilderConstructorFingerprint,
        TooltipContentViewFingerprint,
        TopBarMenuItemImageViewFingerprint
    )
) {
    private const val CUSTOM_FILTER_CLASS_DESCRIPTOR =
        "$COMPONENTS_PATH/CustomFilter;"

    private const val LAYOUT_COMPONENTS_FILTER_CLASS_DESCRIPTOR =
        "$COMPONENTS_PATH/LayoutComponentsFilter;"

    override fun execute(context: BytecodeContext) {
        var notificationButtonIncluded = false
        var soundSearchButtonIncluded = false

        // region patch for hide cast button

        // hide cast button
        MediaRouteButtonFingerprint.resultOrThrow().let {
            val setVisibilityMethod =
                it.mutableClass.methods.find { method -> method.name == "setVisibility" }

            setVisibilityMethod?.apply {
                addInstructions(
                    0, """
                        invoke-static {p1}, $GENERAL_CLASS_DESCRIPTOR->hideCastButton(I)I
                        move-result p1
                        """
                )
            } ?: throw PatchException("Failed to find setVisibility method")
        }

        // hide floating cast banner
        PlayerOverlayChipFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val targetIndex =
                    indexOfFirstWideLiteralInstructionValueOrThrow(PlayerOverlayChip) + 2
                val targetRegister = getInstruction<OneRegisterInstruction>(targetIndex).registerA

                addInstruction(
                    targetIndex + 1,
                    "invoke-static {v$targetRegister}, $GENERAL_CLASS_DESCRIPTOR->hideCastButton(Landroid/view/View;)V"
                )
            }
        }

        // endregion

        // region patch for hide category bar

        ChipCloudFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val targetIndex = it.scanResult.patternScanResult!!.endIndex
                val targetRegister = getInstruction<OneRegisterInstruction>(targetIndex).registerA

                addInstruction(
                    targetIndex + 1,
                    "invoke-static { v$targetRegister }, $GENERAL_CLASS_DESCRIPTOR->hideCategoryBar(Landroid/view/View;)V"
                )
            }
        }

        // endregion

        // region patch for hide floating button

        FloatingButtonFingerprint.resolve(
            context,
            FloatingButtonParentFingerprint.resultOrThrow().classDef
        )
        FloatingButtonFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                addInstructionsWithLabels(
                    1, """
                        invoke-static {}, $GENERAL_CLASS_DESCRIPTOR->hideFloatingButton()Z
                        move-result v0
                        if-eqz v0, :show
                        return-void
                        """, ExternalLabel("show", getInstruction(1))
                )
            }
        }

        // endregion

        // region patch for hide history button

        arrayOf(
            HistoryMenuItemFingerprint,
            HistoryMenuItemOfflineTabFingerprint
        ).forEach { fingerprint ->
            fingerprint.resultOrThrow().let {
                it.mutableMethod.apply {
                    val insertIndex = it.scanResult.patternScanResult!!.startIndex
                    val insertRegister =
                        getInstruction<FiveRegisterInstruction>(insertIndex).registerD

                    addInstructions(
                        insertIndex, """
                            invoke-static {v$insertRegister}, $GENERAL_CLASS_DESCRIPTOR->hideHistoryButton(Z)Z
                            move-result v$insertRegister
                            """
                    )
                }
            }
        }

        // endregion

        // region patch for hide notification button

        if (SettingsPatch.upward0642) {
            TopBarMenuItemImageViewFingerprint.resultOrThrow().mutableMethod.apply {
                val constIndex =
                    indexOfFirstWideLiteralInstructionValueOrThrow(TopBarMenuItemImageView)
                val targetIndex =
                    indexOfFirstInstructionOrThrow(constIndex, Opcode.MOVE_RESULT_OBJECT)
                val targetRegister = getInstruction<OneRegisterInstruction>(targetIndex).registerA

                addInstruction(
                    targetIndex + 1,
                    "invoke-static {v$targetRegister}, $GENERAL_CLASS_DESCRIPTOR->hideNotificationButton(Landroid/view/View;)V"
                )
            }
            notificationButtonIncluded = true
        }

        // endregion

        // region patch for hide sound search button

        SoundSearchFingerprint.result?.let {
            SoundSearchFingerprint.injectLiteralInstructionBooleanCall(
                45625491,
                "$GENERAL_CLASS_DESCRIPTOR->hideSoundSearchButton(Z)Z"
            )
            soundSearchButtonIncluded = true
        }

        // endregion

        // region patch for hide tap to update button

        ContentPillFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                addInstructionsWithLabels(
                    0,
                    """
                        invoke-static {}, $GENERAL_CLASS_DESCRIPTOR->hideTapToUpdateButton()Z
                        move-result v0
                        if-eqz v0, :show
                        return-void
                        """, ExternalLabel("show", getInstruction(0))
                )
            }
        }

        // endregion

        // region patch for hide taste builder

        TasteBuilderConstructorFingerprint.resultOrThrow().let { parentResult ->
            TasteBuilderSyntheticFingerprint.resolve(context, parentResult.classDef)

            parentResult.mutableMethod.apply {
                val constIndex =
                    indexOfFirstWideLiteralInstructionValueOrThrow(MusicTasteBuilderShelf)
                val targetIndex =
                    indexOfFirstInstructionOrThrow(constIndex, Opcode.MOVE_RESULT_OBJECT)
                val targetRegister = getInstruction<OneRegisterInstruction>(targetIndex).registerA

                addInstruction(
                    targetIndex + 1,
                    "invoke-static {v$targetRegister}, $GENERAL_CLASS_DESCRIPTOR->hideTasteBuilder(Landroid/view/View;)V"
                )
            }
        }

        TasteBuilderSyntheticFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val insertIndex = it.scanResult.patternScanResult!!.startIndex
                val insertRegister = getInstruction<OneRegisterInstruction>(insertIndex).registerA

                addInstruction(
                    insertIndex,
                    "const/4 v$insertRegister, 0x0"
                )
            }
        }

        // endregion

        // region patch for hide tooltip content

        TooltipContentViewFingerprint.resultOrThrow().mutableMethod.addInstruction(
            0,
            "return-void"
        )

        // endregion

        // region patch for hide voice search button

        SearchBarFingerprint.alsoResolve(
            context, SearchBarParentFingerprint
        ).let {
            it.mutableMethod.apply {
                val setVisibilityIndex = SearchBarFingerprint.indexOfVisibilityInstruction(this)
                val setVisibilityInstruction =
                    getInstruction<FiveRegisterInstruction>(setVisibilityIndex)

                replaceInstruction(
                    setVisibilityIndex,
                    "invoke-static {v${setVisibilityInstruction.registerC}, v${setVisibilityInstruction.registerD}}, " +
                            "$GENERAL_CLASS_DESCRIPTOR->hideVoiceSearchButton(Landroid/widget/ImageView;I)V"
                )
            }
        }

        // endregion

        LithoFilterPatch.addFilter(CUSTOM_FILTER_CLASS_DESCRIPTOR)
        LithoFilterPatch.addFilter(LAYOUT_COMPONENTS_FILTER_CLASS_DESCRIPTOR)

        SettingsPatch.addSwitchPreference(
            CategoryType.GENERAL,
            "revanced_custom_filter",
            "false"
        )
        SettingsPatch.addPreferenceWithIntent(
            CategoryType.GENERAL,
            "revanced_custom_filter_strings",
            "revanced_custom_filter"
        )
        SettingsPatch.addSwitchPreference(
            CategoryType.GENERAL,
            "revanced_hide_settings_menu",
            "false"
        )
        SettingsPatch.addPreferenceWithIntent(
            CategoryType.GENERAL,
            "revanced_hide_settings_menu_filter_strings",
            "revanced_hide_settings_menu"
        )
        SettingsPatch.addSwitchPreference(
            CategoryType.GENERAL,
            "revanced_hide_button_shelf",
            "false"
        )
        SettingsPatch.addSwitchPreference(
            CategoryType.GENERAL,
            "revanced_hide_carousel_shelf",
            "false"
        )
        SettingsPatch.addSwitchPreference(
            CategoryType.GENERAL,
            "revanced_hide_playlist_card_shelf",
            "false"
        )
        SettingsPatch.addSwitchPreference(
            CategoryType.GENERAL,
            "revanced_hide_samples_shelf",
            "false"
        )
        SettingsPatch.addSwitchPreference(
            CategoryType.GENERAL,
            "revanced_hide_cast_button",
            "true"
        )
        SettingsPatch.addSwitchPreference(
            CategoryType.GENERAL,
            "revanced_hide_category_bar",
            "false"
        )
        SettingsPatch.addSwitchPreference(
            CategoryType.GENERAL,
            "revanced_hide_floating_button",
            "false"
        )
        SettingsPatch.addSwitchPreference(
            CategoryType.GENERAL,
            "revanced_hide_tap_to_update_button",
            "false"
        )
        SettingsPatch.addSwitchPreference(
            CategoryType.GENERAL,
            "revanced_hide_history_button",
            "false"
        )
        if (notificationButtonIncluded) {
            SettingsPatch.addSwitchPreference(
                CategoryType.GENERAL,
                "revanced_hide_notification_button",
                "false"
            )
        }
        if (soundSearchButtonIncluded) {
            SettingsPatch.addSwitchPreference(
                CategoryType.GENERAL,
                "revanced_hide_sound_search_button",
                "false"
            )
        }
        SettingsPatch.addSwitchPreference(
            CategoryType.GENERAL,
            "revanced_hide_voice_search_button",
            "false"
        )
    }
}
