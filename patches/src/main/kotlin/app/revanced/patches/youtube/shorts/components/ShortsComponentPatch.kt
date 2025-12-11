package app.revanced.patches.youtube.shorts.components

import app.revanced.patcher.Fingerprint
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.extensions.InstructionExtensions.removeInstructions
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.shared.litho.addLithoFilter
import app.revanced.patches.shared.litho.lithoFilterPatch
import app.revanced.patches.shared.mainactivity.injectOnCreateMethodCall
import app.revanced.patches.shared.textcomponent.hookSpannableString
import app.revanced.patches.shared.textcomponent.textComponentPatch
import app.revanced.patches.youtube.utils.bottomSheetMenuItemBuilderFingerprint
import app.revanced.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.engagement.engagementPanelHookPatch
import app.revanced.patches.youtube.utils.extension.Constants.COMPONENTS_PATH
import app.revanced.patches.youtube.utils.extension.Constants.SHORTS_CLASS_DESCRIPTOR
import app.revanced.patches.youtube.utils.extension.Constants.SHORTS_PATH
import app.revanced.patches.youtube.utils.extension.Constants.UTILS_PATH
import app.revanced.patches.youtube.utils.fix.litho.lithoLayoutPatch
import app.revanced.patches.youtube.utils.indexOfSpannedCharSequenceInstruction
import app.revanced.patches.youtube.utils.lottie.LOTTIE_ANIMATION_VIEW_CLASS_DESCRIPTOR
import app.revanced.patches.youtube.utils.lottie.lottieAnimationViewHookPatch
import app.revanced.patches.youtube.utils.mainactivity.mainActivityResolvePatch
import app.revanced.patches.youtube.utils.navigation.addBottomBarContainerHook
import app.revanced.patches.youtube.utils.navigation.navigationBarHookPatch
import app.revanced.patches.youtube.utils.patch.PatchList.HIDE_FEED_FLYOUT_MENU
import app.revanced.patches.youtube.utils.patch.PatchList.SHORTS_COMPONENTS
import app.revanced.patches.youtube.utils.playertype.playerTypeHookPatch
import app.revanced.patches.youtube.utils.playservice.is_18_31_or_greater
import app.revanced.patches.youtube.utils.playservice.is_18_34_or_greater
import app.revanced.patches.youtube.utils.playservice.is_18_49_or_greater
import app.revanced.patches.youtube.utils.playservice.is_19_02_or_greater
import app.revanced.patches.youtube.utils.playservice.is_19_11_or_greater
import app.revanced.patches.youtube.utils.playservice.is_19_25_or_greater
import app.revanced.patches.youtube.utils.playservice.is_19_34_or_greater
import app.revanced.patches.youtube.utils.playservice.is_20_07_or_greater
import app.revanced.patches.youtube.utils.playservice.is_20_09_or_greater
import app.revanced.patches.youtube.utils.playservice.versionCheckPatch
import app.revanced.patches.youtube.utils.recyclerview.recyclerViewTreeObserverHook
import app.revanced.patches.youtube.utils.recyclerview.recyclerViewTreeObserverPatch
import app.revanced.patches.youtube.utils.resourceid.bottomBarContainer
import app.revanced.patches.youtube.utils.resourceid.likeTapFeedbackCairo
import app.revanced.patches.youtube.utils.resourceid.reelDynRemix
import app.revanced.patches.youtube.utils.resourceid.reelDynShare
import app.revanced.patches.youtube.utils.resourceid.reelFeedbackLike
import app.revanced.patches.youtube.utils.resourceid.reelFeedbackPause
import app.revanced.patches.youtube.utils.resourceid.reelFeedbackPlay
import app.revanced.patches.youtube.utils.resourceid.reelForcedMuteButton
import app.revanced.patches.youtube.utils.resourceid.reelPlayerFooter
import app.revanced.patches.youtube.utils.resourceid.reelPlayerRightPivotV2Size
import app.revanced.patches.youtube.utils.resourceid.reelRightDislikeIcon
import app.revanced.patches.youtube.utils.resourceid.reelRightLikeIcon
import app.revanced.patches.youtube.utils.resourceid.rightComment
import app.revanced.patches.youtube.utils.resourceid.sharedResourceIdPatch
import app.revanced.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.revanced.patches.youtube.utils.settings.ResourceUtils.getContext
import app.revanced.patches.youtube.utils.settings.settingsPatch
import app.revanced.patches.youtube.utils.toolbar.hookToolBar
import app.revanced.patches.youtube.utils.toolbar.toolBarHookPatch
import app.revanced.patches.youtube.utils.videoIdFingerprintShorts
import app.revanced.patches.youtube.video.information.hookShortsVideoInformation
import app.revanced.patches.youtube.video.information.videoInformationPatch
import app.revanced.patches.youtube.video.playbackstart.PLAYBACK_START_DESCRIPTOR_CLASS_DESCRIPTOR
import app.revanced.patches.youtube.video.playbackstart.playbackStartDescriptorPatch
import app.revanced.patches.youtube.video.playbackstart.playbackStartVideoIdReference
import app.revanced.patches.youtube.video.playbackstart.shortsPlaybackStartIntentFingerprint
import app.revanced.patches.youtube.video.playbackstart.shortsPlaybackStartIntentLegacyFingerprint
import app.revanced.patches.youtube.video.videoid.hookPlayerResponseVideoId
import app.revanced.patches.youtube.video.videoid.videoIdPatch
import app.revanced.util.REGISTER_TEMPLATE_REPLACEMENT
import app.revanced.util.ResourceGroup
import app.revanced.util.cloneMutable
import app.revanced.util.containsLiteralInstruction
import app.revanced.util.containsStringInstruction
import app.revanced.util.copyResources
import app.revanced.util.doRecursively
import app.revanced.util.findMethodOrThrow
import app.revanced.util.findMutableMethodOf
import app.revanced.util.fingerprint.injectLiteralInstructionBooleanCall
import app.revanced.util.fingerprint.matchOrThrow
import app.revanced.util.fingerprint.methodOrThrow
import app.revanced.util.fingerprint.resolvable
import app.revanced.util.getReference
import app.revanced.util.getWalkerMethod
import app.revanced.util.indexOfFirstInstruction
import app.revanced.util.indexOfFirstInstructionOrThrow
import app.revanced.util.indexOfFirstInstructionReversedOrThrow
import app.revanced.util.indexOfFirstLiteralInstruction
import app.revanced.util.indexOfFirstLiteralInstructionOrThrow
import app.revanced.util.indexOfFirstStringInstructionOrThrow
import app.revanced.util.or
import app.revanced.util.replaceLiteralInstructionCall
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.util.MethodUtil
import org.w3c.dom.Element

