package app.revanced.patches.music.player.components

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.extensions.InstructionExtensions.removeInstruction
import app.revanced.patcher.extensions.or
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.util.proxy.mutableTypes.MutableField.Companion.toMutable
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.music.player.components.fingerprints.AudioVideoSwitchToggleFingerprint
import app.revanced.patches.music.player.components.fingerprints.EngagementPanelHeightFingerprint
import app.revanced.patches.music.player.components.fingerprints.EngagementPanelHeightParentFingerprint
import app.revanced.patches.music.player.components.fingerprints.HandleSearchRenderedFingerprint
import app.revanced.patches.music.player.components.fingerprints.HandleSignInEventFingerprint
import app.revanced.patches.music.player.components.fingerprints.InteractionLoggingEnumFingerprint
import app.revanced.patches.music.player.components.fingerprints.MiniPlayerConstructorFingerprint
import app.revanced.patches.music.player.components.fingerprints.MiniPlayerDefaultTextFingerprint
import app.revanced.patches.music.player.components.fingerprints.MiniPlayerDefaultViewVisibilityFingerprint
import app.revanced.patches.music.player.components.fingerprints.MiniPlayerParentFingerprint
import app.revanced.patches.music.player.components.fingerprints.MinimizedPlayerFingerprint
import app.revanced.patches.music.player.components.fingerprints.MppWatchWhileLayoutFingerprint
import app.revanced.patches.music.player.components.fingerprints.MusicActivityWidgetFingerprint
import app.revanced.patches.music.player.components.fingerprints.MusicPlaybackControlsFingerprint
import app.revanced.patches.music.player.components.fingerprints.NextButtonVisibilityFingerprint
import app.revanced.patches.music.player.components.fingerprints.OldEngagementPanelFingerprint
import app.revanced.patches.music.player.components.fingerprints.OldPlayerBackgroundFingerprint
import app.revanced.patches.music.player.components.fingerprints.OldPlayerLayoutFingerprint
import app.revanced.patches.music.player.components.fingerprints.PlayerPatchConstructorFingerprint
import app.revanced.patches.music.player.components.fingerprints.PlayerViewPagerConstructorFingerprint
import app.revanced.patches.music.player.components.fingerprints.QuickSeekOverlayFingerprint
import app.revanced.patches.music.player.components.fingerprints.RemixGenericButtonFingerprint
import app.revanced.patches.music.player.components.fingerprints.RepeatTrackFingerprint
import app.revanced.patches.music.player.components.fingerprints.ShuffleOnClickFingerprint
import app.revanced.patches.music.player.components.fingerprints.SwipeToCloseFingerprint
import app.revanced.patches.music.player.components.fingerprints.SwitchToggleColorFingerprint
import app.revanced.patches.music.player.components.fingerprints.ZenModeFingerprint
import app.revanced.patches.music.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.music.utils.fingerprints.PendingIntentReceiverFingerprint
import app.revanced.patches.music.utils.integrations.Constants.COMPONENTS_PATH
import app.revanced.patches.music.utils.integrations.Constants.INTEGRATIONS_PATH
import app.revanced.patches.music.utils.integrations.Constants.PLAYER_CLASS_DESCRIPTOR
import app.revanced.patches.music.utils.mainactivity.MainActivityResolvePatch
import app.revanced.patches.music.utils.resourceid.SharedResourceIdPatch
import app.revanced.patches.music.utils.resourceid.SharedResourceIdPatch.AudioVideoSwitchToggle
import app.revanced.patches.music.utils.resourceid.SharedResourceIdPatch.ColorGrey
import app.revanced.patches.music.utils.resourceid.SharedResourceIdPatch.DarkBackground
import app.revanced.patches.music.utils.resourceid.SharedResourceIdPatch.MiniPlayerPlayPauseReplayButton
import app.revanced.patches.music.utils.resourceid.SharedResourceIdPatch.MiniPlayerViewPager
import app.revanced.patches.music.utils.resourceid.SharedResourceIdPatch.PlayerViewPager
import app.revanced.patches.music.utils.resourceid.SharedResourceIdPatch.TapBloomView
import app.revanced.patches.music.utils.resourceid.SharedResourceIdPatch.TopEnd
import app.revanced.patches.music.utils.resourceid.SharedResourceIdPatch.TopStart
import app.revanced.patches.music.utils.settings.CategoryType
import app.revanced.patches.music.utils.settings.SettingsPatch
import app.revanced.patches.music.utils.videotype.VideoTypeHookPatch
import app.revanced.patches.shared.litho.LithoFilterPatch
import app.revanced.util.REGISTER_TEMPLATE_REPLACEMENT
import app.revanced.util.addStaticFieldToIntegration
import app.revanced.util.alsoResolve
import app.revanced.util.findMethodOrThrow
import app.revanced.util.getReference
import app.revanced.util.getWalkerMethod
import app.revanced.util.indexOfFirstInstructionOrThrow
import app.revanced.util.indexOfFirstInstructionReversedOrThrow
import app.revanced.util.indexOfFirstStringInstructionOrThrow
import app.revanced.util.indexOfFirstWideLiteralInstructionValueOrThrow
import app.revanced.util.injectLiteralInstructionBooleanCall
import app.revanced.util.injectLiteralInstructionViewCall
import app.revanced.util.patch.BaseBytecodePatch
import app.revanced.util.resultOrThrow
import app.revanced.util.transformMethods
import app.revanced.util.traverseClassHierarchy
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.MethodParameter
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.Reference
import com.android.tools.smali.dexlib2.immutable.ImmutableField
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import kotlin.properties.Delegates

