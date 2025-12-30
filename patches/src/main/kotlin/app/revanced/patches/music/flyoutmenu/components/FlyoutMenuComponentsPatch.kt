package app.revanced.patches.music.flyoutmenu.components

import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patcher.patch.resourcePatch
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.music.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.music.utils.extension.Constants.COMPONENTS_PATH
import app.revanced.patches.music.utils.extension.Constants.FLYOUT_CLASS_DESCRIPTOR
import app.revanced.patches.music.utils.flyoutmenu.flyoutMenuHookPatch
import app.revanced.patches.music.utils.patch.PatchList.FLYOUT_MENU_COMPONENTS
import app.revanced.patches.music.utils.playservice.is_6_36_or_greater
import app.revanced.patches.music.utils.playservice.versionCheckPatch
import app.revanced.patches.music.utils.resourceid.endButtonsContainer
import app.revanced.patches.music.utils.resourceid.sharedResourceIdPatch
import app.revanced.patches.music.utils.resourceid.trimSilenceSwitch
import app.revanced.patches.music.utils.settings.CategoryType
import app.revanced.patches.music.utils.settings.ResourceUtils.updatePatchStatus
import app.revanced.patches.music.utils.settings.addSwitchPreference
import app.revanced.patches.music.utils.settings.settingsPatch
import app.revanced.patches.music.utils.videotype.videoTypeHookPatch
import app.revanced.patches.music.video.information.videoInformationPatch
import app.revanced.patches.shared.litho.addLithoFilter
import app.revanced.patches.shared.litho.lithoFilterPatch
import app.revanced.util.ResourceGroup
import app.revanced.util.copyResources
import app.revanced.util.findMethodOrThrow
import app.revanced.util.fingerprint.injectLiteralInstructionBooleanCall
import app.revanced.util.fingerprint.matchOrThrow
import app.revanced.util.fingerprint.methodOrThrow
import app.revanced.util.fingerprint.resolvable
import app.revanced.util.getReference
import app.revanced.util.getWalkerMethod
import app.revanced.util.indexOfFirstInstructionOrThrow
import app.revanced.util.indexOfFirstLiteralInstructionOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private val resourceFileArray = arrayOf(
    "yt_outline_play_arrow_half_circle_black_24"
).map { "$it.png" }.toTypedArray()

private val flyoutMenuComponentsResourcePatch = resourcePatch(
    description = "flyoutMenuComponentsResourcePatch"
) {
    execute {
        arrayOf("xxxhdpi", "xxhdpi", "xhdpi", "hdpi", "mdpi")
            .map { "drawable-$it" }
            .map { directory ->
                ResourceGroup(
                    directory, *resourceFileArray
                )
            }
            .let { resourceGroups ->
                resourceGroups.forEach {
                    copyResources("music/flyout", it)
                }
            }
    }
}

private const val FILTER_CLASS_DESCRIPTOR =
    "$COMPONENTS_PATH/PlayerFlyoutMenuFilter;"