private const val EXTENSION_ANIMATION_FEEDBACK_CLASS_DESCRIPTOR =
    "$SHORTS_PATH/AnimationFeedbackPatch;"

private val shortsAnimationPatch = bytecodePatch(
    description = "shortsAnimationPatch"
) {
    dependsOn(
        lottieAnimationViewHookPatch,
        settingsPatch,
        versionCheckPatch
    )

    execute {

        reelFeedbackFingerprint.methodOrThrow().apply {
            val maps = if (is_19_34_or_greater)
                mapOf(reelFeedbackLike to "setShortsLikeFeedback")
            else
                mapOf(
                    reelFeedbackLike to "setShortsLikeFeedback",
                    reelFeedbackPause to "setShortsPauseFeedback",
                    reelFeedbackPlay to "setShortsPlayFeedback",
                )

            maps.forEach { (literal, methodName) ->
                val literalIndex = indexOfFirstLiteralInstructionOrThrow(literal)
                val viewIndex = indexOfFirstInstructionOrThrow(literalIndex) {
                    opcode == Opcode.CHECK_CAST &&
                            (this as? ReferenceInstruction)?.reference?.toString() == LOTTIE_ANIMATION_VIEW_CLASS_DESCRIPTOR
                }
                val viewRegister = getInstruction<OneRegisterInstruction>(viewIndex).registerA
                val methodCall = "invoke-static {v$viewRegister}, " +
                        EXTENSION_ANIMATION_FEEDBACK_CLASS_DESCRIPTOR +
                        "->" +
                        methodName +
                        "($LOTTIE_ANIMATION_VIEW_CLASS_DESCRIPTOR)V"

                addInstruction(
                    viewIndex + 1,
                    methodCall
                )
            }

            if (likeTapFeedbackCairo != -1L) {
                val literalIndex = indexOfFirstLiteralInstruction(likeTapFeedbackCairo)
                if (literalIndex > -1) {
                    val literalRegister =
                        getInstruction<OneRegisterInstruction>(literalIndex).registerA

                    addInstructions(
                        literalIndex + 1, """
                            invoke-static { v$literalRegister }, $EXTENSION_ANIMATION_FEEDBACK_CLASS_DESCRIPTOR->getShortsLikeFeedbackId(I)I
                            move-result v$literalRegister
                            """
                    )
                }
            }
        }

        getContext().copyResources(
            "youtube/shorts/feedback",
            ResourceGroup(
                "raw",
                "like_tap_feedback_cairo.json",
                "like_tap_feedback_heart.json",
                "like_tap_feedback_heart_tint.json",
                "like_tap_feedback_hidden.json",
                "pause_tap_feedback_hidden.json",
                "play_tap_feedback_hidden.json"
            )
        )
    }
}

private const val SHORTS_PLAYER_FLYOUT_MENU_FILTER_CLASS_DESCRIPTOR =
    "$COMPONENTS_PATH/ShortsCustomActionsFilter;"
private const val EXTENSION_CUSTOM_ACTIONS_CLASS_DESCRIPTOR =
    "$SHORTS_PATH/CustomActionsPatch;"

