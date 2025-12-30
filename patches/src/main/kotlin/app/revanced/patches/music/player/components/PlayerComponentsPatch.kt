package app.revanced.patches.music.player.components

import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.extensions.InstructionExtensions.removeInstruction
import app.revanced.patcher.extensions.InstructionExtensions.replaceInstruction
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.ResourcePatchContext
import app.revanced.patcher.patch.booleanOption
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patcher.patch.resourcePatch
import app.revanced.patcher.util.proxy.mutableTypes.MutableField.Companion.toMutable
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.music.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.music.utils.extension.Constants.COMPONENTS_PATH
import app.revanced.patches.music.utils.extension.Constants.EXTENSION_PATH
import app.revanced.patches.music.utils.extension.Constants.PLAYER_CLASS_DESCRIPTOR
import app.revanced.patches.music.utils.mainactivity.mainActivityResolvePatch
import app.revanced.patches.music.utils.patch.PatchList.PLAYER_COMPONENTS
import app.revanced.patches.music.utils.pendingIntentReceiverFingerprint
import app.revanced.patches.music.utils.playservice.is_6_27_or_greater
import app.revanced.patches.music.utils.playservice.is_6_42_or_greater
import app.revanced.patches.music.utils.playservice.is_7_18_or_greater
import app.revanced.patches.music.utils.playservice.is_7_25_or_greater
import app.revanced.patches.music.utils.playservice.is_7_29_or_greater
import app.revanced.patches.music.utils.playservice.is_8_03_or_greater
import app.revanced.patches.music.utils.playservice.is_8_05_or_greater
import app.revanced.patches.music.utils.playservice.is_8_12_or_greater
import app.revanced.patches.music.utils.playservice.versionCheckPatch
import app.revanced.patches.music.utils.resourceid.colorGrey
import app.revanced.patches.music.utils.resourceid.darkBackground
import app.revanced.patches.music.utils.resourceid.miniPlayerPlayPauseReplayButton
import app.revanced.patches.music.utils.resourceid.miniPlayerViewPager
import app.revanced.patches.music.utils.resourceid.playerViewPager
import app.revanced.patches.music.utils.resourceid.sharedResourceIdPatch
import app.revanced.patches.music.utils.resourceid.tapBloomView
import app.revanced.patches.music.utils.resourceid.topEnd
import app.revanced.patches.music.utils.resourceid.topStart
import app.revanced.patches.music.utils.settings.CategoryType
import app.revanced.patches.music.utils.settings.ResourceUtils.updatePatchStatus
import app.revanced.patches.music.utils.settings.addPreferenceWithIntent
import app.revanced.patches.music.utils.settings.addSwitchPreference
import app.revanced.patches.music.utils.settings.settingsPatch
import app.revanced.patches.music.utils.videotype.videoTypeHookPatch
import app.revanced.patches.shared.comments.commentsPanelPatch
import app.revanced.patches.shared.litho.addLithoFilter
import app.revanced.patches.shared.litho.lithoFilterPatch
import app.revanced.patches.shared.mainactivity.getMainActivityMethod
import app.revanced.util.REGISTER_TEMPLATE_REPLACEMENT
import app.revanced.util.addStaticFieldToExtension
import app.revanced.util.adoptChild
import app.revanced.util.cloneMutable
import app.revanced.util.doRecursively
import app.revanced.util.findInstructionIndicesReversed
import app.revanced.util.findMethodOrThrow
import app.revanced.util.fingerprint.injectLiteralInstructionBooleanCall
import app.revanced.util.fingerprint.injectLiteralInstructionViewCall
import app.revanced.util.fingerprint.legacyFingerprint
import app.revanced.util.fingerprint.matchOrNull
import app.revanced.util.fingerprint.matchOrThrow
import app.revanced.util.fingerprint.methodCall
import app.revanced.util.fingerprint.methodOrThrow
import app.revanced.util.fingerprint.mutableClassOrThrow
import app.revanced.util.fingerprint.resolvable
import app.revanced.util.getReference
import app.revanced.util.getWalkerMethod
import app.revanced.util.indexOfFirstInstruction
import app.revanced.util.indexOfFirstInstructionOrThrow
import app.revanced.util.indexOfFirstInstructionReversedOrThrow
import app.revanced.util.indexOfFirstLiteralInstructionOrThrow
import app.revanced.util.indexOfFirstStringInstructionOrThrow
import app.revanced.util.insertNode
import app.revanced.util.or
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.immutable.ImmutableField
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodParameter
import org.w3c.dom.Element

private const val IMAGE_VIEW_TAG_NAME =
    "com.google.android.libraries.youtube.common.ui.TouchImageView"
private const val NEXT_BUTTON_VIEW_ID =
    "mini_player_next_button"
private const val PREVIOUS_BUTTON_VIEW_ID =
    "mini_player_previous_button"