@Suppress("unused", "LocalVariableName")
object PlayerComponentsPatch : BaseBytecodePatch(
    name = "Player components",
    description = "Adds options to hide or change components related to the player.",
    dependencies = setOf(
        LithoFilterPatch::class,
        MainActivityResolvePatch::class,
        PlayerComponentsResourcePatch::class,
        SettingsPatch::class,
        SharedResourceIdPatch::class,
        VideoTypeHookPatch::class,
    ),
    compatiblePackages = COMPATIBLE_PACKAGE,
    fingerprints = setOf(
        AudioVideoSwitchToggleFingerprint,
        EngagementPanelHeightParentFingerprint,
        HandleSearchRenderedFingerprint,
        InteractionLoggingEnumFingerprint,
        MinimizedPlayerFingerprint,
        MiniPlayerConstructorFingerprint,
        MiniPlayerDefaultTextFingerprint,
        MiniPlayerDefaultViewVisibilityFingerprint,
        MiniPlayerParentFingerprint,
        MppWatchWhileLayoutFingerprint,
        MusicActivityWidgetFingerprint,
        MusicPlaybackControlsFingerprint,
        OldEngagementPanelFingerprint,
        OldPlayerBackgroundFingerprint,
        OldPlayerLayoutFingerprint,
        PendingIntentReceiverFingerprint,
        PlayerPatchConstructorFingerprint,
        PlayerViewPagerConstructorFingerprint,
        QuickSeekOverlayFingerprint,
        RemixGenericButtonFingerprint,
        RepeatTrackFingerprint,
        ShuffleOnClickFingerprint,
        SwipeToCloseFingerprint,
    )
) {
    private const val FILTER_CLASS_DESCRIPTOR =
        "$COMPONENTS_PATH/PlayerComponentsFilter;"

    private const val INTEGRATIONS_VIDEO_UTILS_CLASS_DESCRIPTOR =
        "$INTEGRATIONS_PATH/utils/VideoUtils;"

    override fun execute(context: BytecodeContext) {

        // region patch for disable gesture in player

        val playerViewPagerConstructorMethod =
            PlayerViewPagerConstructorFingerprint.resultOrThrow().mutableMethod
        val mainActivityOnStartMethod =
            MainActivityResolvePatch.getMethod("onStart")

        mapOf(
            MiniPlayerViewPager to "disableMiniPlayerGesture",
            PlayerViewPager to "disablePlayerGesture"
        ).forEach { (literal, methodName) ->
            val viewPagerReference = playerViewPagerConstructorMethod.let {
                val constIndex = it.indexOfFirstWideLiteralInstructionValueOrThrow(literal)
                val targetIndex = it.indexOfFirstInstructionOrThrow(constIndex, Opcode.IPUT_OBJECT)

                it.getInstruction<ReferenceInstruction>(targetIndex).reference.toString()
            }
            mainActivityOnStartMethod.apply {
                val insertIndex = indexOfFirstInstructionOrThrow {
                    opcode == Opcode.IGET_OBJECT
                            && getReference<FieldReference>()?.toString() == viewPagerReference
                }
                val insertRegister = getInstruction<TwoRegisterInstruction>(insertIndex).registerA
                val jumpIndex =
                    indexOfFirstInstructionOrThrow(insertIndex, Opcode.INVOKE_VIRTUAL) + 1

                addInstructionsWithLabels(
                    insertIndex, """
                        invoke-static {}, $PLAYER_CLASS_DESCRIPTOR->$methodName()Z
                        move-result v$insertRegister
                        if-nez v$insertRegister, :disable
                        """, ExternalLabel("disable", getInstruction(jumpIndex))
                )
            }
        }

        SettingsPatch.addSwitchPreference(
            CategoryType.PLAYER,
            "revanced_disable_mini_player_gesture",
            "false"
        )
        SettingsPatch.addSwitchPreference(
            CategoryType.PLAYER,
            "revanced_disable_player_gesture",
            "false"
        )

        // endregion

        // region patch for enable color match player and enable black player background

        lateinit var colorMathPlayerInvokeVirtualReference: Reference
        lateinit var colorMathPlayerIGetReference: Reference
        lateinit var colorMathPlayerIPutReference: Reference
        lateinit var colorMathPlayerMethodParameter: List<MethodParameter>

        MiniPlayerConstructorFingerprint.resultOrThrow().let { parentResult ->
            // Resolves fingerprints
            SwitchToggleColorFingerprint.resolve(context, parentResult.classDef)

            SwitchToggleColorFingerprint.resultOrThrow().let {
                it.mutableMethod.apply {
                    colorMathPlayerMethodParameter = parameters

                    val relativeIndex = it.scanResult.patternScanResult!!.endIndex + 1
                    val invokeVirtualIndex =
                        indexOfFirstInstructionOrThrow(relativeIndex, Opcode.INVOKE_VIRTUAL)
                    val iGetIndex = indexOfFirstInstructionOrThrow(relativeIndex, Opcode.IGET)

                    colorMathPlayerInvokeVirtualReference =
                        getInstruction<ReferenceInstruction>(invokeVirtualIndex).reference
                    colorMathPlayerIGetReference =
                        getInstruction<ReferenceInstruction>(iGetIndex).reference

                    // black player background
                    val invokeDirectIndex = indexOfFirstInstructionOrThrow(Opcode.INVOKE_DIRECT)
                    val targetMethod = getWalkerMethod(context, invokeDirectIndex)

                    targetMethod.apply {
                        val insertIndex = indexOfFirstInstructionOrThrow(Opcode.IF_NE)

                        addInstructions(
                            insertIndex, """
                            invoke-static {p1}, $PLAYER_CLASS_DESCRIPTOR->enableBlackPlayerBackground(I)I
                            move-result p1
                            invoke-static {p2}, $PLAYER_CLASS_DESCRIPTOR->enableBlackPlayerBackground(I)I
                            move-result p2
                            """
                        )
                    }
                }

                parentResult.mutableMethod.apply {
                    val colorGreyIndex = indexOfFirstWideLiteralInstructionValueOrThrow(ColorGrey)
                    val iPutIndex = indexOfFirstInstructionOrThrow(colorGreyIndex, Opcode.IPUT)

                    colorMathPlayerIPutReference =
                        getInstruction<ReferenceInstruction>(iPutIndex).reference
                }

                parentResult.mutableClass.methods.filter { method ->
                    method.accessFlags == AccessFlags.PUBLIC or AccessFlags.FINAL
                            && method.parameters == colorMathPlayerMethodParameter
                            && method.returnType == "V"
                }.forEach { mutableMethod ->
                    mutableMethod.apply {
                        val freeRegister = implementation!!.registerCount - parameters.size - 3

                        val invokeDirectIndex =
                            indexOfFirstInstructionReversedOrThrow(Opcode.INVOKE_DIRECT)
                        val invokeDirectReference =
                            getInstruction<ReferenceInstruction>(invokeDirectIndex).reference

                        addInstructionsWithLabels(
                            invokeDirectIndex + 1, """
                                invoke-static {}, $PLAYER_CLASS_DESCRIPTOR->enableColorMatchPlayer()Z
                                move-result v$freeRegister
                                if-eqz v$freeRegister, :off
                                invoke-virtual {p1}, $colorMathPlayerInvokeVirtualReference
                                move-result-object v$freeRegister
                                check-cast v$freeRegister, ${(colorMathPlayerIGetReference as FieldReference).definingClass}
                                iget v$freeRegister, v$freeRegister, $colorMathPlayerIGetReference
                                iput v$freeRegister, p0, $colorMathPlayerIPutReference
                                :off
                                invoke-direct {p0}, $invokeDirectReference
                                """
                        )
                        removeInstruction(invokeDirectIndex)
                    }
                }
            }
        }

        SettingsPatch.addSwitchPreference(
            CategoryType.PLAYER,
            "revanced_enable_black_player_background",
            "false"
        )
        SettingsPatch.addSwitchPreference(
            CategoryType.PLAYER,
            "revanced_enable_color_match_player",
            "true"
        )

        // endregion

        // region patch for enable force minimized player

        MinimizedPlayerFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val insertIndex = it.scanResult.patternScanResult!!.endIndex
                val insertRegister = getInstruction<OneRegisterInstruction>(insertIndex).registerA

                addInstructions(
                    insertIndex, """
                        invoke-static {v$insertRegister}, $PLAYER_CLASS_DESCRIPTOR->enableForceMinimizedPlayer(Z)Z
                        move-result v$insertRegister
                        """
                )
            }
        }

        SettingsPatch.addSwitchPreference(
            CategoryType.PLAYER,
            "revanced_enable_force_minimized_player",
            "true"
        )

        // endregion

        // region patch for enable next previous button

        val NEXT_BUTTON_FIELD_NAME = "nextButton"
        val PREVIOUS_BUTTON_FIELD_NAME = "previousButton"
        val NEXT_BUTTON_CLASS_FIELD_NAME = "nextButtonClass"
        val PREVIOUS_BUTTON_CLASS_FIELD_NAME = "previousButtonClass"
        val NEXT_BUTTON_METHOD_NAME = "setNextButton"
        val PREVIOUS_BUTTON_METHOD_NAME = "setPreviousButton"
        val NEXT_BUTTON_ONCLICK_METHOD_NAME = "setNextButtonOnClickListener"
        val PREVIOUS_BUTTON_ONCLICK_METHOD_NAME = "setPreviousButtonOnClickListener"
        val NEXT_BUTTON_INTENT_STRING = "YTM Next"
        val PREVIOUS_BUTTON_INTENT_STRING = "YTM Previous"

        val miniPlayerConstructorMutableMethod =
            MiniPlayerConstructorFingerprint.resultOrThrow().mutableMethod

        val mppWatchWhileLayoutMutableMethod =
            MppWatchWhileLayoutFingerprint.resultOrThrow().mutableMethod

        val pendingIntentReceiverMutableMethod =
            PendingIntentReceiverFingerprint.resultOrThrow().mutableMethod

        if (!SettingsPatch.upward0642) {
            MiniPlayerParentFingerprint.resultOrThrow().let { parentResult ->
                // Resolves fingerprints
                NextButtonVisibilityFingerprint.resolve(context, parentResult.classDef)

                NextButtonVisibilityFingerprint.resultOrThrow().let {
                    it.mutableMethod.apply {
                        val targetIndex = it.scanResult.patternScanResult!!.startIndex + 1
                        val targetRegister =
                            getInstruction<OneRegisterInstruction>(targetIndex).registerA

                        addInstructions(
                            targetIndex + 1, """
                                invoke-static {v$targetRegister}, $PLAYER_CLASS_DESCRIPTOR->enableMiniPlayerNextButton(Z)Z
                                move-result v$targetRegister
                                """
                        )
                    }
                }
            }
        } else {
            miniPlayerConstructorMutableMethod.setInstanceFieldValue(
                NEXT_BUTTON_METHOD_NAME,
                TopStart
            )
            mppWatchWhileLayoutMutableMethod.setStaticFieldValue(NEXT_BUTTON_FIELD_NAME, TopStart)
            pendingIntentReceiverMutableMethod.setOnClickListener(
                context,
                NEXT_BUTTON_INTENT_STRING,
                NEXT_BUTTON_ONCLICK_METHOD_NAME,
                NEXT_BUTTON_CLASS_FIELD_NAME
            )
        }

        miniPlayerConstructorMutableMethod.setInstanceFieldValue(
            PREVIOUS_BUTTON_METHOD_NAME,
            TopEnd
        )
        mppWatchWhileLayoutMutableMethod.setStaticFieldValue(PREVIOUS_BUTTON_FIELD_NAME, TopEnd)
        pendingIntentReceiverMutableMethod.setOnClickListener(
            context,
            PREVIOUS_BUTTON_INTENT_STRING,
            PREVIOUS_BUTTON_ONCLICK_METHOD_NAME,
            PREVIOUS_BUTTON_CLASS_FIELD_NAME
        )

        mppWatchWhileLayoutMutableMethod.setViewArray()

        SettingsPatch.addSwitchPreference(
            CategoryType.PLAYER,
            "revanced_enable_mini_player_next_button",
            "true"
        )
        SettingsPatch.addSwitchPreference(
            CategoryType.PLAYER,
            "revanced_enable_mini_player_previous_button",
            "true"
        )

        // endregion

        // region patch for enable swipe to dismiss mini player

        var swipeToDismissWidgetIndex by Delegates.notNull<Int>()
        lateinit var swipeToDismissIGetObjectReference: Reference
        lateinit var swipeToDismissInvokeInterfacePrimaryReference: Reference
        lateinit var swipeToDismissCheckCastReference: Reference
        lateinit var swipeToDismissSGetObjectReference: Reference
        lateinit var swipeToDismissNewInstanceReference: Reference
        lateinit var swipeToDismissInvokeStaticReference: Reference
        lateinit var swipeToDismissInvokeDirectReference: Reference
        lateinit var swipeToDismissInvokeInterfaceSecondaryReference: Reference

        fun MutableMethod.getSwipeToDismissReference(
            opcode: Opcode,
            reversed: Boolean
        ): Reference {
            val targetIndex = if (reversed)
                indexOfFirstInstructionReversedOrThrow(swipeToDismissWidgetIndex, opcode)
            else
                indexOfFirstInstructionOrThrow(swipeToDismissWidgetIndex, opcode)

            return getInstruction<ReferenceInstruction>(targetIndex).reference
        }

        if (!SettingsPatch.upward0642) {
            SwipeToCloseFingerprint.resultOrThrow().let {
                it.mutableMethod.apply {
                    val insertIndex = implementation!!.instructions.lastIndex
                    val targetRegister =
                        getInstruction<OneRegisterInstruction>(insertIndex).registerA

                    addInstructions(
                        insertIndex, """
                            invoke-static {v$targetRegister}, $PLAYER_CLASS_DESCRIPTOR->enableSwipeToDismissMiniPlayer(Z)Z
                            move-result v$targetRegister
                            """
                    )
                }
            }
        } else {

            // region dismiss mini player by swiping down

            InteractionLoggingEnumFingerprint.resultOrThrow().let {
                it.mutableMethod.apply {
                    val stringIndex =
                        indexOfFirstStringInstructionOrThrow("INTERACTION_LOGGING_GESTURE_TYPE_SWIPE")
                    val sPutObjectIndex =
                        indexOfFirstInstructionOrThrow(stringIndex, Opcode.SPUT_OBJECT)

                    swipeToDismissSGetObjectReference =
                        getInstruction<ReferenceInstruction>(sPutObjectIndex).reference
                }
            }

            MusicActivityWidgetFingerprint.resultOrThrow().let {
                it.mutableMethod.apply {
                    swipeToDismissWidgetIndex =
                        indexOfFirstWideLiteralInstructionValueOrThrow(79500)

                    swipeToDismissIGetObjectReference =
                        getSwipeToDismissReference(Opcode.IGET_OBJECT, true)
                    swipeToDismissInvokeInterfacePrimaryReference =
                        getSwipeToDismissReference(Opcode.INVOKE_INTERFACE, true)
                    swipeToDismissCheckCastReference =
                        getSwipeToDismissReference(Opcode.CHECK_CAST, true)
                    swipeToDismissNewInstanceReference =
                        getSwipeToDismissReference(Opcode.NEW_INSTANCE, true)
                    swipeToDismissInvokeStaticReference =
                        getSwipeToDismissReference(Opcode.INVOKE_STATIC, false)
                    swipeToDismissInvokeDirectReference =
                        getSwipeToDismissReference(Opcode.INVOKE_DIRECT, false)
                    swipeToDismissInvokeInterfaceSecondaryReference =
                        getSwipeToDismissReference(Opcode.INVOKE_INTERFACE, false)
                }
            }

            HandleSearchRenderedFingerprint.resultOrThrow().let { parentResult ->
                // resolves fingerprints
                HandleSignInEventFingerprint.resolve(context, parentResult.classDef)

                HandleSignInEventFingerprint.resultOrThrow().let {
                    val dismissBehaviorMethod =
                        it.getWalkerMethod(context, it.scanResult.patternScanResult!!.startIndex)

                    dismissBehaviorMethod.apply {
                        val insertIndex = indexOfFirstInstructionOrThrow {
                            getReference<FieldReference>()?.type == "Ljava/util/concurrent/atomic/AtomicBoolean;"
                        }
                        val primaryRegister =
                            getInstruction<TwoRegisterInstruction>(insertIndex).registerB
                        val secondaryRegister = primaryRegister + 1
                        val tertiaryRegister = secondaryRegister + 1

                        val freeRegister = implementation!!.registerCount - parameters.size - 2

                        addInstructionsWithLabels(
                            insertIndex, """
                                invoke-static {}, $PLAYER_CLASS_DESCRIPTOR->enableSwipeToDismissMiniPlayer()Z
                                move-result v$freeRegister
                                if-nez v$freeRegister, :dismiss
                                iget-object v$primaryRegister, v$primaryRegister, $swipeToDismissIGetObjectReference
                                invoke-interface {v$primaryRegister}, $swipeToDismissInvokeInterfacePrimaryReference
                                move-result-object v$primaryRegister
                                check-cast v$primaryRegister, $swipeToDismissCheckCastReference
                                sget-object v$secondaryRegister, $swipeToDismissSGetObjectReference
                                new-instance v$tertiaryRegister, $swipeToDismissNewInstanceReference
                                const p0, 0x878b
                                invoke-static {p0}, $swipeToDismissInvokeStaticReference
                                move-result-object p0
                                invoke-direct {v$tertiaryRegister, p0}, $swipeToDismissInvokeDirectReference
                                const/4 p0, 0x0
                                invoke-interface {v$primaryRegister, v$secondaryRegister, v$tertiaryRegister, p0}, $swipeToDismissInvokeInterfaceSecondaryReference
                                return-void
                                """, ExternalLabel("dismiss", getInstruction(insertIndex))
                        )
                    }
                }
            }

            // endregion

            // region hides default text display when the app is cold started

            MiniPlayerDefaultTextFingerprint.resultOrThrow().let {
                it.mutableMethod.apply {
                    val insertIndex = it.scanResult.patternScanResult!!.endIndex
                    val insertRegister =
                        getInstruction<TwoRegisterInstruction>(insertIndex).registerB

                    addInstructions(
                        insertIndex, """
                            invoke-static {v$insertRegister}, $PLAYER_CLASS_DESCRIPTOR->enableSwipeToDismissMiniPlayer(Ljava/lang/Object;)Ljava/lang/Object;
                            move-result-object v$insertRegister
                            """
                    )
                }
            }

            // endregion

            // region hides default text display after dismissing the mini player

            MiniPlayerDefaultViewVisibilityFingerprint.resultOrThrow().let {
                it.mutableClass.methods.find { method ->
                    method.parameters == listOf("Landroid/view/View;", "I")
                }?.apply {
                    val bottomSheetBehaviorIndex = indexOfFirstInstructionOrThrow {
                        val reference = getReference<MethodReference>()
                        opcode == Opcode.INVOKE_VIRTUAL
                                && reference?.definingClass == "Lcom/google/android/material/bottomsheet/BottomSheetBehavior;"
                                && reference.parameterTypes.first() == "Z"
                    }
                    val freeRegister =
                        getInstruction<FiveRegisterInstruction>(bottomSheetBehaviorIndex).registerD

                    addInstructionsWithLabels(
                        bottomSheetBehaviorIndex - 2,
                        """
                            invoke-static {}, $PLAYER_CLASS_DESCRIPTOR->enableSwipeToDismissMiniPlayer()Z
                            move-result v$freeRegister
                            if-nez v$freeRegister, :dismiss
                            """,
                        ExternalLabel("dismiss", getInstruction(bottomSheetBehaviorIndex + 1))
                    )
                } ?: throw PatchException("Could not find targetMethod")

            }

            // endregion

        }

        SettingsPatch.addSwitchPreference(
            CategoryType.PLAYER,
            "revanced_enable_swipe_to_dismiss_mini_player",
            "true"
        )

        // endregion

        // region patch for enable zen mode

        MiniPlayerConstructorFingerprint.resultOrThrow().let { parentResult ->
            // resolves fingerprints
            SwitchToggleColorFingerprint.resolve(context, parentResult.classDef)
            ZenModeFingerprint.resolve(context, parentResult.classDef)

            // this method is used for old player background (deprecated since YT Music v6.34.51)
            ZenModeFingerprint.result?.let {
                it.mutableMethod.apply {
                    val startIndex = it.scanResult.patternScanResult!!.startIndex
                    val targetRegister =
                        getInstruction<OneRegisterInstruction>(startIndex).registerA

                    val insertIndex = it.scanResult.patternScanResult!!.endIndex + 1

                    addInstructions(
                        insertIndex, """
                            invoke-static {v$targetRegister}, $PLAYER_CLASS_DESCRIPTOR->enableZenMode(I)I
                            move-result v$targetRegister
                            """
                    )
                }
            } // no exception

            SwitchToggleColorFingerprint.resultOrThrow().let {
                it.mutableMethod.apply {
                    val invokeDirectIndex = indexOfFirstInstructionOrThrow(Opcode.INVOKE_DIRECT)
                    val walkerMethod = getWalkerMethod(context, invokeDirectIndex)

                    walkerMethod.addInstructions(
                        0, """
                            invoke-static {p1}, $PLAYER_CLASS_DESCRIPTOR->enableZenMode(I)I
                            move-result p1
                            invoke-static {p2}, $PLAYER_CLASS_DESCRIPTOR->enableZenMode(I)I
                            move-result p2
                            """
                    )
                }
            }
        }

        SettingsPatch.addSwitchPreference(
            CategoryType.PLAYER,
            "revanced_enable_zen_mode",
            "false"
        )
        SettingsPatch.addSwitchPreference(
            CategoryType.PLAYER,
            "revanced_enable_zen_mode_podcast",
            "false",
            "revanced_enable_zen_mode"
        )

        // endregion

        // region patch for hide audio video switch toggle

        AudioVideoSwitchToggleFingerprint.resultOrThrow().mutableMethod.apply {
            val constIndex = indexOfFirstWideLiteralInstructionValueOrThrow(AudioVideoSwitchToggle)
            val viewIndex = indexOfFirstInstructionOrThrow(constIndex, Opcode.MOVE_RESULT_OBJECT)
            val viewRegister = getInstruction<OneRegisterInstruction>(viewIndex).registerA

            addInstruction(
                viewIndex + 1,
                "invoke-static {v$viewRegister}, $PLAYER_CLASS_DESCRIPTOR->hideAudioVideoSwitchToggle(Landroid/view/View;)V"
            )
        }

        SettingsPatch.addSwitchPreference(
            CategoryType.PLAYER,
            "revanced_hide_audio_video_switch_toggle",
            "false"
        )

        // endregion

        // region patch for hide channel guideline, timestamps & emoji picker buttons

        LithoFilterPatch.addFilter(FILTER_CLASS_DESCRIPTOR)

        SettingsPatch.addSwitchPreference(
            CategoryType.PLAYER,
            "revanced_hide_comment_channel_guidelines",
            "true"
        )
        SettingsPatch.addSwitchPreference(
            CategoryType.PLAYER,
            "revanced_hide_comment_timestamp_and_emoji_buttons",
            "false"
        )

        // region patch for hide double-tap overlay filter

        val smaliInstruction = """
            invoke-static {v$REGISTER_TEMPLATE_REPLACEMENT}, $PLAYER_CLASS_DESCRIPTOR->hideDoubleTapOverlayFilter(Landroid/view/View;)V
            """

        arrayOf(
            DarkBackground,
            TapBloomView
        ).forEach { literal ->
            QuickSeekOverlayFingerprint.injectLiteralInstructionViewCall(
                literal,
                smaliInstruction
            )
        }

        SettingsPatch.addSwitchPreference(
            CategoryType.PLAYER,
            "revanced_hide_double_tap_overlay_filter",
            "false"
        )

        // endregion

        // region patch for hide fullscreen share button

        RemixGenericButtonFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val targetIndex = it.scanResult.patternScanResult!!.endIndex
                val targetRegister = getInstruction<TwoRegisterInstruction>(targetIndex).registerA

                addInstructions(
                    targetIndex + 1, """
                        invoke-static {v$targetRegister}, $PLAYER_CLASS_DESCRIPTOR->hideFullscreenShareButton(I)I
                        move-result v$targetRegister
                        """
                )
            }
        }

        SettingsPatch.addSwitchPreference(
            CategoryType.PLAYER,
            "revanced_hide_fullscreen_share_button",
            "false"
        )

        // endregion

        // region patch for remember repeat state

        RepeatTrackFingerprint.resultOrThrow().let {
            it.mutableMethod.apply {
                val targetIndex = it.scanResult.patternScanResult!!.endIndex
                val targetRegister = getInstruction<OneRegisterInstruction>(targetIndex).registerA

                addInstructions(
                    targetIndex, """
                        invoke-static {v$targetRegister}, $PLAYER_CLASS_DESCRIPTOR->rememberRepeatState(Z)Z
                        move-result v$targetRegister
                        """
                )
            }
        }

        SettingsPatch.addSwitchPreference(
            CategoryType.PLAYER,
            "revanced_remember_repeat_state",
            "true"
        )

        // endregion

        // region patch for remember shuffle state

        ShuffleOnClickFingerprint.resultOrThrow().mutableMethod.apply {
            val accessibilityIndex =
                ShuffleOnClickFingerprint.indexOfAccessibilityInstruction(this)

            // region set shuffle enum

            val enumIndex = indexOfFirstInstructionReversedOrThrow(accessibilityIndex) {
                opcode == Opcode.INVOKE_DIRECT &&
                        getReference<MethodReference>()?.returnType == "Ljava/lang/String;"
            }
            val enumRegister = getInstruction<FiveRegisterInstruction>(enumIndex).registerD
            val enumClass =
                (getInstruction<ReferenceInstruction>(enumIndex).reference as MethodReference).parameterTypes.first()

            addInstruction(
                enumIndex,
                "invoke-static {v$enumRegister}, $PLAYER_CLASS_DESCRIPTOR->setShuffleState(Ljava/lang/Enum;)V"
            )

            // endregion

            // region set static field

            val shuffleClassIndex =
                indexOfFirstInstructionReversedOrThrow(accessibilityIndex, Opcode.CHECK_CAST)
            val shuffleClass =
                getInstruction<ReferenceInstruction>(shuffleClassIndex).reference.toString()
            val shuffleMutableClass = context.findClass { classDef ->
                classDef.type == shuffleClass
            }!!.mutableClass

            val shuffleMethod = shuffleMutableClass.methods.find { method ->
                method.parameterTypes.firstOrNull() == enumClass &&
                        method.parameterTypes.size == 1 &&
                        method.returnType == "V"
            } ?: throw PatchException("target not found")

            val smaliInstructions =
                """
                    if-eqz v0, :ignore
                    sget-object v1, $enumClass->b:$enumClass
                    invoke-virtual {v0, v1}, $shuffleClass->${shuffleMethod.name}($enumClass)V
                    :ignore
                    return-void
                """

            context.addStaticFieldToIntegration(
                INTEGRATIONS_VIDEO_UTILS_CLASS_DESCRIPTOR,
                "shuffleTracks",
                "shuffleClass",
                shuffleClass,
                smaliInstructions
            )

            // endregion

            // region make all methods accessible

            context.traverseClassHierarchy(shuffleMutableClass) {
                transformMethods {
                    ImmutableMethod(
                        definingClass,
                        name,
                        parameters,
                        returnType,
                        AccessFlags.PUBLIC or AccessFlags.FINAL,
                        annotations,
                        hiddenApiRestrictions,
                        implementation
                    ).toMutable()
                }
            }

            // endregion

        }

        MusicPlaybackControlsFingerprint.resultOrThrow().mutableMethod.addInstruction(
            0,
            "invoke-static {}, $PLAYER_CLASS_DESCRIPTOR->shuffleTracks()V"
        )

        SettingsPatch.addSwitchPreference(
            CategoryType.PLAYER,
            "revanced_remember_shuffle_state",
            "true"
        )

        // endregion

        // region patch for restore old comments popup panels

        var restoreOldCommentsPopupPanel = false

        if (SettingsPatch.upward0627 && !SettingsPatch.upward0718) {
            OldEngagementPanelFingerprint.injectLiteralInstructionBooleanCall(
                45427672,
                "$PLAYER_CLASS_DESCRIPTOR->restoreOldCommentsPopUpPanels(Z)Z"
            )
            restoreOldCommentsPopupPanel = true
        } else if (SettingsPatch.upward0718) {

            // region disable player from being pushed to the top when opening a comment

            MppWatchWhileLayoutFingerprint.resultOrThrow().mutableMethod.apply {
                val callableIndex =
                    MppWatchWhileLayoutFingerprint.indexOfCallableInstruction(this)
                val insertIndex =
                    indexOfFirstInstructionReversedOrThrow(callableIndex, Opcode.NEW_INSTANCE)
                val insertRegister = getInstruction<OneRegisterInstruction>(insertIndex).registerA

                addInstructionsWithLabels(
                    insertIndex, """
                        invoke-static {}, $PLAYER_CLASS_DESCRIPTOR->restoreOldCommentsPopUpPanels()Z
                        move-result v$insertRegister
                        if-eqz v$insertRegister, :restore
                        """, ExternalLabel("restore", getInstruction(callableIndex + 1))
                )
            }

            // endregion

            // region region limit the height of the engagement panel

            EngagementPanelHeightFingerprint.alsoResolve(
                context, EngagementPanelHeightParentFingerprint
            ).let {
                it.mutableMethod.apply {
                    val targetIndex = it.scanResult.patternScanResult!!.endIndex
                    val targetRegister =
                        getInstruction<OneRegisterInstruction>(targetIndex).registerA

                    addInstructions(
                        targetIndex + 1, """
                            invoke-static {v$targetRegister}, $PLAYER_CLASS_DESCRIPTOR->restoreOldCommentsPopUpPanels(Z)Z
                            move-result v$targetRegister
                            """
                    )
                }
            }

            MiniPlayerDefaultViewVisibilityFingerprint.resultOrThrow().let {
                it.mutableClass.methods.find { method ->
                    method.parameters == listOf("Landroid/view/View;", "I")
                }?.apply {
                    val targetIndex = indexOfFirstInstructionOrThrow {
                        val reference = getReference<MethodReference>()
                        opcode == Opcode.INVOKE_INTERFACE
                                && reference?.returnType == "Z"
                                && reference.parameterTypes.size == 0
                    } + 1
                    val targetRegister =
                        getInstruction<OneRegisterInstruction>(targetIndex).registerA

                    addInstructions(
                        targetIndex + 1, """
                            invoke-static {v$targetRegister}, $PLAYER_CLASS_DESCRIPTOR->restoreOldCommentsPopUpPanels(Z)Z
                            move-result v$targetRegister
                            """
                    )
                } ?: throw PatchException("Could not find targetMethod")

            }

            // endregion

            restoreOldCommentsPopupPanel = true
        }

        if (restoreOldCommentsPopupPanel) {
            SettingsPatch.addSwitchPreference(
                CategoryType.PLAYER,
                "revanced_restore_old_comments_popup_panels",
                "false"
            )
        }

        // endregion

        // region patch for restore old player background

        OldPlayerBackgroundFingerprint.result?.let {
            OldPlayerBackgroundFingerprint.injectLiteralInstructionBooleanCall(
                45415319,
                "$PLAYER_CLASS_DESCRIPTOR->restoreOldPlayerBackground(Z)Z"
            )

            SettingsPatch.addSwitchPreference(
                CategoryType.PLAYER,
                "revanced_restore_old_player_background",
                "false"
            )
        }

        // endregion

        // region patch for restore old player layout

        OldPlayerLayoutFingerprint.result?.let {
            OldPlayerLayoutFingerprint.injectLiteralInstructionBooleanCall(
                45399578,
                "$PLAYER_CLASS_DESCRIPTOR->restoreOldPlayerLayout(Z)Z"
            )

            SettingsPatch.addSwitchPreference(
                CategoryType.PLAYER,
                "revanced_restore_old_player_layout",
                "false"
            )
        }

        // endregion

    }

    private fun MutableMethod.setInstanceFieldValue(
        methodName: String,
        viewId: Long
    ) {
        val miniPlayerPlayPauseReplayButtonIndex =
            indexOfFirstWideLiteralInstructionValueOrThrow(MiniPlayerPlayPauseReplayButton)
        val miniPlayerPlayPauseReplayButtonRegister =
            getInstruction<OneRegisterInstruction>(miniPlayerPlayPauseReplayButtonIndex).registerA
        val findViewByIdIndex =
            indexOfFirstInstructionOrThrow(
                miniPlayerPlayPauseReplayButtonIndex,
                Opcode.INVOKE_VIRTUAL
            )
        val parentViewRegister =
            getInstruction<FiveRegisterInstruction>(findViewByIdIndex).registerC

        addInstructions(
            miniPlayerPlayPauseReplayButtonIndex, """
                const v$miniPlayerPlayPauseReplayButtonRegister, $viewId
                invoke-virtual {v$parentViewRegister, v$miniPlayerPlayPauseReplayButtonRegister}, Landroid/view/View;->findViewById(I)Landroid/view/View;
                move-result-object v$miniPlayerPlayPauseReplayButtonRegister
                invoke-static {v$miniPlayerPlayPauseReplayButtonRegister}, $PLAYER_CLASS_DESCRIPTOR->$methodName(Landroid/view/View;)V
                """
        )
    }

    private fun MutableMethod.setStaticFieldValue(
        fieldName: String,
        viewId: Long
    ) {
        val miniPlayerPlayPauseReplayButtonIndex =
            indexOfFirstWideLiteralInstructionValueOrThrow(MiniPlayerPlayPauseReplayButton)
        val constRegister =
            getInstruction<OneRegisterInstruction>(miniPlayerPlayPauseReplayButtonIndex).registerA
        val findViewByIdIndex =
            indexOfFirstInstructionOrThrow(
                miniPlayerPlayPauseReplayButtonIndex,
                Opcode.INVOKE_VIRTUAL
            )
        val findViewByIdRegister =
            getInstruction<FiveRegisterInstruction>(findViewByIdIndex).registerC

        addInstructions(
            miniPlayerPlayPauseReplayButtonIndex, """
                const v$constRegister, $viewId
                invoke-virtual {v$findViewByIdRegister, v$constRegister}, $definingClass->findViewById(I)Landroid/view/View;
                move-result-object v$constRegister
                sput-object v$constRegister, $PLAYER_CLASS_DESCRIPTOR->$fieldName:Landroid/view/View;
                """
        )
    }

    private fun MutableMethod.setViewArray() {
        val miniPlayerPlayPauseReplayButtonIndex =
            indexOfFirstWideLiteralInstructionValueOrThrow(MiniPlayerPlayPauseReplayButton)
        val invokeStaticIndex =
            indexOfFirstInstructionOrThrow(
                miniPlayerPlayPauseReplayButtonIndex,
                Opcode.INVOKE_STATIC
            )
        val viewArrayRegister = getInstruction<FiveRegisterInstruction>(invokeStaticIndex).registerC

        addInstructions(
            invokeStaticIndex, """
                invoke-static {v$viewArrayRegister}, $PLAYER_CLASS_DESCRIPTOR->getViewArray([Landroid/view/View;)[Landroid/view/View;
                move-result-object v$viewArrayRegister
                """
        )
    }

    private fun MutableMethod.setOnClickListener(
        context: BytecodeContext,
        intentString: String,
        methodName: String,
        fieldName: String
    ) {
        val startIndex = indexOfFirstStringInstructionOrThrow(intentString)
        val onClickIndex = indexOfFirstInstructionReversedOrThrow(startIndex, Opcode.INVOKE_VIRTUAL)
        val onClickReference = getInstruction<ReferenceInstruction>(onClickIndex).reference
        val onClickReferenceDefiningClass = (onClickReference as MethodReference).definingClass

        context.findMethodOrThrow(onClickReferenceDefiningClass)
            .apply {
                addInstruction(
                    implementation!!.instructions.lastIndex,
                    "sput-object p0, $PLAYER_CLASS_DESCRIPTOR->$fieldName:$onClickReferenceDefiningClass"
                )
            }

        PlayerPatchConstructorFingerprint.resultOrThrow().let {
            val mutableClass = it.mutableClass
            mutableClass.methods.find { method -> method.name == methodName }
                ?.apply {
                    mutableClass.staticFields.add(
                        ImmutableField(
                            definingClass,
                            fieldName,
                            onClickReferenceDefiningClass,
                            AccessFlags.PUBLIC or AccessFlags.STATIC,
                            null,
                            annotations,
                            null
                        ).toMutable()
                    )
                    addInstructionsWithLabels(
                        0, """
                            sget-object v0, $PLAYER_CLASS_DESCRIPTOR->$fieldName:$onClickReferenceDefiningClass
                            if-eqz v0, :ignore
                            invoke-virtual {v0}, $onClickReference
                            :ignore
                            return-void
                            """
                    )
                }
        }
    }
}