private val shortsCustomActionsPatch = bytecodePatch(
    description = "shortsCustomActionsPatch"
) {
    dependsOn(
        lithoFilterPatch,
        lithoLayoutPatch,
        playerTypeHookPatch,
        recyclerViewTreeObserverPatch,
        toolBarHookPatch,
        videoIdPatch,
        videoInformationPatch,
        versionCheckPatch,
    )

    execute {
        if (!is_18_34_or_greater) {
            return@execute
        }

        // region hook toolbar more button

        hookToolBar("$EXTENSION_CUSTOM_ACTIONS_CLASS_DESCRIPTOR->setToolbarMenu")

        // toolbar in Shorts livestream
        liveHeaderElementsContainerFingerprint.methodOrThrow().apply {
            val addViewIndex = indexOfAddLiveHeaderElementsContainerInstruction(this)
            val viewRegister = getInstruction<FiveRegisterInstruction>(addViewIndex).registerD

            addInstruction(
                addViewIndex + 1,
                "invoke-static {v$viewRegister}, " +
                        "$EXTENSION_CUSTOM_ACTIONS_CLASS_DESCRIPTOR->onLiveHeaderElementsContainerCreate(Landroid/view/View;)V"
            )
        }

        // endregion

        // region add litho filter

        hookPlayerResponseVideoId("$SHORTS_PLAYER_FLYOUT_MENU_FILTER_CLASS_DESCRIPTOR->newPlayerResponseVideoId(Ljava/lang/String;Z)V")
        hookShortsVideoInformation("$SHORTS_PLAYER_FLYOUT_MENU_FILTER_CLASS_DESCRIPTOR->newShortsVideoStarted(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;JZ)V")

        addLithoFilter(SHORTS_PLAYER_FLYOUT_MENU_FILTER_CLASS_DESCRIPTOR)

        // endregion

        if (!is_19_02_or_greater) {
            return@execute
        }

        // region hook flyout menu

        bottomSheetMenuListBuilderFingerprint.matchOrThrow().let {
            it.method.apply {
                val addListIndex = indexOfFirstInstructionOrThrow {
                    opcode == Opcode.INVOKE_VIRTUAL &&
                            getReference<MethodReference>()?.name == "add"
                }
                val addListReference = getInstruction<ReferenceInstruction>(addListIndex).reference

                val getObjectIndex = indexOfFirstInstructionReversedOrThrow(addListIndex) {
                    opcode == Opcode.INVOKE_VIRTUAL &&
                            getReference<MethodReference>()?.returnType == "Ljava/lang/Object;"
                }
                val getObjectReference =
                    getInstruction<ReferenceInstruction>(getObjectIndex).reference as MethodReference

                val bottomSheetMenuInitializeIndex = indexOfFirstInstructionOrThrow {
                    val reference = getReference<MethodReference>()
                    opcode == Opcode.INVOKE_STATIC_RANGE &&
                            reference?.returnType == "V" &&
                            reference.parameterTypes[1] == "Ljava/lang/Object;"
                }
                val bottomSheetMenuObjectRegister =
                    getInstruction<RegisterRangeInstruction>(bottomSheetMenuInitializeIndex).startRegister
                val bottomSheetMenuObject =
                    (getInstruction<ReferenceInstruction>(bottomSheetMenuInitializeIndex).reference as MethodReference).parameterTypes[0]!!

                val bottomSheetMenuListIndex = it.patternMatch!!.startIndex
                val bottomSheetMenuListField =
                    (getInstruction<ReferenceInstruction>(bottomSheetMenuListIndex).reference as FieldReference)

                val bottomSheetMenuClass = bottomSheetMenuListField.definingClass
                val bottomSheetMenuList = bottomSheetMenuListField.type

                val bottomSheetMenuClassRegister =
                    getInstruction<TwoRegisterInstruction>(bottomSheetMenuListIndex).registerB
                val bottomSheetMenuListRegister =
                    getInstruction<TwoRegisterInstruction>(bottomSheetMenuListIndex).registerA

                addInstruction(
                    bottomSheetMenuListIndex + 1,
                    "invoke-static {v$bottomSheetMenuClassRegister, v$bottomSheetMenuListRegister}, " +
                            "$EXTENSION_CUSTOM_ACTIONS_CLASS_DESCRIPTOR->addFlyoutMenu(Ljava/lang/Object;Ljava/lang/Object;)V"
                )

                addInstruction(
                    bottomSheetMenuInitializeIndex + 1,
                    "invoke-static {v$bottomSheetMenuObjectRegister}, " +
                            "$EXTENSION_CUSTOM_ACTIONS_CLASS_DESCRIPTOR->setFlyoutMenuObject(Ljava/lang/Object;)V"
                )

                val addFlyoutMenuMethod =
                    findMethodOrThrow(EXTENSION_CUSTOM_ACTIONS_CLASS_DESCRIPTOR) {
                        name == "addFlyoutMenu" &&
                                accessFlags == AccessFlags.PRIVATE or AccessFlags.STATIC
                    }

                val customActionClass = with(addFlyoutMenuMethod) {
                    val thirdParameter = parameters[2]

                    addInstructions(
                        3, """
                            check-cast p0, $bottomSheetMenuClass
                            check-cast v0, $bottomSheetMenuObject
                            invoke-virtual {p0, v0, p2}, $bottomSheetMenuClass->buildFlyoutMenu(${bottomSheetMenuObject}${thirdParameter})${getObjectReference.definingClass}
                            move-result-object v0
                            invoke-virtual {v0}, $getObjectReference
                            move-result-object v0
                            check-cast p1, $bottomSheetMenuList
                            invoke-virtual {p1, v0}, $addListReference
                            return-void
                            """
                    )

                    thirdParameter
                }

                val bottomSheetMenuItemBuilderMethod = bottomSheetMenuItemBuilderFingerprint
                    .methodOrThrow()

                val newParameter =
                    bottomSheetMenuItemBuilderMethod.parameters + listOf(customActionClass)

                it.classDef.methods.add(
                    bottomSheetMenuItemBuilderMethod
                        .cloneMutable(
                            accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
                            name = "buildFlyoutMenu",
                            registerCount = bottomSheetMenuItemBuilderMethod.implementation!!.registerCount + 1,
                            parameters = newParameter,
                        ).apply {
                            val drawableIndex = indexOfFirstInstructionOrThrow {
                                opcode == Opcode.INVOKE_DIRECT &&
                                        getReference<MethodReference>()?.returnType == "Landroid/graphics/drawable/Drawable;"
                            }
                            val drawableRegister =
                                getInstruction<OneRegisterInstruction>(drawableIndex + 1).registerA

                            addInstructions(
                                drawableIndex + 2, """
                                    invoke-virtual {p2}, $customActionClass->getDrawable()Landroid/graphics/drawable/Drawable;
                                    move-result-object v$drawableRegister
                                    """
                            )

                            val charSequenceIndex = indexOfSpannedCharSequenceInstruction(this)
                            val charSequenceRegister =
                                getInstruction<OneRegisterInstruction>(charSequenceIndex + 1).registerA

                            val insertIndex = charSequenceIndex + 2

                            if (HIDE_FEED_FLYOUT_MENU.included == true)
                                removeInstructions(insertIndex, 2)

                            addInstructions(
                                insertIndex, """
                                    invoke-virtual {p2}, $customActionClass->getLabel()Ljava/lang/String;
                                    move-result-object v$charSequenceRegister
                                    """
                            )
                        }
                )
            }
        }

        if (is_19_11_or_greater) {
            // The type of the Shorts flyout menu is RecyclerView.
            recyclerViewTreeObserverHook("$EXTENSION_CUSTOM_ACTIONS_CLASS_DESCRIPTOR->onFlyoutMenuCreate(Landroid/support/v7/widget/RecyclerView;)V")
        } else {
            // The type of the Shorts flyout menu is ListView.
            val dismissReference = with(
                bottomSheetMenuDismissFingerprint.methodOrThrow(
                    bottomSheetMenuListBuilderFingerprint
                )
            ) {
                val dismissIndex = indexOfDismissInstruction(this)
                getInstruction<ReferenceInstruction>(dismissIndex).reference
            }

            bottomSheetMenuItemClickFingerprint
                .methodOrThrow(bottomSheetMenuListBuilderFingerprint)
                .addInstructionsWithLabels(
                    0,
                    """
                        invoke-static/range {p2 .. p2}, $EXTENSION_CUSTOM_ACTIONS_CLASS_DESCRIPTOR->onBottomSheetMenuItemClick(Landroid/view/View;)Z
                        move-result v0
                        if-eqz v0, :ignore
                        invoke-virtual {p0}, $dismissReference
                        return-void
                        :ignore
                        nop
                        """,
                )
        }

        // endregion

    }
}