private val playerComponentsResourcePatch = resourcePatch(
    description = "playerComponentsResourcePatch"
) {
    dependsOn(versionCheckPatch)

    execute {
        val publicFile = get("res/values/public.xml")

        // Since YT Music v6.42.51,the resources for the next button have been removed, we need to add them manually.
        if (is_6_42_or_greater) {
            publicFile.writeText(
                publicFile.readText()
                    .replace(
                        "\"TOP_START\"",
                        "\"$NEXT_BUTTON_VIEW_ID\""
                    )
            )
            insertNode(false)
        }
        publicFile.writeText(
            publicFile.readText()
                .replace(
                    "\"TOP_END\"",
                    "\"$PREVIOUS_BUTTON_VIEW_ID\""
                )
        )
        insertNode(true)
    }
}

private fun ResourcePatchContext.insertNode(isPreviousButton: Boolean) {
    var shouldAddPreviousButton = true

    document("res/layout/watch_while_layout.xml").use { document ->
        document.doRecursively loop@{ node ->
            if (node !is Element) return@loop

            node.getAttributeNode("android:id")?.let { attribute ->
                if (isPreviousButton) {
                    if (attribute.textContent == "@id/mini_player_play_pause_replay_button" &&
                        shouldAddPreviousButton
                    ) {
                        node.insertNode(IMAGE_VIEW_TAG_NAME, node) {
                            setPreviousButtonNodeAttribute()
                        }
                        shouldAddPreviousButton = false
                    }
                } else {
                    if (attribute.textContent == "@id/mini_player") {
                        node.adoptChild(IMAGE_VIEW_TAG_NAME) {
                            setNextButtonNodeAttribute()
                        }
                    }
                }
            }
        }
    }
}

private fun Element.setNextButtonNodeAttribute() {
    mapOf(
        "android:id" to "@id/$NEXT_BUTTON_VIEW_ID",
        "android:padding" to "@dimen/item_medium_spacing",
        "android:layout_width" to "@dimen/remix_generic_button_size",
        "android:layout_height" to "@dimen/remix_generic_button_size",
        "android:src" to "@drawable/music_player_next",
        "android:scaleType" to "fitCenter",
        "android:contentDescription" to "@string/accessibility_next",
        "style" to "@style/MusicPlayerButton"
    ).forEach { (k, v) ->
        setAttribute(k, v)
    }
}

private fun Element.setPreviousButtonNodeAttribute() {
    mapOf(
        "android:id" to "@id/$PREVIOUS_BUTTON_VIEW_ID",
        "android:padding" to "@dimen/item_medium_spacing",
        "android:layout_width" to "@dimen/remix_generic_button_size",
        "android:layout_height" to "@dimen/remix_generic_button_size",
        "android:src" to "@drawable/music_player_prev",
        "android:scaleType" to "fitCenter",
        "android:contentDescription" to "@string/accessibility_previous",
        "style" to "@style/MusicPlayerButton"
    ).forEach { (k, v) ->
        setAttribute(k, v)
    }
}

private const val FILTER_CLASS_DESCRIPTOR =
    "$COMPONENTS_PATH/PlayerComponentsFilter;"

private const val EXTENSION_VIDEO_UTILS_CLASS_DESCRIPTOR =
    "$EXTENSION_PATH/utils/VideoUtils;"