@Suppress("unused")
val flyoutMenuComponentsPatch = bytecodePatch(
    FLYOUT_MENU_COMPONENTS.title,
    FLYOUT_MENU_COMPONENTS.summary,
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(
        settingsPatch,
        flyoutMenuComponentsResourcePatch,
        flyoutMenuHookPatch,
        lithoFilterPatch,
        sharedResourceIdPatch,
        versionCheckPatch,
        videoInformationPatch,
        videoTypeHookPatch,
    )

    execute {
        var trimSilenceIncluded = false

        // region patch for disable trim silence

        if (trimSilenceConfigFingerprint.resolvable()) {
            trimSilenceConfigFingerprint.injectLiteralInstructionBooleanCall(
                TRIM_SILENCE_FEATURE_FLAG,
                "$FLYOUT_CLASS_DESCRIPTOR->disableTrimSilence(Z)Z"
            )

            trimSilenceSwitchFingerprint.methodOrThrow().apply {
                val constIndex =
                    indexOfFirstLiteralInstructionOrThrow(trimSilenceSwitch)
                val onCheckedChangedListenerIndex =
                    indexOfFirstInstructionOrThrow(constIndex, Opcode.INVOKE_DIRECT)
                val onCheckedChangedListenerReference =
                    getInstruction<ReferenceInstruction>(onCheckedChangedListenerIndex).reference
                val onCheckedChangedListenerDefiningClass =
                    (onCheckedChangedListenerReference as MethodReference).definingClass

                findMethodOrThrow(onCheckedChangedListenerDefiningClass) {
                    name == "onCheckedChanged"
                }.apply {
                    val onCheckedChangedWalkerIndex =
                        indexOfFirstInstructionOrThrow {
                            val reference = getReference<MethodReference>()
                            opcode == Opcode.INVOKE_VIRTUAL &&
                                    reference?.returnType == "V" &&
                                    reference.parameterTypes.size == 1 &&
                                    reference.parameterTypes[0] == "Z"
                        }

                    getWalkerMethod(onCheckedChangedWalkerIndex).apply {
                        val insertIndex = indexOfFirstInstructionOrThrow(Opcode.MOVE_RESULT)
                        val insertRegister =
                            getInstruction<OneRegisterInstruction>(insertIndex).registerA

                        addInstructions(
                            insertIndex + 1, """
                                invoke-static {v$insertRegister}, $FLYOUT_CLASS_DESCRIPTOR->disableTrimSilenceSwitch(Z)Z
                                move-result v$insertRegister
                                """
                        )
                    }
                }
            }
            trimSilenceIncluded = true
        }

        // endregion

        // region patch for enable compact dialog

        screenWidthFingerprint.matchOrThrow(screenWidthParentFingerprint).let {
            it.method.apply {
                val index = it.patternMatch!!.startIndex
                val register = getInstruction<TwoRegisterInstruction>(index).registerA

                addInstructions(
                    index, """
                        invoke-static {v$register}, $FLYOUT_CLASS_DESCRIPTOR->enableCompactDialog(I)I
                        move-result v$register
                        """
                )
            }
        }

        // endregion

        // region patch for hide flyout menu components and replace menu

        menuItemFingerprint.matchOrThrow().let {
            it.method.apply {
                val freeIndex = indexOfFirstInstructionOrThrow(Opcode.OR_INT_LIT16)
                val textViewIndex = it.patternMatch!!.startIndex
                val imageViewIndex = it.patternMatch!!.endIndex

                val freeRegister =
                    getInstruction<TwoRegisterInstruction>(freeIndex).registerA
                val textViewRegister =
                    getInstruction<OneRegisterInstruction>(textViewIndex).registerA
                val imageViewRegister =
                    getInstruction<OneRegisterInstruction>(imageViewIndex).registerA

                val enumIndex = indexOfFirstInstructionOrThrow {
                    opcode == Opcode.INVOKE_STATIC
                            && (this as? ReferenceInstruction)?.reference.toString()
                        .contains("(I)L")
                } + 1
                val enumRegister = getInstruction<OneRegisterInstruction>(enumIndex).registerA

                addInstructionsWithLabels(
                    enumIndex + 1,
                    """
                        invoke-static {v$enumRegister, v$textViewRegister, v$imageViewRegister}, $FLYOUT_CLASS_DESCRIPTOR->replaceComponents(Ljava/lang/Enum;Landroid/widget/TextView;Landroid/widget/ImageView;)V
                        invoke-static {v$enumRegister}, $FLYOUT_CLASS_DESCRIPTOR->hideComponents(Ljava/lang/Enum;)Z
                        move-result v$freeRegister
                        if-nez v$freeRegister, :hide
                        """,
                    ExternalLabel("hide", getInstruction(implementation!!.instructions.lastIndex))
                )
            }
        }

        touchOutsideFingerprint.methodOrThrow().apply {
            val setOnClickListenerIndex = indexOfFirstInstructionOrThrow {
                opcode == Opcode.INVOKE_VIRTUAL &&
                        getReference<MethodReference>()?.name == "setOnClickListener"
            }
            val setOnClickListenerRegister =
                getInstruction<FiveRegisterInstruction>(setOnClickListenerIndex).registerC

            addInstruction(
                setOnClickListenerIndex + 1,
                "invoke-static {v$setOnClickListenerRegister}, $FLYOUT_CLASS_DESCRIPTOR->setTouchOutSideView(Landroid/view/View;)V"
            )
        }

        endButtonsContainerFingerprint.methodOrThrow().apply {
            val startIndex =
                indexOfFirstLiteralInstructionOrThrow(endButtonsContainer)
            val targetIndex =
                indexOfFirstInstructionOrThrow(startIndex, Opcode.MOVE_RESULT_OBJECT)
            val targetRegister = getInstruction<OneRegisterInstruction>(targetIndex).registerA

            addInstruction(
                targetIndex + 1,
                "invoke-static {v$targetRegister}, $FLYOUT_CLASS_DESCRIPTOR->hideLikeDislikeContainer(Landroid/view/View;)V"
            )
        }

        // endregion

        // region patch for enable sleep timer

        /**
         * Forces sleep timer menu to be enabled.
         * This method may be desperate in the future.
         */
        if (sleepTimerFingerprint.resolvable()) {
            sleepTimerFingerprint.methodOrThrow().apply {
                val insertIndex = implementation!!.instructions.lastIndex
                val targetRegister = getInstruction<OneRegisterInstruction>(insertIndex).registerA

                addInstruction(
                    insertIndex,
                    "const/4 v$targetRegister, 0x1"
                )
            }
        }

        // endregion

        addLithoFilter(FILTER_CLASS_DESCRIPTOR)

        if (trimSilenceIncluded) {
            addSwitchPreference(
                CategoryType.FLYOUT,
                "revanced_disable_trim_silence",
                "true"
            )
        }
        addSwitchPreference(
            CategoryType.FLYOUT,
            "revanced_enable_compact_dialog",
            "true"
        )
        addSwitchPreference(
            CategoryType.FLYOUT,
            "revanced_hide_flyout_menu_like_dislike",
            "false",
            false
        )
        if (is_6_36_or_greater) {
            addSwitchPreference(
                CategoryType.FLYOUT,
                "revanced_hide_flyout_menu_3_column_component",
                "false",
                false
            )
        }
        addSwitchPreference(
            CategoryType.FLYOUT,
            "revanced_hide_flyout_menu_add_to_queue",
            "false",
            false
        )
        addSwitchPreference(
            CategoryType.FLYOUT,
            "revanced_hide_flyout_menu_captions",
            "false",
            false
        )
        addSwitchPreference(
            CategoryType.FLYOUT,
            "revanced_hide_flyout_menu_delete_playlist",
            "false",
            false
        )
        addSwitchPreference(
            CategoryType.FLYOUT,
            "revanced_hide_flyout_menu_dismiss_queue",
            "false",
            false
        )
        addSwitchPreference(
            CategoryType.FLYOUT,
            "revanced_hide_flyout_menu_download",
            "false",
            false
        )
        addSwitchPreference(
            CategoryType.FLYOUT,
            "revanced_hide_flyout_menu_edit_playlist",
            "false",
            false
        )
        addSwitchPreference(
            CategoryType.FLYOUT,
            "revanced_hide_flyout_menu_go_to_album",
            "false",
            false
        )
        addSwitchPreference(
            CategoryType.FLYOUT,
            "revanced_hide_flyout_menu_go_to_artist",
            "false",
            false
        )
        addSwitchPreference(
            CategoryType.FLYOUT,
            "revanced_hide_flyout_menu_go_to_episode",
            "false",
            false
        )
        addSwitchPreference(
            CategoryType.FLYOUT,
            "revanced_hide_flyout_menu_go_to_podcast",
            "false",
            false
        )
        addSwitchPreference(
            CategoryType.FLYOUT,
            "revanced_hide_flyout_menu_help",
            "false",
            false
        )
        addSwitchPreference(
            CategoryType.FLYOUT,
            "revanced_hide_flyout_menu_not_interested",
            "false",
            false
        )
        addSwitchPreference(
            CategoryType.FLYOUT,
            "revanced_hide_flyout_menu_pin_to_speed_dial",
            "false",
            false
        )
        addSwitchPreference(
            CategoryType.FLYOUT,
            "revanced_hide_flyout_menu_play_next",
            "false",
            false
        )
        addSwitchPreference(
            CategoryType.FLYOUT,
            "revanced_hide_flyout_menu_quality",
            "false",
            false
        )
        addSwitchPreference(
            CategoryType.FLYOUT,
            "revanced_hide_flyout_menu_remove_from_library",
            "false",
            false
        )
        addSwitchPreference(
            CategoryType.FLYOUT,
            "revanced_hide_flyout_menu_remove_from_playlist",
            "false",
            false
        )
        addSwitchPreference(
            CategoryType.FLYOUT,
            "revanced_hide_flyout_menu_report",
            "false",
            false
        )
        addSwitchPreference(
            CategoryType.FLYOUT,
            "revanced_hide_flyout_menu_save_episode_for_later_save_to_library",
            "false",
            false
        )
        addSwitchPreference(
            CategoryType.FLYOUT,
            "revanced_hide_flyout_menu_save_to_playlist",
            "false",
            false
        )
        addSwitchPreference(
            CategoryType.FLYOUT,
            "revanced_hide_flyout_menu_share",
            "false",
            false
        )
        addSwitchPreference(
            CategoryType.FLYOUT,
            "revanced_hide_flyout_menu_shuffle_play",
            "false",
            false
        )
        addSwitchPreference(
            CategoryType.FLYOUT,
            "revanced_hide_flyout_menu_sleep_timer",
            "false",
            false
        )
        addSwitchPreference(
            CategoryType.FLYOUT,
            "revanced_hide_flyout_menu_start_radio",
            "false",
            false
        )
        addSwitchPreference(
            CategoryType.FLYOUT,
            "revanced_hide_flyout_menu_stats_for_nerds",
            "false",
            false
        )
        addSwitchPreference(
            CategoryType.FLYOUT,
            "revanced_hide_flyout_menu_subscribe",
            "false",
            false
        )
        addSwitchPreference(
            CategoryType.FLYOUT,
            "revanced_hide_flyout_menu_taste_match",
            "false",
            false
        )
        addSwitchPreference(
            CategoryType.FLYOUT,
            "revanced_hide_flyout_menu_unpin_from_speed_dial",
            "false",
            false
        )
        addSwitchPreference(
            CategoryType.FLYOUT,
            "revanced_hide_flyout_menu_view_song_credit",
            "false",
            false
        )
        addSwitchPreference(
            CategoryType.FLYOUT,
            "revanced_replace_flyout_menu_dismiss_queue",
            "false"
        )
        addSwitchPreference(
            CategoryType.FLYOUT,
            "revanced_replace_flyout_menu_dismiss_queue_continue_watch",
            "true",
            "revanced_replace_flyout_menu_dismiss_queue"
        )
        addSwitchPreference(
            CategoryType.FLYOUT,
            "revanced_replace_flyout_menu_report",
            "true"
        )
        addSwitchPreference(
            CategoryType.FLYOUT,
            "revanced_replace_flyout_menu_report_only_player",
            "true",
            "revanced_replace_flyout_menu_report"
        )

        updatePatchStatus(FLYOUT_MENU_COMPONENTS)

    }
}