private val shortsNavigationBarPatch = bytecodePatch(
    description = "shortsNavigationBarPatch"
) {
    dependsOn(
        navigationBarHookPatch,
        playerTypeHookPatch,
    )

    execute {
        var count = 0
        classes.forEach { classDef ->
            classDef.methods.filter { method ->
                method.returnType == "V" &&
                        method.accessFlags == AccessFlags.PUBLIC or AccessFlags.FINAL &&
                        method.parameters == listOf("Landroid/view/View;", "Landroid/os/Bundle;") &&
                        method.containsStringInstruction("r_pfvc") &&
                        method.containsLiteralInstruction(bottomBarContainer)
            }.forEach { method ->
                proxy(classDef)
                    .mutableClass
                    .findMutableMethodOf(method).apply {
                        val constIndex = indexOfFirstLiteralInstruction(bottomBarContainer)
                        val targetIndex = indexOfFirstInstructionOrThrow(constIndex) {
                            getReference<MethodReference>()?.name == "getHeight"
                        } + 1
                        val heightRegister =
                            getInstruction<OneRegisterInstruction>(targetIndex).registerA
                        addInstructions(
                            targetIndex + 1, """
                                invoke-static {v$heightRegister}, $SHORTS_CLASS_DESCRIPTOR->setNavigationBarHeight(I)I
                                move-result v$heightRegister
                                """
                        )
                        count++
                    }
            }
        }

        if (count == 0) throw PatchException("shortsNavigationBarPatch failed")

        addBottomBarContainerHook("$SHORTS_CLASS_DESCRIPTOR->setNavigationBar(Landroid/view/View;)V")
    }
}

private const val EXTENSION_REPEAT_STATE_CLASS_DESCRIPTOR =
    "$SHORTS_PATH/ShortsRepeatStatePatch;"