@Suppress("unused")
val playerComponentsPatch = bytecodePatch(
    PLAYER_COMPONENTS.title,
    PLAYER_COMPONENTS.summary,
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(
        settingsPatch,
        playerComponentsResourcePatch,
        sharedResourceIdPatch,
        lithoFilterPatch,
        mainActivityResolvePatch,
        videoTypeHookPatch,
        commentsPanelPatch,
    )

    val swipeToDismissMiniPlayer by booleanOption(
        key = "swipeToDismissMiniPlayer",
        default = true,
        title = "Swipe to dismiss miniplayer",
        description = """
            Adds an option to enable swipe to dismiss the miniplayer.
            
            If this patch option is enabled, the empty miniplayer may not show when the miniplayer is dismissed.
            """.trimIndent(),
        required = true
    )

    execute {

        // region patch for add next previous button

        val nextButtonViewMethodName = "setNextButtonView"
        val previousButtonViewMethodName = "setPreviousButtonView"
        val nextButtonClassFieldName = "nextButtonClass"
        val previousButtonClassFieldName = "previousButtonClass"
        val nextOnClickListenerMethodName = "setNextButtonOnClickListener"
        val previousOnClickListenerMethodName = "setPreviousButtonOnClickListener"
        val nextButtonClickedMethodName = "nextButtonClicked"
        val previousButtonClickedMethodName = "previousButtonClicked"
        val nextButtonIntentString = "YTM Next"
        val previousButtonIntentString = "YTM Previous"

        fun MutableMethod.setButtonView(
            methodName: String,
            viewId: Long
        ) {
            val miniPlayerPlayPauseReplayButtonIndex =
                indexOfFirstLiteralInstructionOrThrow(miniPlayerPlayPauseReplayButton)
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
                    invoke-static {v$constRegister}, $PLAYER_CLASS_DESCRIPTOR->$methodName(Landroid/view/View;)V
                    """
            )
        }

        fun MutableMethod.setViewArray() {
            val miniPlayerPlayPauseReplayButtonIndex =
                indexOfFirstLiteralInstructionOrThrow(miniPlayerPlayPauseReplayButton)
            val invokeStaticIndex =
                indexOfFirstInstructionOrThrow(
                    miniPlayerPlayPauseReplayButtonIndex,
                    Opcode.INVOKE_STATIC
                )
            val viewArrayRegister =
                getInstruction<FiveRegisterInstruction>(invokeStaticIndex).registerC

            addInstructions(
                invokeStaticIndex, """
                    invoke-static {v$viewArrayRegister}, $PLAYER_CLASS_DESCRIPTOR->getViewArray([Landroid/view/View;)[Landroid/view/View;
                    move-result-object v$viewArrayRegister
                    """
            )
        }

        fun MutableMethod.setIntentOnClickListener(
            intentString: String,
            methodName: String,
            fieldName: String
        ) {
            val startIndex = indexOfFirstStringInstructionOrThrow(intentString)
            val onClickIndex =
                indexOfFirstInstructionReversedOrThrow(startIndex, Opcode.INVOKE_VIRTUAL)
            val onClickReference = getInstruction<ReferenceInstruction>(onClickIndex).reference
            val onClickReferenceDefiningClass = (onClickReference as MethodReference).definingClass

            findMethodOrThrow(onClickReferenceDefiningClass)
                .apply {
                    addInstruction(
                        implementation!!.instructions.lastIndex,
                        "sput-object p0, $PLAYER_CLASS_DESCRIPTOR->$fieldName:$onClickReferenceDefiningClass"
                    )
                }

            playerPatchConstructorFingerprint.mutableClassOrThrow().let { mutableClass ->
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

        fun MutableMethod.setOnclickListener(
            methodName: String,
            viewId: Long
        ) {
            val miniPlayerPlayPauseReplayButtonIndex =
                indexOfFirstLiteralInstructionOrThrow(miniPlayerPlayPauseReplayButton)
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

        val miniPlayerConstructorMutableMethod =
            miniPlayerConstructorFingerprint.methodOrThrow()

        val mppWatchWhileLayoutMutableMethod =
            mppWatchWhileLayoutFingerprint.methodOrThrow()

        val pendingIntentReceiverMutableMethod =
            pendingIntentReceiverFingerprint.methodOrThrow()

        if (!is_6_42_or_greater) {
            nextButtonVisibilityFingerprint.matchOrThrow(miniPlayerParentFingerprint).let {
                it.method.apply {
                    val targetIndex = it.patternMatch!!.startIndex + 1
                    val targetRegister =
                        getInstruction<OneRegisterInstruction>(targetIndex).registerA

                    addInstructions(
                        targetIndex + 1, """
                            invoke-static {v$targetRegister}, $PLAYER_CLASS_DESCRIPTOR->addMiniPlayerNextButton(Z)Z
                            move-result v$targetRegister
                            """
                    )
                }
            }
        } else {
            miniPlayerConstructorMutableMethod.setOnclickListener(
                nextOnClickListenerMethodName,
                topStart
            )
            mppWatchWhileLayoutMutableMethod.setButtonView(nextButtonViewMethodName, topStart)
            pendingIntentReceiverMutableMethod.setIntentOnClickListener(
                nextButtonIntentString,
                nextButtonClickedMethodName,
                nextButtonClassFieldName
            )
        }

        miniPlayerConstructorMutableMethod.setOnclickListener(
            previousOnClickListenerMethodName,
            topEnd
        )
        mppWatchWhileLayoutMutableMethod.setButtonView(previousButtonViewMethodName, topEnd)
        pendingIntentReceiverMutableMethod.setIntentOnClickListener(
            previousButtonIntentString,
            previousButtonClickedMethodName,
            previousButtonClassFieldName
        )

        mppWatchWhileLayoutMutableMethod.setViewArray()

        addSwitchPreference(
            CategoryType.PLAYER,
            "revanced_add_miniplayer_next_button",
            "true"
        )
        addSwitchPreference(
            CategoryType.PLAYER,
            "revanced_add_miniplayer_previous_button",
            "true"
        )

        // endregion

        // region patch for color match player, change player background and enable zen mode (6.35+)

        val (
            colorMathPlayerMethodParameter,
            colorMathPlayerInvokeVirtualReference,
            colorMathPlayerIGetReference
        ) = switchToggleColorFingerprint.matchOrThrow(miniPlayerConstructorFingerprint).let {
            with(it.method) {
                val relativeIndex = it.patternMatch!!.endIndex + 1
                val invokeVirtualIndex =
                    indexOfFirstInstructionOrThrow(relativeIndex, Opcode.INVOKE_VIRTUAL)
                val iGetIndex = indexOfFirstInstructionOrThrow(relativeIndex, Opcode.IGET)

                // black player background
                val invokeDirectIndex = indexOfFirstInstructionOrThrow(Opcode.INVOKE_DIRECT)

                getWalkerMethod(invokeDirectIndex).apply {
                    val index = indexOfFirstInstructionOrThrow(Opcode.FILLED_NEW_ARRAY)
                    val register = getInstruction<OneRegisterInstruction>(index + 1).registerA

                    addInstructions(
                        index + 2, """
                            invoke-static {v$register}, $PLAYER_CLASS_DESCRIPTOR->changePlayerBackgroundColor([I)[I
                            move-result-object v$register
                            """
                    )
                }

                Triple(
                    parameters,
                    getInstruction<ReferenceInstruction>(invokeVirtualIndex).reference,
                    getInstruction<ReferenceInstruction>(iGetIndex).reference
                )
            }
        }

        val colorMathPlayerIPutReference = with(miniPlayerConstructorFingerprint.methodOrThrow()) {
            val colorGreyIndex = indexOfFirstLiteralInstructionOrThrow(colorGrey)
            val iPutIndex = indexOfFirstInstructionOrThrow(colorGreyIndex, Opcode.IPUT)
            getInstruction<ReferenceInstruction>(iPutIndex).reference
        }

        miniPlayerConstructorFingerprint.mutableClassOrThrow().methods.filter {
            it.accessFlags == AccessFlags.PUBLIC or AccessFlags.FINAL &&
                    it.parameters == colorMathPlayerMethodParameter &&
                    it.returnType == "V"
        }.forEach { method ->
            method.apply {
                val freeRegister = implementation!!.registerCount - parameters.size - 3
                val invokeDirectIndex =
                    indexOfFirstInstructionReversedOrThrow(Opcode.INVOKE_DIRECT)
                val invokeDirectReference =
                    getInstruction<ReferenceInstruction>(invokeDirectIndex).reference

                addInstructionsWithLabels(
                    invokeDirectIndex + 1, """
                        invoke-static {}, $PLAYER_CLASS_DESCRIPTOR->changeMiniPlayerColor()Z
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

        addSwitchPreference(
            CategoryType.PLAYER,
            "revanced_change_miniplayer_color",
            "true"
        )
        addSwitchPreference(
            CategoryType.PLAYER,
            "revanced_change_player_background_color",
            "false"
        )
        addPreferenceWithIntent(
            CategoryType.PLAYER,
            "revanced_custom_player_background_color_primary",
            "revanced_change_player_background_color"
        )
        addPreferenceWithIntent(
            CategoryType.PLAYER,
            "revanced_custom_player_background_color_secondary",
            "revanced_change_player_background_color"
        )

        // endregion

        // region patch for enable thick seek bar

        var thickSeekBar = false

        fun MutableMethod.thickSeekBarHook(index: Int, methodName: String = "enableThickSeekBar") {
            val register = getInstruction<OneRegisterInstruction>(index + 1).registerA

            addInstructions(
                index + 2, """
                    invoke-static {v$register}, $PLAYER_CLASS_DESCRIPTOR->$methodName(Z)Z
                    move-result v$register
                    """
            )
        }

        if (is_7_25_or_greater) {
            val thickSeekBarMethodCall = thickSeekBarFeatureFlagFingerprint.methodCall()
            val filter: Instruction.() -> Boolean = {
                opcode == Opcode.INVOKE_VIRTUAL &&
                        getReference<MethodReference>()?.toString() == thickSeekBarMethodCall
            }

            thickSeekBarInflateFingerprint.methodOrThrow().apply {
                val indexes = findInstructionIndicesReversed(filter)

                thickSeekBarHook(indexes.first(), "changeSeekBarPosition")
                thickSeekBarHook(indexes.last())
            }

            if (is_7_29_or_greater) {
                thickSeekBarColorFingerprint.methodOrThrow().apply {
                    findInstructionIndicesReversed(filter).forEach { thickSeekBarHook(it) }
                }
            }

            addSwitchPreference(
                CategoryType.PLAYER,
                "revanced_change_seekbar_position",
                "false"
            )

            thickSeekBar = true
        }

        // endregion

        // region patch for disable gesture in player

        val playerViewPagerConstructorMethod =
            playerViewPagerConstructorFingerprint.methodOrThrow()
        val mainActivityOnStartMethod =
            getMainActivityMethod("onStart")

        mapOf(
            miniPlayerViewPager to "disableMiniPlayerGesture",
            playerViewPager to "disablePlayerGesture"
        ).forEach { (literal, methodName) ->
            val viewPagerReference = with(playerViewPagerConstructorMethod) {
                val constIndex = indexOfFirstLiteralInstructionOrThrow(literal)
                val targetIndex = indexOfFirstInstructionOrThrow(constIndex, Opcode.IPUT_OBJECT)

                getInstruction<ReferenceInstruction>(targetIndex).reference.toString()
            }
            mainActivityOnStartMethod.apply {
                val insertIndex = indexOfFirstInstructionOrThrow {
                    opcode == Opcode.IGET_OBJECT &&
                            getReference<FieldReference>()?.toString() == viewPagerReference
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

        addSwitchPreference(
            CategoryType.PLAYER,
            "revanced_disable_miniplayer_gesture",
            "false"
        )
        addSwitchPreference(
            CategoryType.PLAYER,
            "revanced_disable_player_gesture",
            "false"
        )

        // endregion

        // region patch for forced minimized player

        minimizedPlayerFingerprint.matchOrThrow().let {
            it.method.apply {
                val insertIndex = it.patternMatch!!.endIndex
                val insertRegister = getInstruction<OneRegisterInstruction>(insertIndex).registerA

                addInstructions(
                    insertIndex, """
                        invoke-static {v$insertRegister}, $PLAYER_CLASS_DESCRIPTOR->enableForcedMiniPlayer(Z)Z
                        move-result v$insertRegister
                        """
                )
            }
        }

        addSwitchPreference(
            CategoryType.PLAYER,
            "revanced_enable_forced_miniplayer",
            "true"
        )

        // endregion

        // region patch for enable smooth transition animation

        if (is_8_12_or_greater) {
            smoothTransitionAnimationFingerprint.injectLiteralInstructionBooleanCall(
                SMOOTH_TRANSITION_ANIMATION_FEATURE_FLAG,
                "$PLAYER_CLASS_DESCRIPTOR->enableSmoothTransitionAnimation(Z)Z"
            )

            val smoothTransitionAnimationMethod =
                smoothTransitionAnimationFingerprint.methodCall()

            fun indexOfSmoothTransitionAnimation(method: Method) =
                method.indexOfFirstInstruction {
                    opcode == Opcode.INVOKE_VIRTUAL &&
                            getReference<MethodReference>()?.toString() == smoothTransitionAnimationMethod
                }

            val smoothTransitionAnimationInvertedFingerprint = legacyFingerprint(
                name = "smoothTransitionAnimationInvertedFingerprint",
                returnType = "V",
                accessFlags = AccessFlags.PUBLIC.value,
                parameters = emptyList(),
                customFingerprint = { method, _ ->
                    indexOfSmoothTransitionAnimation(method) >= 0
                }
            )

            smoothTransitionAnimationInvertedFingerprint
                .methodOrThrow(smoothTransitionAnimationInvertedParentFingerprint)
                .apply {
                    val index = indexOfSmoothTransitionAnimation(this)
                    val register = getInstruction<OneRegisterInstruction>(index + 1).registerA

                    addInstructions(
                        index + 2, """
                            invoke-static {v$register}, $PLAYER_CLASS_DESCRIPTOR->enableSmoothTransitionAnimationInverted(Z)Z
                            move-result v$register
                            """
                    )
                }

            addSwitchPreference(
                CategoryType.PLAYER,
                "revanced_enable_smooth_transition_animation",
                "true"
            )
        }

        // endregion

        // region patch for enable swipe to dismiss miniplayer

        if (swipeToDismissMiniPlayer == true) {
            if (!is_6_42_or_greater) {
                swipeToCloseFingerprint.methodOrThrow().apply {
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
            } else {

                // region dismiss mini player by swiping down

                val swipeToDismissSGetObjectReference =
                    with(interactionLoggingEnumFingerprint.methodOrThrow()) {
                        val stringIndex =
                            indexOfFirstStringInstructionOrThrow("INTERACTION_LOGGING_GESTURE_TYPE_SWIPE")
                        val sPutObjectIndex =
                            indexOfFirstInstructionOrThrow(stringIndex, Opcode.SPUT_OBJECT)

                        getInstruction<ReferenceInstruction>(sPutObjectIndex).reference
                    }

                val musicActivityWidgetMethod =
                    musicActivityWidgetFingerprint.methodOrThrow()

                val swipeToDismissWidgetIndex =
                    musicActivityWidgetMethod.indexOfFirstLiteralInstructionOrThrow(79500L)

                fun getSwipeToDismissReference(
                    opcode: Opcode,
                    reversed: Boolean
                ) = with(musicActivityWidgetMethod) {
                    val targetIndex = if (reversed)
                        indexOfFirstInstructionReversedOrThrow(swipeToDismissWidgetIndex, opcode)
                    else
                        indexOfFirstInstructionOrThrow(swipeToDismissWidgetIndex, opcode)

                    getInstruction<ReferenceInstruction>(targetIndex).reference
                }

                val swipeToDismissIGetObjectReference =
                    getSwipeToDismissReference(Opcode.IGET_OBJECT, true)
                val swipeToDismissInvokeInterfacePrimaryReference =
                    getSwipeToDismissReference(Opcode.INVOKE_INTERFACE, true)
                val swipeToDismissCheckCastReference =
                    getSwipeToDismissReference(Opcode.CHECK_CAST, true)
                val swipeToDismissNewInstanceReference =
                    getSwipeToDismissReference(Opcode.NEW_INSTANCE, true)
                val swipeToDismissInvokeStaticReference =
                    getSwipeToDismissReference(Opcode.INVOKE_STATIC, false)
                val swipeToDismissInvokeDirectReference =
                    getSwipeToDismissReference(Opcode.INVOKE_DIRECT, false)
                val swipeToDismissInvokeInterfaceSecondaryReference =
                    getSwipeToDismissReference(Opcode.INVOKE_INTERFACE, false)

                handleSignInEventFingerprint.matchOrThrow(handleSearchRenderedFingerprint).let {
                    val dismissBehaviorMethod =
                        it.getWalkerMethod(it.patternMatch!!.startIndex)

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

                // endregion

                // region hides default text display when the app is cold started

                miniPlayerDefaultTextFingerprint.matchOrThrow().let {
                    it.method.apply {
                        val insertIndex = it.patternMatch!!.endIndex
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

                miniPlayerDefaultViewVisibilityFingerprint.mutableClassOrThrow().let {
                    it.methods.find { method ->
                        method.parameters == listOf("Landroid/view/View;", "I")
                    }?.apply {
                        val bottomSheetBehaviorIndex = indexOfFirstInstructionOrThrow {
                            val reference = getReference<MethodReference>()
                            opcode == Opcode.INVOKE_VIRTUAL &&
                                    reference?.definingClass == "Lcom/google/android/material/bottomsheet/BottomSheetBehavior;" &&
                                    reference.parameterTypes.first() == "Z"
                        }
                        val freeRegister =
                            getInstruction<FiveRegisterInstruction>(bottomSheetBehaviorIndex).registerD

                        val getFieldIndex = bottomSheetBehaviorIndex - 2
                        val getFieldReference =
                            getInstruction<ReferenceInstruction>(getFieldIndex).reference
                        val getFieldInstruction = getInstruction<TwoRegisterInstruction>(getFieldIndex)

                        addInstructionsWithLabels(
                            getFieldIndex + 1, """
                                invoke-static {}, $PLAYER_CLASS_DESCRIPTOR->enableSwipeToDismissMiniPlayer()Z
                                move-result v$freeRegister
                                if-nez v$freeRegister, :dismiss
                                iget-object v${getFieldInstruction.registerA}, v${getFieldInstruction.registerB}, $getFieldReference
                                """,
                            ExternalLabel("dismiss", getInstruction(bottomSheetBehaviorIndex + 1))
                        )
                        removeInstruction(getFieldIndex)
                    } ?: throw PatchException("Could not find targetMethod")
                }

                // endregion

            }

            addSwitchPreference(
                CategoryType.PLAYER,
                "revanced_enable_swipe_to_dismiss_miniplayer",
                "true"
            )
        }

        // endregion

        // region patch for enable scroll to top in comments

        addSwitchPreference(
            CategoryType.PLAYER,
            "revanced_enable_comments_scroll_top",
            "false"
        )

        // endregion

        if (thickSeekBar) {
            addSwitchPreference(
                CategoryType.PLAYER,
                "revanced_enable_thick_seekbar",
                "true"
            )
        }

        // region patch for enable zen mode (~ 6.34)

        // this method is used for old player background (deprecated since YT Music v6.34.51)
        zenModeFingerprint.matchOrNull(miniPlayerConstructorFingerprint)?.let {
            it.method.apply {
                val startIndex = it.patternMatch!!.startIndex
                val targetRegister =
                    getInstruction<OneRegisterInstruction>(startIndex).registerA

                val insertIndex = it.patternMatch!!.endIndex + 1

                addInstructions(
                    insertIndex, """
                        invoke-static {v$targetRegister}, $PLAYER_CLASS_DESCRIPTOR->enableZenMode(I)I
                        move-result v$targetRegister
                        """
                )
            }
        } // no exception

        addSwitchPreference(
            CategoryType.PLAYER,
            "revanced_enable_zen_mode",
            "false"
        )
        addSwitchPreference(
            CategoryType.PLAYER,
            "revanced_enable_zen_mode_podcast",
            "false",
            "revanced_enable_zen_mode"
        )

        // endregion

        // region patch for hide channel guideline, timestamps & emoji picker buttons

        addLithoFilter(FILTER_CLASS_DESCRIPTOR)

        addSwitchPreference(
            CategoryType.PLAYER,
            "revanced_hide_comment_channel_guidelines",
            "true"
        )

        // region patch for hide double-tap overlay filter

        val smaliInstruction = """
            invoke-static {v$REGISTER_TEMPLATE_REPLACEMENT}, $PLAYER_CLASS_DESCRIPTOR->hideDoubleTapOverlayFilter(Landroid/view/View;)V
            """

        arrayOf(
            darkBackground,
            tapBloomView
        ).forEach { literal ->
            quickSeekOverlayFingerprint.injectLiteralInstructionViewCall(
                literal,
                smaliInstruction
            )
        }

        addSwitchPreference(
            CategoryType.PLAYER,
            "revanced_hide_double_tap_overlay_filter",
            "false"
        )

        // endregion

        addSwitchPreference(
            CategoryType.PLAYER,
            "revanced_hide_comment_timestamp_and_emoji_buttons",
            "false"
        )

        // region patch for hide information button

        addSwitchPreference(
            CategoryType.PLAYER,
            "revanced_hide_comments_information_button",
            "false"
        )

        // endregion

        // region patch for hide lyrics share button

        if (is_8_05_or_greater) {
            addSwitchPreference(
                CategoryType.PLAYER,
                "revanced_hide_lyrics_share_button",
                "false"
            )
        }

        // endregion

        // region patch for hide fullscreen share button

        remixGenericButtonFingerprint.matchOrThrow().let {
            it.method.apply {
                val targetIndex = it.patternMatch!!.endIndex
                val targetRegister = getInstruction<TwoRegisterInstruction>(targetIndex).registerA

                addInstructions(
                    targetIndex + 1, """
                        invoke-static {v$targetRegister}, $PLAYER_CLASS_DESCRIPTOR->hideFullscreenShareButton(I)I
                        move-result v$targetRegister
                        """
                )
            }
        }

        addSwitchPreference(
            CategoryType.PLAYER,
            "revanced_hide_fullscreen_share_button",
            "false"
        )

        // endregion

        // region patch for hide song video toggle

        audioVideoSwitchToggleFingerprint.methodOrThrow().apply {
            implementation!!.instructions
                .withIndex()
                .filter { (_, instruction) ->
                    val reference = (instruction as? ReferenceInstruction)?.reference
                    instruction.opcode == Opcode.INVOKE_VIRTUAL &&
                            reference is MethodReference &&
                            reference.toString().endsWith(AUDIO_VIDEO_SWITCH_TOGGLE_VISIBILITY)
                }
                .map { (index, _) -> index }
                .reversed()
                .forEach { index ->
                    val instruction = getInstruction<FiveRegisterInstruction>(index)

                    replaceInstruction(
                        index,
                        "invoke-static {v${instruction.registerC}, v${instruction.registerD}}," +
                                "$PLAYER_CLASS_DESCRIPTOR->hideSongVideoToggle(Landroid/view/View;I)V"
                    )
                }
        }

        if (is_8_05_or_greater) {
            audioVideoSwitchToggleFeatureFlagsFingerprint.injectLiteralInstructionBooleanCall(
                AUDIO_VIDEO_SWITCH_TOGGLE_FEATURE_FLAG,
                "$PLAYER_CLASS_DESCRIPTOR->hideSongVideoToggle(Z)Z"
            )
        }

        addSwitchPreference(
            CategoryType.PLAYER,
            "revanced_hide_song_video_toggle",
            "false"
        )

        // endregion

        // region patch for remember repeat state

        val (repeatTrackMethod, repeatTrackIndex) = repeatTrackFingerprint.matchOrThrow().let {
            with(it.method) {
                val targetIndex = it.patternMatch!!.endIndex
                val targetRegister = getInstruction<OneRegisterInstruction>(targetIndex).registerA

                addInstructions(
                    targetIndex, """
                        invoke-static {v$targetRegister}, $PLAYER_CLASS_DESCRIPTOR->rememberRepeatState(Z)Z
                        move-result v$targetRegister
                        """
                )
                Pair(this, targetIndex)
            }
        }

        addSwitchPreference(
            CategoryType.PLAYER,
            "revanced_remember_repeat_state",
            "true"
        )

        // endregion

        // region patch for remember shuffle state

        shuffleOnClickFingerprint.methodOrThrow().apply {
            // region set shuffle enum
            val enumClass = shuffleEnumFingerprint.methodOrThrow().definingClass

            val startIndex = indexOfFirstLiteralInstructionOrThrow(SHUFFLE_BUTTON_ID)

            val (enumIndex, enumRegister) = if (is_8_03_or_greater) {
                val index = indexOfFirstInstructionReversedOrThrow(startIndex) {
                    opcode == Opcode.INVOKE_VIRTUAL &&
                            getReference<MethodReference>()?.returnType == enumClass
                }
                val register = getInstruction<OneRegisterInstruction>(index + 1).registerA

                Pair(index + 2, register)
            } else {
                val index = indexOfFirstInstructionReversedOrThrow(startIndex) {
                    opcode == Opcode.INVOKE_DIRECT &&
                            getReference<MethodReference>()?.returnType == "Ljava/lang/String;"
                }
                val register = getInstruction<FiveRegisterInstruction>(index).registerD

                Pair(index, register)
            }

            addInstruction(
                enumIndex,
                "invoke-static {v$enumRegister}, $PLAYER_CLASS_DESCRIPTOR->setShuffleState(Ljava/lang/Enum;)V"
            )

            // endregion

            // region set static field

            val shuffleClassIndex =
                indexOfFirstInstructionReversedOrThrow(enumIndex, Opcode.CHECK_CAST)
            val shuffleClass =
                getInstruction<ReferenceInstruction>(shuffleClassIndex).reference.toString()
            val shuffleMutableClass = classBy { classDef ->
                classDef.type == shuffleClass
            }?.mutableClass
                ?: throw PatchException("shuffle class not found")

            val smaliInstructions =
                """
                    if-eqz v0, :ignore
                    sget-object v1, $enumClass->b:$enumClass
                    invoke-virtual {v0, v1}, $shuffleClass->shuffleTracks($enumClass)V
                    :ignore
                    return-void
                """

            addStaticFieldToExtension(
                EXTENSION_VIDEO_UTILS_CLASS_DESCRIPTOR,
                "shuffleTracks",
                "shuffleClass",
                shuffleClass,
                smaliInstructions
            )

            // endregion

            // region make all methods accessible

            fun Method.indexOfEnumOrdinalInstruction() =
                indexOfFirstInstruction {
                    val reference = getReference<MethodReference>()
                    opcode == Opcode.INVOKE_VIRTUAL &&
                            reference?.name == "ordinal" &&
                            reference.definingClass == enumClass
                }

            val isShuffleMethod: Method.() -> Boolean = {
                returnType == "V" &&
                        indexOfEnumOrdinalInstruction() >= 0 &&
                        indexOfFirstInstruction {
                            opcode == Opcode.INVOKE_VIRTUAL &&
                                    getReference<MethodReference>()?.name == "post"
                        } >= 0
            }

            val shuffleMethod = shuffleMutableClass.methods.find { method ->
                method.isShuffleMethod()
            } ?: throw PatchException("shuffle method not found")
            val shuffleMethodRegisterCount = shuffleMethod.implementation!!.registerCount

            shuffleMutableClass.methods.add(
                shuffleMethod.cloneMutable(
                    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
                    name = "shuffleTracks",
                    registerCount = if (is_8_03_or_greater) {
                        shuffleMethodRegisterCount + 1
                    } else {
                        shuffleMethodRegisterCount
                    },
                    parameters = listOf(
                        ImmutableMethodParameter(
                            enumClass,
                            annotations,
                            "enumClass"
                        )
                    )
                ).apply {
                    if (is_8_03_or_greater) {
                        val index = indexOfEnumOrdinalInstruction()
                        val register = getInstruction<FiveRegisterInstruction>(index).registerC

                        addInstruction(
                            index,
                            "move-object/from16 v$register, p1"
                        )
                    }
                }
            )

            // endregion

        }

        musicPlaybackControlsFingerprint.methodOrThrow().addInstruction(
            0,
            "invoke-static {}, $PLAYER_CLASS_DESCRIPTOR->shuffleTracks()V"
        )

        if (is_7_25_or_greater) {
            repeatTrackMethod.addInstruction(
                repeatTrackIndex,
                "invoke-static {}, $PLAYER_CLASS_DESCRIPTOR->shuffleTracksWithDelay()V"
            )
        }

        addSwitchPreference(
            CategoryType.PLAYER,
            "revanced_remember_shuffle_state",
            "true"
        )

        // endregion

        // region patch for restore old comments popup panels

        var restoreOldCommentsPopupPanel = false

        if (is_6_27_or_greater && !is_7_18_or_greater) {
            oldEngagementPanelFingerprint.injectLiteralInstructionBooleanCall(
                OLD_ENGAGEMENT_PANEL_FEATURE_FLAG,
                "$PLAYER_CLASS_DESCRIPTOR->restoreOldCommentsPopUpPanels(Z)Z"
            )
            restoreOldCommentsPopupPanel = true
        } else if (is_7_18_or_greater) {

            // region disable player from being pushed to the top when opening a comment

            mppWatchWhileLayoutFingerprint.methodOrThrow().apply {
                val callableIndex = indexOfCallableInstruction(this)
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

            engagementPanelHeightFingerprint.matchOrThrow(engagementPanelHeightParentFingerprint)
                .let {
                    it.method.apply {
                        val targetIndex = it.patternMatch!!.endIndex
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

            miniPlayerDefaultViewVisibilityFingerprint.mutableClassOrThrow().let {
                it.methods.find { method ->
                    method.parameters == listOf("Landroid/view/View;", "I")
                }?.apply {
                    val targetIndex = indexOfFirstInstructionOrThrow {
                        val reference = getReference<MethodReference>()
                        opcode == Opcode.INVOKE_INTERFACE &&
                                reference?.returnType == "Z" &&
                                reference.parameterTypes.isEmpty()
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
            addSwitchPreference(
                CategoryType.PLAYER,
                "revanced_restore_old_comments_popup_panels",
                "false"
            )
        }

        // endregion

        // region patch for restore old player background

        if (oldPlayerBackgroundFingerprint.resolvable()) {
            oldPlayerBackgroundFingerprint.injectLiteralInstructionBooleanCall(
                OLD_PLAYER_BACKGROUND_FEATURE_FLAG,
                "$PLAYER_CLASS_DESCRIPTOR->restoreOldPlayerBackground(Z)Z"
            )
            addSwitchPreference(
                CategoryType.PLAYER,
                "revanced_restore_old_player_background",
                "false"
            )
        }

        // endregion

        // region patch for restore old player layout

        if (oldPlayerLayoutFingerprint.resolvable()) {
            oldPlayerLayoutFingerprint.injectLiteralInstructionBooleanCall(
                OLD_PLAYER_LAYOUT_FEATURE_FLAG,
                "$PLAYER_CLASS_DESCRIPTOR->restoreOldPlayerLayout(Z)Z"
            )
            addSwitchPreference(
                CategoryType.PLAYER,
                "revanced_restore_old_player_layout",
                "false"
            )
        }

        // endregion

        updatePatchStatus(PLAYER_COMPONENTS)

    }
}