private val shortsRepeatPatch = bytecodePatch(
    description = "shortsRepeatPatch"
) {
    execute {
        dependsOn(
            mainActivityResolvePatch,
            versionCheckPatch,
        )

        injectOnCreateMethodCall(
            EXTENSION_REPEAT_STATE_CLASS_DESCRIPTOR,
            "setMainActivity"
        )

        val endScreenReference = with(reelEnumConstructorFingerprint.methodOrThrow()) {
            val insertIndex = indexOfFirstInstructionOrThrow(Opcode.RETURN_VOID)

            addInstructions(
                insertIndex,
                """
                    # Pass the first enum value to extension.
                    # Any enum value of this type will work.
                    sget-object v0, $definingClass->a:$definingClass
                    invoke-static { v0 }, $EXTENSION_REPEAT_STATE_CLASS_DESCRIPTOR->setYTShortsRepeatEnum(Ljava/lang/Enum;)V
                    """,
            )

            val endScreenStringIndex =
                indexOfFirstStringInstructionOrThrow("REEL_LOOP_BEHAVIOR_END_SCREEN")
            val endScreenReferenceIndex =
                indexOfFirstInstructionOrThrow(endScreenStringIndex, Opcode.SPUT_OBJECT)

            getInstruction<ReferenceInstruction>(endScreenReferenceIndex).reference.toString()
        }

        lateinit var insertMethod: MutableMethod
        var insertMethodFound = false

        if (is_18_49_or_greater) {
            insertMethod = reelPlaybackRepeatFingerprint.methodOrThrow()
        } else {
            val isInsertMethod: Method.() -> Boolean = {
                parameters.size == 1 &&
                        parameterTypes.first().startsWith("L") &&
                        returnType == "V" &&
                        indexOfFirstInstruction {
                            getReference<FieldReference>()?.toString() == endScreenReference
                        } >= 0
            }

            classes.forEach { classDef ->
                if (!insertMethodFound) {
                    classDef.methods.forEach { method ->
                        if (method.isInsertMethod()) {
                            insertMethodFound = true
                            insertMethod = proxy(classDef)
                                .mutableClass
                                .findMutableMethodOf(method)
                        }
                    }
                }
            }
        }

        val enumMethod = reelEnumStaticFingerprint.methodOrThrow(reelEnumConstructorFingerprint)

        insertMethod.apply {
            implementation!!.instructions
                .withIndex()
                .filter { (_, instruction) ->
                    val reference =
                        (instruction as? ReferenceInstruction)?.reference
                    reference is MethodReference &&
                            MethodUtil.methodSignaturesMatch(enumMethod, reference)
                }
                .map { (index, _) -> index }
                .reversed()
                .forEach { index ->
                    val register =
                        getInstruction<OneRegisterInstruction>(index + 1).registerA

                    addInstructions(
                        index + 2, """
                            invoke-static {v$register}, $EXTENSION_REPEAT_STATE_CLASS_DESCRIPTOR->changeShortsRepeatBehavior(Ljava/lang/Enum;)Ljava/lang/Enum;
                            move-result-object v$register
                            """
                    )
                }
        }

        // As of YouTube 20.09, Google has removed the code for 'Autoplay' and 'Pause' from this method.
        // Manually add the 'Autoplay' code that Google removed.
        // Tested on YouTube 20.10.
        if (is_20_09_or_greater) {
            val (directReference, virtualReference) = with(
                reelPlaybackFingerprint.methodOrThrow(
                    videoIdFingerprintShorts
                )
            ) {
                val directIndex = indexOfInitializationInstruction(this)
                val virtualIndex = indexOfFirstInstructionOrThrow(directIndex) {
                    opcode == Opcode.INVOKE_VIRTUAL &&
                            getReference<MethodReference>()?.parameterTypes?.size == 1
                }

                Pair(
                    getInstruction<ReferenceInstruction>(directIndex).reference as MethodReference,
                    getInstruction<ReferenceInstruction>(virtualIndex).reference as MethodReference
                )
            }

            insertMethod.apply {
                val extensionIndex = indexOfFirstInstructionOrThrow {
                    opcode == Opcode.INVOKE_STATIC &&
                            getReference<MethodReference>()?.definingClass == EXTENSION_REPEAT_STATE_CLASS_DESCRIPTOR
                }
                val enumRegister =
                    getInstruction<OneRegisterInstruction>(extensionIndex + 1).registerA
                val freeIndex = indexOfFirstInstructionOrThrow(extensionIndex) {
                    opcode == Opcode.SGET_OBJECT &&
                            getReference<FieldReference>()?.name != "a"
                }
                val freeRegister = getInstruction<OneRegisterInstruction>(freeIndex).registerA
                val getIndex = indexOfFirstInstructionOrThrow(extensionIndex) {
                    val reference = getReference<FieldReference>()
                    opcode == Opcode.IGET_OBJECT &&
                            reference?.definingClass == definingClass &&
                            reference.type == virtualReference.definingClass
                }
                val getReference = getInstruction<ReferenceInstruction>(getIndex).reference

                addInstructionsWithLabels(
                    extensionIndex + 2, """
                        invoke-static {v$enumRegister}, $EXTENSION_REPEAT_STATE_CLASS_DESCRIPTOR->isAutoPlay(Ljava/lang/Enum;)Z
                        move-result v$freeRegister
                        if-eqz v$freeRegister, :ignore
                        new-instance v0, ${directReference.definingClass}
                        const/4 v1, 0x3
                        const/4 v2, 0x0
                        invoke-direct {v0, v1, v2, v2}, $directReference
                        iget-object v3, p0, $getReference
                        invoke-virtual {v3, v0}, $virtualReference
                        return-void
                        :ignore
                        nop
                        """
                )
            }
        }

        if (is_19_34_or_greater) {
            shortsHUDFeatureFingerprint.injectLiteralInstructionBooleanCall(
                SHORTS_HUD_FEATURE_FLAG,
                "0x0"
            )
        }
    }
}

private val shortsTimeStampPatch = bytecodePatch(
    description = "shortsTimeStampPatch"
) {
    dependsOn(versionCheckPatch)

    execute {

        if (!is_19_34_or_greater) {
            return@execute
        }

        // region patch for enable time stamp

        listOf(
            Triple(
                shortsTimeStampPrimarySecondaryFingerprint,
                TIME_STAMP_PRIMARY_FEATURE_FLAG,
                "enableShortsTimeStamp"
            ),
            Triple(
                shortsTimeStampPrimarySecondaryFingerprint,
                TIME_STAMP_SECONDARY_FEATURE_FLAG,
                "enableShortsTimeStamp"
            ),
            Triple(
                shortsTimeStampTertiaryFingerprint,
                TIME_STAMP_TERTIARY_FEATURE_FLAG,
                "enableShortsTimeStamp"
            ),
            Triple(
                shortsTimeStampQuaternaryFingerprint,
                TIME_STAMP_QUATERNARY_FEATURE_FLAG,
                "enableShortsTimeStamp"
            ),
            Triple(
                shortsTimeStampClearDisplayFingerprint,
                TIME_STAMP_CLEAR_DISPLAY_FEATURE_FLAG,
                "enableShortsTimeStampReverse"
            ),
            Triple(
                shortsClearModeFingerprint,
                CLEAR_MODE_FEATURE_FLAG,
                "enableShortsClearMode"
            ),
        ).forEach { (fingerprint, literalValue, methodName) ->
            fingerprint.injectLiteralInstructionBooleanCall(
                literalValue,
                "$SHORTS_CLASS_DESCRIPTOR->$methodName(Z)Z"
            )
        }

        shortsTimeStampPrimarySecondaryFingerprint.methodOrThrow().apply {
            val literalIndex =
                indexOfFirstLiteralInstructionOrThrow(TIME_STAMP_RELATIVE_INDEX_LITERAL)
            val literalRegister = getInstruction<OneRegisterInstruction>(literalIndex).registerA

            addInstructions(
                literalIndex + 1, """
                    invoke-static {v$literalRegister}, $SHORTS_CLASS_DESCRIPTOR->enableShortsTimeStamp(I)I
                    move-result v$literalRegister
                    """
            )
        }

        // endregion

        // Google hasn't finalized this feature, so the layout of the Shorts title and the Shorts timestamp overlap.
        // To fix the layout overlap issue, place the Shorts timestamp below the Shorts title.

        getContext().document("res/layout/consumption_feed_player_overlay.xml").use { document ->
            document.doRecursively loop@{ node ->
                if (node !is Element) return@loop

                node.getAttributeNode("android:id")?.let { attribute ->
                    if (attribute.textContent == "@id/reel_player_footer_container") {
                        node.setAttribute(
                            "yt:layout_constraintBottom_toTopOf",
                            "@id/reel_vod_timestamps_container"
                        )
                    }
                }
            }
        }
    }
}

private val shortsToolBarPatch = bytecodePatch(
    description = "shortsToolBarPatch"
) {
    execute {
        shortsToolBarFingerprint.matchOrThrow().let {
            it.method.apply {
                val insertIndex = it.patternMatch!!.startIndex
                val insertRegister = getInstruction<TwoRegisterInstruction>(insertIndex).registerA

                addInstructions(
                    insertIndex, """
                        invoke-static {v$insertRegister}, $SHORTS_CLASS_DESCRIPTOR->hideShortsToolBar(Z)Z
                        move-result v$insertRegister
                        """
                )
            }
        }
    }
}

private const val EXTENSION_RETURN_YOUTUBE_CHANNEL_NAME_CLASS_DESCRIPTOR =
    "$UTILS_PATH/ReturnYouTubeChannelNamePatch;"

private const val BUTTON_FILTER_CLASS_DESCRIPTOR =
    "$COMPONENTS_PATH/ShortsButtonFilter;"
private const val SHELF_FILTER_CLASS_DESCRIPTOR =
    "$COMPONENTS_PATH/ShortsShelfFilter;"
private const val RETURN_YOUTUBE_CHANNEL_NAME_FILTER_CLASS_DESCRIPTOR =
    "$COMPONENTS_PATH/ReturnYouTubeChannelNameFilterPatch;"

@Suppress("unused")
val shortsComponentPatch = bytecodePatch(
    SHORTS_COMPONENTS.title,
    SHORTS_COMPONENTS.summary,
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(
        settingsPatch,

        shortsAnimationPatch,
        shortsCustomActionsPatch,
        shortsNavigationBarPatch,
        shortsRepeatPatch,
        shortsTimeStampPatch,
        shortsToolBarPatch,

        engagementPanelHookPatch,
        lithoFilterPatch,
        lithoLayoutPatch,
        navigationBarHookPatch,
        playbackStartDescriptorPatch,
        playerTypeHookPatch,
        sharedResourceIdPatch,
        textComponentPatch,
        versionCheckPatch,
        videoInformationPatch,
    )

    execute {
        fun MutableMethod.hideButtons(
            insertIndex: Int,
            descriptor: String
        ) {
            val insertRegister = getInstruction<OneRegisterInstruction>(insertIndex).registerA

            addInstructions(
                insertIndex + 1, """
                    invoke-static {v$insertRegister}, $SHORTS_CLASS_DESCRIPTOR->$descriptor
                    move-result-object v$insertRegister
                    """
            )
        }

        fun Pair<String, Fingerprint>.hideButton(
            id: Long,
            descriptor: String,
            reversed: Boolean
        ) =
            methodOrThrow().apply {
                val constIndex = indexOfFirstLiteralInstructionOrThrow(id)
                val insertIndex = if (reversed)
                    indexOfFirstInstructionReversedOrThrow(constIndex, Opcode.CHECK_CAST)
                else
                    indexOfFirstInstructionOrThrow(constIndex, Opcode.CHECK_CAST)
                val insertRegister = getInstruction<OneRegisterInstruction>(insertIndex).registerA

                addInstruction(
                    insertIndex,
                    "invoke-static {v$insertRegister}, $SHORTS_CLASS_DESCRIPTOR->$descriptor(Landroid/view/View;)V"
                )
            }

        fun Pair<String, Fingerprint>.hideButtons(
            id: Long,
            descriptor: String
        ) =
            methodOrThrow().apply {
                val constIndex = indexOfFirstLiteralInstructionOrThrow(id)
                val insertIndex = indexOfFirstInstructionOrThrow(constIndex, Opcode.CHECK_CAST)

                hideButtons(insertIndex, descriptor)
            }

        var settingArray = arrayOf(
            "PREFERENCE_SCREEN: SHORTS",
            "SETTINGS: SHORTS_COMPONENTS"
        )

        if (is_18_34_or_greater) {
            settingArray += "SETTINGS: SHORTS_CUSTOM_ACTIONS_SHARED"
            settingArray += "SETTINGS: SHORTS_CUSTOM_ACTIONS_TOOLBAR"
        }

        if (is_19_02_or_greater) {
            settingArray += "SETTINGS: SHORTS_CUSTOM_ACTIONS_FLYOUT_MENU"
        }

        if (is_19_34_or_greater) {
            settingArray += "SETTINGS: SHORTS_REPEAT_STATE_BACKGROUND"
            settingArray += "SETTINGS: SHORTS_TIME_STAMP"
        } else {
            settingArray += "SETTINGS: SHORTS_PLAY_PAUSE_BUTTON_BACKGROUND"
        }

        // region patch for hide comments button (non-litho)

        shortsButtonFingerprint.hideButton(rightComment, "hideShortsCommentsButton", false)

        // endregion

        // region patch for hide dislike button (non-litho)

        shortsButtonFingerprint.methodOrThrow().apply {
            val constIndex =
                indexOfFirstLiteralInstructionOrThrow(reelRightDislikeIcon)
            val constRegister = getInstruction<OneRegisterInstruction>(constIndex).registerA

            val jumpIndex = indexOfFirstInstructionOrThrow(constIndex, Opcode.CONST_CLASS) + 2

            addInstructionsWithLabels(
                constIndex + 1, """
                    invoke-static {}, $SHORTS_CLASS_DESCRIPTOR->hideShortsDislikeButton()Z
                    move-result v$constRegister
                    if-nez v$constRegister, :hide
                    const v$constRegister, $reelRightDislikeIcon
                    """, ExternalLabel("hide", getInstruction(jumpIndex))
            )
        }

        // endregion

        // region patch for hide like button (non-litho)

        shortsButtonFingerprint.methodOrThrow().apply {
            val insertIndex = indexOfFirstLiteralInstructionOrThrow(reelRightLikeIcon)
            val insertRegister = getInstruction<OneRegisterInstruction>(insertIndex).registerA
            val jumpIndex = indexOfFirstInstructionOrThrow(insertIndex, Opcode.CONST_CLASS) + 2

            addInstructionsWithLabels(
                insertIndex + 1, """
                    invoke-static {}, $SHORTS_CLASS_DESCRIPTOR->hideShortsLikeButton()Z
                    move-result v$insertRegister
                    if-nez v$insertRegister, :hide
                    const v$insertRegister, $reelRightLikeIcon
                    """, ExternalLabel("hide", getInstruction(jumpIndex))
            )
        }

        // endregion

        // region patch for hide sound button

        if (shortsPivotLegacyFingerprint.resolvable()) {
            // Legacy method.
            shortsPivotLegacyFingerprint.methodOrThrow().apply {
                val targetIndex =
                    indexOfFirstLiteralInstructionOrThrow(reelForcedMuteButton)
                val targetRegister = getInstruction<OneRegisterInstruction>(targetIndex).registerA

                val insertIndex = indexOfFirstInstructionReversedOrThrow(targetIndex, Opcode.IF_EQZ)
                val jumpIndex = indexOfFirstInstructionOrThrow(targetIndex, Opcode.GOTO)

                addInstructionsWithLabels(
                    insertIndex, """
                        invoke-static {}, $SHORTS_CLASS_DESCRIPTOR->hideShortsSoundButton()Z
                        move-result v$targetRegister
                        if-nez v$targetRegister, :hide
                        """, ExternalLabel("hide", getInstruction(jumpIndex))
                )
            }
        } else if (reelPlayerRightPivotV2Size != -1L) {
            // Invoke Sound button dimen into extension.
            val smaliInstruction = """
                invoke-static {v$REGISTER_TEMPLATE_REPLACEMENT}, $SHORTS_CLASS_DESCRIPTOR->getShortsSoundButtonDimenId(I)I
                move-result v$REGISTER_TEMPLATE_REPLACEMENT
                """

            replaceLiteralInstructionCall(
                reelPlayerRightPivotV2Size,
                smaliInstruction
            )
        } else {
            throw PatchException("ReelPlayerRightPivotV2Size is not found")
        }

        // endregion

        // region patch for hide remix button (non-litho)

        shortsButtonFingerprint.hideButton(reelDynRemix, "hideShortsRemixButton", true)

        // endregion

        // region patch for hide share button (non-litho)

        shortsButtonFingerprint.hideButton(reelDynShare, "hideShortsShareButton", true)

        // endregion

        // region patch for hide subscribe button (non-litho)

        // This method is deprecated since YouTube v18.31.xx.
        if (!is_18_31_or_greater) {
            val subscriptionFieldReference =
                with(shortsSubscriptionsTabletParentFingerprint.methodOrThrow()) {
                    val targetIndex =
                        indexOfFirstLiteralInstructionOrThrow(reelPlayerFooter) - 1
                    (getInstruction<ReferenceInstruction>(targetIndex)).reference as FieldReference
                }
            shortsSubscriptionsTabletFingerprint.methodOrThrow(
                shortsSubscriptionsTabletParentFingerprint
            ).apply {
                implementation!!.instructions.filter { instruction ->
                    val fieldReference =
                        (instruction as? ReferenceInstruction)?.reference as? FieldReference
                    instruction.opcode == Opcode.IGET &&
                            fieldReference == subscriptionFieldReference
                }.forEach { instruction ->
                    val insertIndex = implementation!!.instructions.indexOf(instruction) + 1
                    val register = (instruction as TwoRegisterInstruction).registerA

                    addInstructions(
                        insertIndex, """
                            invoke-static {v$register}, $SHORTS_CLASS_DESCRIPTOR->hideShortsSubscribeButton(I)I
                            move-result v$register
                            """
                    )
                }
            }
        }

        // endregion

        // region patch for hide paused header

        shortsPausedHeaderFingerprint.matchOrThrow().let {
            it.method.apply {
                val targetIndex = it.patternMatch!!.endIndex + 1
                val targetInstruction = getInstruction(targetIndex)
                val targetReference =
                    (targetInstruction as? ReferenceInstruction)?.reference as? MethodReference
                val useMethodWalker = targetInstruction.opcode == Opcode.INVOKE_VIRTUAL &&
                        targetReference?.returnType == "V" &&
                        targetReference.parameterTypes.firstOrNull() == "Landroid/view/View;"

                if (useMethodWalker) {
                    // YouTube 18.29.38 ~ YouTube 19.28.42
                    getWalkerMethod(targetIndex).apply {
                        addInstructionsWithLabels(
                            0, """
                                invoke-static {}, $SHORTS_CLASS_DESCRIPTOR->hideShortsPausedHeader()Z
                                move-result v0
                                if-eqz v0, :show
                                return-void
                                """, ExternalLabel("show", getInstruction(0))
                        )
                    }
                } else {
                    // YouTube 19.29.42 ~
                    val insertIndex = it.patternMatch!!.startIndex
                    val insertRegister =
                        getInstruction<OneRegisterInstruction>(insertIndex).registerA

                    addInstructions(
                        insertIndex, """
                            invoke-static {v$insertRegister}, $SHORTS_CLASS_DESCRIPTOR->hideShortsPausedHeader(Z)Z
                            move-result v$insertRegister
                            """
                    )
                }
            }
        }

        // endregion

        // region patch for return shorts channel name

        hookSpannableString(
            EXTENSION_RETURN_YOUTUBE_CHANNEL_NAME_CLASS_DESCRIPTOR,
            "onCharSequenceLoaded"
        )

        hookShortsVideoInformation("$EXTENSION_RETURN_YOUTUBE_CHANNEL_NAME_CLASS_DESCRIPTOR->newShortsVideoStarted(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;JZ)V")

        // endregion

        // region patch for restore shorts old player layout

        if (!is_19_25_or_greater) {
            shortsFullscreenFeatureFingerprint.injectLiteralInstructionBooleanCall(
                FULLSCREEN_FEATURE_FLAG,
                "$SHORTS_CLASS_DESCRIPTOR->restoreShortsOldPlayerLayout()Z"
            )
            settingArray += "SETTINGS: RESTORE_SHORTS_OLD_PLAYER_LAYOUT"
        }

        // endregion

        // region patch for open Shorts in regular player

        fun extensionInstructions(playbackStartRegister: Int, freeRegister: Int) =
            """
                invoke-virtual { v$playbackStartRegister }, $playbackStartVideoIdReference
                move-result-object v$freeRegister
                invoke-static { v$freeRegister }, $SHORTS_CLASS_DESCRIPTOR->openShortInRegularPlayer(Ljava/lang/String;)Z
                move-result v$freeRegister
                if-eqz v$freeRegister, :disabled
                return-void
                :disabled
                nop
            """

        if (is_19_25_or_greater) {
            shortsPlaybackStartIntentFingerprint.methodOrThrow().addInstructionsWithLabels(
                0,
                """
                    move-object/from16 v0, p1
                    ${extensionInstructions(0, 1)}
                    """
            )
        } else {
            shortsPlaybackStartIntentLegacyFingerprint.methodOrThrow().apply {
                val index = indexOfFirstInstructionOrThrow {
                    getReference<MethodReference>()?.returnType == PLAYBACK_START_DESCRIPTOR_CLASS_DESCRIPTOR
                }
                val freeRegister = getInstruction<FiveRegisterInstruction>(index).registerC
                val playbackStartRegister =
                    getInstruction<OneRegisterInstruction>(index + 1).registerA

                addInstructionsWithLabels(
                    index + 2,
                    extensionInstructions(playbackStartRegister, freeRegister)
                )
            }
        }

        // endregion

        // region Disable experimental Shorts flags.

        // Flags might be present in earlier targets, but they are not found in 19.47.53.
        // If these flags are forced on, the experimental layout is still not used and
        // it appears the features requires additional server side data to fully use.
        if (is_20_07_or_greater) {
            mapOf(
                // Experimental Shorts player uses Android native buttons and not Litho,
                // and the layout is provided by the server.
                //
                // Since the buttons are native components and not Litho, it should be possible to
                // fix the RYD Shorts loading delay by asynchronously loading RYD and updating
                // the button text after RYD has loaded.
                shortsExperimentalPlayerFeatureFlagFingerprint to SHORTS_EXPERIMENTAL_PLAYER_FEATURE_FLAG,

                // Experimental UI renderer must also be disabled since it requires the
                // experimental Shorts player.  If this is enabled but Shorts player
                // is disabled then the app crashes when the Shorts player is opened.
                renderNextUIFeatureFlagFingerprint to RENDER_NEXT_UI_FEATURE_FLAG
            ).forEach { (fingerprint, literal) ->
                fingerprint.injectLiteralInstructionBooleanCall(
                    literal,
                    "0x0"
                )
            }
        }

        // endregion

        addLithoFilter(BUTTON_FILTER_CLASS_DESCRIPTOR)
        addLithoFilter(SHELF_FILTER_CLASS_DESCRIPTOR)
        addLithoFilter(RETURN_YOUTUBE_CHANNEL_NAME_FILTER_CLASS_DESCRIPTOR)

        // region add settings

        addPreference(settingArray, SHORTS_COMPONENTS)

        // endregion

    }
}
