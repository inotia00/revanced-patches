package app.revanced.patches.youtube.player.components

import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.extensions.InstructionExtensions.removeInstruction
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.shared.litho.addLithoFilter
import app.revanced.patches.shared.litho.lithoFilterPatch
import app.revanced.patches.shared.spans.addSpanFilter
import app.revanced.patches.shared.spans.inclusiveSpanPatch
import app.revanced.patches.shared.startVideoInformerFingerprint
import app.revanced.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.controlsoverlay.controlsOverlayConfigPatch
import app.revanced.patches.youtube.utils.engagement.engagementPanelBuilderMethod
import app.revanced.patches.youtube.utils.engagement.engagementPanelFreeRegister
import app.revanced.patches.youtube.utils.engagement.engagementPanelHookPatch
import app.revanced.patches.youtube.utils.engagement.engagementPanelIdIndex
import app.revanced.patches.youtube.utils.engagement.engagementPanelIdRegister
import app.revanced.patches.youtube.utils.extension.Constants.COMPONENTS_PATH
import app.revanced.patches.youtube.utils.extension.Constants.PLAYER_CLASS_DESCRIPTOR
import app.revanced.patches.youtube.utils.extension.Constants.SPANS_PATH
import app.revanced.patches.youtube.utils.extension.sharedExtensionPatch
import app.revanced.patches.youtube.utils.fix.endscreensuggestedvideo.endScreenSuggestedVideoPatch
import app.revanced.patches.youtube.utils.fix.litho.lithoLayoutPatch
import app.revanced.patches.youtube.utils.patch.PatchList.PLAYER_COMPONENTS
import app.revanced.patches.youtube.utils.playertype.playerTypeHookPatch
import app.revanced.patches.youtube.utils.playservice.is_19_18_or_greater
import app.revanced.patches.youtube.utils.playservice.is_20_02_or_greater
import app.revanced.patches.youtube.utils.playservice.is_20_03_or_greater
import app.revanced.patches.youtube.utils.playservice.is_20_05_or_greater
import app.revanced.patches.youtube.utils.playservice.is_20_12_or_greater
import app.revanced.patches.youtube.utils.playservice.versionCheckPatch
import app.revanced.patches.youtube.utils.resourceid.darkBackground
import app.revanced.patches.youtube.utils.resourceid.fadeDurationFast
import app.revanced.patches.youtube.utils.resourceid.scrimOverlay
import app.revanced.patches.youtube.utils.resourceid.seekUndoEduOverlayStub
import app.revanced.patches.youtube.utils.resourceid.sharedResourceIdPatch
import app.revanced.patches.youtube.utils.resourceid.tapBloomView
import app.revanced.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.revanced.patches.youtube.utils.settings.settingsPatch
import app.revanced.patches.youtube.utils.youtubeControlsOverlayFingerprint
import app.revanced.patches.youtube.video.information.hookVideoInformation
import app.revanced.patches.youtube.video.information.videoInformationPatch
import app.revanced.util.REGISTER_TEMPLATE_REPLACEMENT
import app.revanced.util.Utils.printWarn
import app.revanced.util.findMethodOrThrow
import app.revanced.util.findMutableMethodOf
import app.revanced.util.fingerprint.injectLiteralInstructionBooleanCall
import app.revanced.util.fingerprint.injectLiteralInstructionViewCall
import app.revanced.util.fingerprint.matchOrThrow
import app.revanced.util.fingerprint.methodOrThrow
import app.revanced.util.fingerprint.mutableClassOrThrow
import app.revanced.util.getReference
import app.revanced.util.getWalkerMethod
import app.revanced.util.indexOfFirstInstruction
import app.revanced.util.indexOfFirstInstructionOrThrow
import app.revanced.util.indexOfFirstInstructionReversedOrThrow
import app.revanced.util.indexOfFirstLiteralInstructionOrThrow
import app.revanced.util.or
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.NarrowLiteralInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ThreeRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.WideLiteralInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private val speedOverlayPatch = bytecodePatch(
    description = "speedOverlayPatch"
) {
    dependsOn(
        sharedExtensionPatch,
        sharedResourceIdPatch,
        versionCheckPatch,
    )

    execute {
        fun MutableMethod.hookSpeedOverlay(
            insertIndex: Int,
            insertRegister: Int,
            jumpIndex: Int
        ) {
            addInstructionsWithLabels(
                insertIndex, """
                    invoke-static {}, $PLAYER_CLASS_DESCRIPTOR->disableSpeedOverlay()Z
                    move-result v$insertRegister
                    if-eqz v$insertRegister, :disable
                    """, ExternalLabel("disable", getInstruction(jumpIndex))
            )
        }

        fun MutableMethod.hookRelativeSpeedValue(startIndex: Int) {
            val relativeIndex = indexOfFirstInstructionOrThrow(startIndex, Opcode.CMPL_FLOAT)
            val relativeRegister = getInstruction<ThreeRegisterInstruction>(relativeIndex).registerB

            addInstructions(
                relativeIndex, """
                    invoke-static {v$relativeRegister}, $PLAYER_CLASS_DESCRIPTOR->speedOverlayRelativeValue(F)F
                    move-result v$relativeRegister
                    """
            )
        }

        if (!is_19_18_or_greater) {
            // Used on YouTube 18.29.38 ~ YouTube 19.17.41

            // region patch for Disable speed overlay (Enable slide to seek)

            mapOf(
                restoreSlideToSeekBehaviorFingerprint to RESTORE_SLIDE_TO_SEEK_FEATURE_FLAG,
                speedOverlayFingerprint to SPEED_OVERLAY_FEATURE_FLAG
            ).forEach { (fingerprint, literal) ->
                fingerprint.injectLiteralInstructionBooleanCall(
                    literal,
                    "$PLAYER_CLASS_DESCRIPTOR->disableSpeedOverlay(Z)Z"
                )
            }

            // endregion

            // region patch for Custom speed overlay float value

            val speedFieldReference = with(speedOverlayFloatValueFingerprint.methodOrThrow()) {
                val literalIndex =
                    indexOfFirstLiteralInstructionOrThrow(SPEED_OVERLAY_LEGACY_FEATURE_FLAG)
                val floatIndex =
                    indexOfFirstInstructionOrThrow(literalIndex, Opcode.DOUBLE_TO_FLOAT)
                val floatRegister = getInstruction<TwoRegisterInstruction>(floatIndex).registerA

                addInstructions(
                    floatIndex + 1, """
                        invoke-static {v$floatRegister}, $PLAYER_CLASS_DESCRIPTOR->speedOverlayValue(F)F
                        move-result v$floatRegister
                        """
                )

                val speedFieldIndex = indexOfFirstInstructionOrThrow(literalIndex) {
                    opcode == Opcode.IPUT &&
                            getReference<FieldReference>()?.type == "F"
                }

                getInstruction<ReferenceInstruction>(speedFieldIndex).reference.toString()
            }

            fun indexOfFirstSpeedFieldInstruction(method: Method) =
                method.indexOfFirstInstruction {
                    opcode == Opcode.IGET &&
                            getReference<FieldReference>()?.toString() == speedFieldReference
                }

            val isSyntheticMethod: Method.() -> Boolean = {
                name == "run" &&
                        accessFlags == AccessFlags.PUBLIC or AccessFlags.FINAL &&
                        parameterTypes.isEmpty() &&
                        indexOfFirstSpeedFieldInstruction(this) >= 0 &&
                        indexOfFirstInstruction(Opcode.CMPL_FLOAT) >= 0
            }

            classes.forEach { classDef ->
                classDef.methods.forEach { method ->
                    if (method.isSyntheticMethod()) {
                        proxy(classDef)
                            .mutableClass
                            .findMutableMethodOf(method)
                            .apply {
                                val speedFieldIndex = indexOfFirstSpeedFieldInstruction(this)
                                hookRelativeSpeedValue(speedFieldIndex)
                            }
                    }
                }
            }

            // endregion

        } else {
            // Used on YouTube 19.18.41~

            // region patch for Disable speed overlay (Enable slide to seek)

            nextGenWatchLayoutFingerprint.methodOrThrow().apply {
                val booleanValueIndex = indexOfFirstInstructionOrThrow {
                    getReference<MethodReference>()?.name == "booleanValue"
                }
                val insertIndex = indexOfFirstInstructionOrThrow(booleanValueIndex - 10) {
                    opcode == Opcode.IGET_OBJECT &&
                            getReference<FieldReference>()?.definingClass == definingClass
                }
                val insertInstruction = getInstruction<TwoRegisterInstruction>(insertIndex)
                val insertReference = getInstruction<ReferenceInstruction>(insertIndex).reference

                addInstruction(
                    insertIndex + 1,
                    "iget-object v${insertInstruction.registerA}, v${insertInstruction.registerB}, $insertReference"
                )

                val jumpIndex = indexOfFirstInstructionOrThrow(booleanValueIndex) {
                    opcode == Opcode.IGET_OBJECT &&
                            getReference<FieldReference>()?.definingClass == definingClass
                }

                hookSpeedOverlay(insertIndex + 1, insertInstruction.registerA, jumpIndex)
            }

            val (slideToSeekBooleanMethod, slideToSeekSyntheticMethod) =
                slideToSeekMotionEventFingerprint.matchOrThrow(
                    horizontalTouchOffsetConstructorFingerprint
                ).let {
                    with(it.method) {
                        val patternMatch = it.patternMatch!!
                        val jumpIndex = patternMatch.endIndex + 1
                        val insertIndex = patternMatch.endIndex - 1
                        val insertRegister =
                            getInstruction<TwoRegisterInstruction>(insertIndex).registerA

                        hookSpeedOverlay(insertIndex, insertRegister, jumpIndex)

                        val slideToSeekBooleanMethod =
                            getWalkerMethod(patternMatch.startIndex + 1)

                        val slideToSeekConstructorMethod =
                            findMethodOrThrow(slideToSeekBooleanMethod.definingClass)

                        val slideToSeekSyntheticIndex = slideToSeekConstructorMethod
                            .indexOfFirstInstructionReversedOrThrow {
                                opcode == Opcode.NEW_INSTANCE
                            }

                        val slideToSeekSyntheticClass = slideToSeekConstructorMethod
                            .getInstruction<ReferenceInstruction>(slideToSeekSyntheticIndex)
                            .reference
                            .toString()

                        val slideToSeekSyntheticMethod =
                            findMethodOrThrow(slideToSeekSyntheticClass) {
                                name == "run"
                            }

                        Pair(slideToSeekBooleanMethod, slideToSeekSyntheticMethod)
                    }
                }

            slideToSeekBooleanMethod.apply {
                val insertIndex = indexOfFirstInstructionOrThrow {
                    opcode == Opcode.IGET_OBJECT
                }
                val insertRegister = getInstruction<TwoRegisterInstruction>(insertIndex).registerA
                val jumpIndex = indexOfFirstInstructionReversedOrThrow {
                    opcode == Opcode.INVOKE_VIRTUAL
                }

                hookSpeedOverlay(insertIndex, insertRegister, jumpIndex)
            }

            slideToSeekSyntheticMethod.apply {
                val speedOverlayFloatValueIndex = indexOfFirstInstructionOrThrow {
                    (this as? NarrowLiteralInstruction)?.narrowLiteral == 2.0f.toRawBits()
                }
                val insertIndex =
                    indexOfFirstInstructionReversedOrThrow(speedOverlayFloatValueIndex) {
                        getReference<MethodReference>()?.name == "removeCallbacks"
                    } + 1
                val insertRegister =
                    getInstruction<FiveRegisterInstruction>(insertIndex - 1).registerC
                val jumpIndex =
                    indexOfFirstInstructionOrThrow(
                        speedOverlayFloatValueIndex,
                        Opcode.RETURN_VOID
                    ) + 1

                hookSpeedOverlay(insertIndex, insertRegister, jumpIndex)
            }

            // endregion

            // region patch for Custom speed overlay float value

            slideToSeekSyntheticMethod.apply {
                val speedOverlayFloatValueIndex = indexOfFirstInstructionOrThrow {
                    (this as? NarrowLiteralInstruction)?.narrowLiteral == 2.0f.toRawBits()
                }
                val speedOverlayFloatValueRegister =
                    getInstruction<OneRegisterInstruction>(speedOverlayFloatValueIndex).registerA

                addInstructions(
                    speedOverlayFloatValueIndex + 1, """
                        invoke-static {v$speedOverlayFloatValueRegister}, $PLAYER_CLASS_DESCRIPTOR->speedOverlayValue(F)F
                        move-result v$speedOverlayFloatValueRegister
                        """
                )

                hookRelativeSpeedValue(speedOverlayFloatValueIndex)
            }

            // Removed in YouTube 20.03+
            if (!is_20_03_or_greater) {
                speedOverlayTextValueFingerprint.matchOrThrow().let {
                    it.method.apply {
                        val targetIndex = it.patternMatch!!.startIndex
                        val targetRegister =
                            getInstruction<OneRegisterInstruction>(targetIndex).registerA

                        addInstructions(
                            targetIndex + 1, """
                                invoke-static {}, $PLAYER_CLASS_DESCRIPTOR->speedOverlayValue()D
                                move-result-wide v$targetRegister
                                """
                        )
                    }
                }
            }

            // endregion

        }
    }
}

private const val PLAYER_COMPONENTS_FILTER_CLASS_DESCRIPTOR =
    "$COMPONENTS_PATH/PlayerComponentsFilter;"
private const val SANITIZE_VIDEO_SUBTITLE_FILTER_CLASS_DESCRIPTOR =
    "$SPANS_PATH/SanitizeVideoSubtitleFilter;"

@Suppress("unused")
val playerComponentsPatch = bytecodePatch(
    PLAYER_COMPONENTS.title,
    PLAYER_COMPONENTS.summary,
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(
        settingsPatch,
        controlsOverlayConfigPatch,
        endScreenSuggestedVideoPatch,
        inclusiveSpanPatch,
        lithoFilterPatch,
        lithoLayoutPatch,
        playerTypeHookPatch,
        sharedResourceIdPatch,
        speedOverlayPatch,
        videoInformationPatch,
        versionCheckPatch,
        engagementPanelHookPatch,
    )

    execute {
        var settingArray = arrayOf(
            "PREFERENCE_SCREEN: PLAYER",
            "SETTINGS: PLAYER_COMPONENTS"
        )

        fun MutableMethod.getAllLiteralComponent(
            startIndex: Int,
            endIndex: Int
        ): String {
            var literalComponent = ""
            for (index in startIndex..endIndex) {
                val opcode = getInstruction(index).opcode
                if (opcode != Opcode.CONST_16 && opcode != Opcode.CONST_4)
                    continue

                val register = getInstruction<OneRegisterInstruction>(index).registerA
                val value = getInstruction<WideLiteralInstruction>(index).wideLiteral.toInt()

                val line = """
                const/16 v$register, $value
                
                """.trimIndent()

                literalComponent += line
            }

            return literalComponent
        }

        fun MutableMethod.getFirstLiteralComponent(
            startIndex: Int,
            endIndex: Int
        ): String {
            val constRegister =
                getInstruction<FiveRegisterInstruction>(endIndex).registerE

            for (index in endIndex downTo startIndex) {
                val instruction = getInstruction(index)
                if (instruction.opcode != Opcode.CONST_16 && instruction.opcode != Opcode.CONST_4)
                    continue

                if ((instruction as OneRegisterInstruction).registerA != constRegister)
                    continue

                val constValue = (instruction as WideLiteralInstruction).wideLiteral.toInt()

                return "const/16 v$constRegister, $constValue"
            }
            return ""
        }

        // region patch for custom player overlay opacity

        youtubeControlsOverlayFingerprint.methodOrThrow().apply {
            val constIndex = indexOfFirstLiteralInstructionOrThrow(scrimOverlay)
            val targetIndex = indexOfFirstInstructionOrThrow(constIndex, Opcode.CHECK_CAST)
            val targetParameter = getInstruction<ReferenceInstruction>(targetIndex).reference
            val targetRegister = getInstruction<OneRegisterInstruction>(targetIndex).registerA

            if (!targetParameter.toString().endsWith("Landroid/widget/ImageView;"))
                throw PatchException("Method signature parameter did not match: $targetParameter")

            addInstruction(
                targetIndex + 1,
                "invoke-static {v$targetRegister}, $PLAYER_CLASS_DESCRIPTOR->changeOpacity(Landroid/widget/ImageView;)V"
            )
        }

        // endregion

        // region patch for disable auto player popup panels

        fun MutableMethod.hookInitVideoPanel(initVideoPanel: Int) =
            addInstructions(
                0, """
                    const/4 v0, $initVideoPanel
                    invoke-static {v0}, $PLAYER_CLASS_DESCRIPTOR->setInitVideoPanel(Z)V
                    """
            )

        if (is_20_05_or_greater) {
            engagementPanelBuilderMethod.addInstructionsWithLabels(
                engagementPanelIdIndex, """
                    move/from16 v$engagementPanelFreeRegister, p4
                    invoke-static {v$engagementPanelFreeRegister, v$engagementPanelIdRegister}, $PLAYER_CLASS_DESCRIPTOR->disableAutoPlayerPopupPanels(ZLjava/lang/String;)Z
                    move-result v$engagementPanelFreeRegister
                    if-eqz v$engagementPanelFreeRegister, :shown
                    const/4 v$engagementPanelFreeRegister, 0x0
                    return-object v$engagementPanelFreeRegister
                    :shown
                    nop
                    """
            )
            hookVideoInformation("$PLAYER_CLASS_DESCRIPTOR->disableAutoPlayerPopupPanels(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;JZ)V")
        } else {
            arrayOf(
                lithoComponentOnClickListenerFingerprint,
                offlineActionsOnClickListenerFingerprint,
            ).forEach { fingerprint ->
                fingerprint.methodOrThrow().apply {
                    val syntheticIndex =
                        indexOfFirstInstruction(Opcode.NEW_INSTANCE)
                    if (syntheticIndex >= 0) {
                        val syntheticReference =
                            getInstruction<ReferenceInstruction>(syntheticIndex).reference.toString()

                        findMethodOrThrow(syntheticReference) {
                            name == "onClick"
                        }.hookInitVideoPanel(0)
                    } else {
                        printWarn("target Opcode not found in ${fingerprint.first}")
                    }
                }
            }

            findMethodOrThrow(
                engagementPanelPlaylistSyntheticFingerprint.methodOrThrow().definingClass
            ) {
                name == "onClick"
            }.hookInitVideoPanel(0)

            startVideoInformerFingerprint.methodOrThrow().hookInitVideoPanel(1)

            engagementPanelBuilderMethod.addInstructionsWithLabels(
                0, """
                    move/from16 v0, p4
                    invoke-static {v0}, $PLAYER_CLASS_DESCRIPTOR->disableAutoPlayerPopupPanels(Z)Z
                    move-result v0
                    if-eqz v0, :shown
                    const/4 v0, 0x0
                    return-object v0
                    :shown
                    nop
                    """
            )
        }

        // endregion

        // region patch for disable auto switch mix playlists

        hookVideoInformation("$PLAYER_CLASS_DESCRIPTOR->disableAutoSwitchMixPlaylists(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;JZ)V")

        // endregion

        // region patch for hide channel watermark

        watermarkFingerprint.matchOrThrow(watermarkParentFingerprint).let {
            it.method.apply {
                val insertIndex = it.patternMatch!!.endIndex
                val register = getInstruction<TwoRegisterInstruction>(insertIndex).registerA

                addInstructions(
                    insertIndex + 1, """
                        invoke-static {v$register}, $PLAYER_CLASS_DESCRIPTOR->hideChannelWatermark(Z)Z
                        move-result v$register
                        """
                )
            }
        }

        // endregion

        // region patch for hide crowdfunding box

        crowdfundingBoxFingerprint.matchOrThrow().let {
            it.method.apply {
                val insertIndex = it.patternMatch!!.endIndex
                val register = getInstruction<TwoRegisterInstruction>(insertIndex).registerA

                addInstruction(
                    insertIndex,
                    "invoke-static {v$register}, $PLAYER_CLASS_DESCRIPTOR->hideCrowdfundingBox(Landroid/view/View;)V"
                )
            }
        }

        // endregion

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

        // endregion

        // region patch for hide end screen cards

        listOf(
            layoutCircleFingerprint,
            layoutIconFingerprint,
            layoutVideoFingerprint
        ).forEach { fingerprint ->
            fingerprint.matchOrThrow().let {
                it.method.apply {
                    val insertIndex = it.patternMatch!!.endIndex
                    val viewRegister = getInstruction<OneRegisterInstruction>(insertIndex).registerA

                    addInstruction(
                        insertIndex + 1,
                        "invoke-static { v$viewRegister }, $PLAYER_CLASS_DESCRIPTOR->hideEndScreenCards(Landroid/view/View;)V"
                    )
                }
            }
        }

        // endregion

        // region patch for hide filmstrip overlay

        fun MutableMethod.hookFilmstripOverlay(
            index: Int = 0,
            register: Int = 0
        ) {
            val stringInstructions = if (returnType == "Z")
                """
                    const/4 v$register, 0x0
                    return v$register
                """
            else if (returnType == "V")
                """
                    return-void
                """
            else
                throw Exception("This case should never happen.")

            addInstructionsWithLabels(
                index, """
                    invoke-static {}, $PLAYER_CLASS_DESCRIPTOR->hideFilmstripOverlay()Z
                    move-result v$register
                    if-eqz v$register, :shown
                    """ + stringInstructions + """
                        :shown
                        nop
                    """
            )
        }

        val filmStripOverlayFingerprints = mutableListOf(
            filmStripOverlayInteractionFingerprint,
            filmStripOverlayPreviewFingerprint
        )

        if (is_20_12_or_greater) {
            filmStripOverlayMotionEventPrimaryFingerprint.matchOrThrow(
                filmStripOverlayStartParentFingerprint
            ).let {
                it.method.apply {
                    val index = it.patternMatch!!.startIndex
                    val register = getInstruction<TwoRegisterInstruction>(index).registerA

                    hookFilmstripOverlay(index, register)
                }
            }

            filmStripOverlayMotionEventSecondaryFingerprint.matchOrThrow(
                filmStripOverlayStartParentFingerprint
            ).let {
                it.method.apply {
                    val index = it.patternMatch!!.startIndex + 2
                    val register = getInstruction<OneRegisterInstruction>(index).registerA

                    addInstructions(
                        index, """
                            invoke-static {v$register}, $PLAYER_CLASS_DESCRIPTOR->hideFilmstripOverlay(Z)Z
                            move-result v$register
                            """
                    )
                }
            }
        } else {
            filmStripOverlayFingerprints += filmStripOverlayConfigFingerprint
        }

        filmStripOverlayFingerprints.forEach { fingerprint ->
            fingerprint.methodOrThrow(filmStripOverlayEnterParentFingerprint).hookFilmstripOverlay()
        }

        // Removed in YouTube 20.03+
        if (!is_20_03_or_greater) {
            youtubeControlsOverlayFingerprint.methodOrThrow().apply {
                val constIndex = indexOfFirstLiteralInstructionOrThrow(fadeDurationFast)
                val constRegister = getInstruction<OneRegisterInstruction>(constIndex).registerA
                val insertIndex =
                    indexOfFirstInstructionReversedOrThrow(constIndex, Opcode.INVOKE_VIRTUAL) + 1
                val jumpIndex = implementation!!.instructions.let { instruction ->
                    insertIndex + instruction.subList(insertIndex, instruction.size - 1)
                        .indexOfFirst { instructions ->
                            instructions.opcode == Opcode.GOTO || instructions.opcode == Opcode.GOTO_16
                        }
                }

                val replaceInstruction = getInstruction<TwoRegisterInstruction>(insertIndex)
                val replaceReference =
                    getInstruction<ReferenceInstruction>(insertIndex).reference

                addInstructionsWithLabels(
                    insertIndex + 1, getAllLiteralComponent(insertIndex, jumpIndex - 1) + """
                        const v$constRegister, $fadeDurationFast
                        invoke-static {}, $PLAYER_CLASS_DESCRIPTOR->hideFilmstripOverlay()Z
                        move-result v${replaceInstruction.registerA}
                        if-nez v${replaceInstruction.registerA}, :hidden
                        iget-object v${replaceInstruction.registerA}, v${replaceInstruction.registerB}, $replaceReference
                        """, ExternalLabel("hidden", getInstruction(jumpIndex))
                )
                removeInstruction(insertIndex)
            }
        } else if (is_20_05_or_greater) {
            // This is a new film strip overlay added to YouTube 20.05+
            // Disabling this flag is not related to the operation of the patch.
            filmStripOverlayConfigV2Fingerprint.injectLiteralInstructionBooleanCall(
                FILM_STRIP_OVERLAY_V2_FEATURE_FLAG,
                "0x0"
            )
        }

        // endregion

        // region patch for hide info cards

        infoCardsIncognitoFingerprint.matchOrThrow().let {
            it.method.apply {
                val targetIndex = it.patternMatch!!.startIndex
                val targetRegister =
                    getInstruction<TwoRegisterInstruction>(targetIndex).registerA

                addInstructions(
                    targetIndex + 1, """
                        invoke-static {v$targetRegister}, $PLAYER_CLASS_DESCRIPTOR->hideInfoCard(Z)Z
                        move-result v$targetRegister
                        """
                )
            }
        }

        // endregion

        // region patch for hide seek message

        seekEduContainerFingerprint.methodOrThrow().apply {
            addInstructionsWithLabels(
                0, """
                    invoke-static {}, $PLAYER_CLASS_DESCRIPTOR->hideSeekMessage()Z
                    move-result v0
                    if-eqz v0, :default
                    return-void
                    """, ExternalLabel("default", getInstruction(0))
            )
        }

        // Removed in YouTube 20.02+
        if (!is_20_02_or_greater) {
            youtubeControlsOverlayFingerprint.methodOrThrow().apply {
                val insertIndex =
                    indexOfFirstLiteralInstructionOrThrow(seekUndoEduOverlayStub)
                val insertRegister = getInstruction<OneRegisterInstruction>(insertIndex).registerA

                val onClickListenerIndex = indexOfFirstInstructionOrThrow(insertIndex) {
                    opcode == Opcode.INVOKE_VIRTUAL &&
                            getReference<MethodReference>()?.name == "setOnClickListener"
                }
                val constComponent = getFirstLiteralComponent(insertIndex, onClickListenerIndex - 1)

                if (constComponent.isNotEmpty()) {
                    addInstruction(
                        onClickListenerIndex + 2,
                        constComponent
                    )
                }
                addInstructionsWithLabels(
                    insertIndex, """
                        invoke-static {}, $PLAYER_CLASS_DESCRIPTOR->hideSeekUndoMessage()Z
                        move-result v$insertRegister
                        if-nez v$insertRegister, :default
                        """, ExternalLabel("default", getInstruction(onClickListenerIndex + 1))
                )

                settingArray += "SETTINGS: HIDE_SEEK_UNDO_MESSAGE"
            }
        }

        // endregion

        // region patch for hide suggested actions

        suggestedActionsFingerprint.matchOrThrow().let {
            it.method.apply {
                val targetIndex = it.patternMatch!!.endIndex
                val targetRegister = getInstruction<OneRegisterInstruction>(targetIndex).registerA

                addInstruction(
                    targetIndex + 1,
                    "invoke-static {v$targetRegister}, $PLAYER_CLASS_DESCRIPTOR->hideSuggestedActions(Landroid/view/View;)V"

                )
            }
        }

        // endregion

        // region patch for skip autoplay countdown

        // This patch works fine when the [EndScreenSuggestedVideoPatch] patch is included.
        touchAreaOnClickListenerFingerprint.mutableClassOrThrow().let {
            it.methods.find { method ->
                method.parameters == listOf("Landroid/view/View${'$'}OnClickListener;")
            }?.apply {
                val setOnClickListenerIndex = indexOfFirstInstructionOrThrow {
                    opcode == Opcode.INVOKE_VIRTUAL &&
                            getReference<MethodReference>()?.name == "setOnClickListener"
                }
                val setOnClickListenerRegister =
                    getInstruction<FiveRegisterInstruction>(setOnClickListenerIndex).registerC

                addInstruction(
                    setOnClickListenerIndex + 1,
                    "invoke-static {v$setOnClickListenerRegister}, $PLAYER_CLASS_DESCRIPTOR->skipAutoPlayCountdown(Landroid/view/View;)V"
                )
            } ?: throw PatchException("Failed to find setOnClickListener method")
        }

        // endregion

        // region patch for hide video zoom overlay

        videoZoomSnapIndicatorFingerprint.methodOrThrow().apply {
            addInstructionsWithLabels(
                0, """
                    invoke-static {}, $PLAYER_CLASS_DESCRIPTOR->hideZoomOverlay()Z
                    move-result v0
                    if-eqz v0, :shown
                    return-void
                    """, ExternalLabel("shown", getInstruction(0))
            )
        }

        // endregion

        addSpanFilter(SANITIZE_VIDEO_SUBTITLE_FILTER_CLASS_DESCRIPTOR)
        addLithoFilter(PLAYER_COMPONENTS_FILTER_CLASS_DESCRIPTOR)

        // region add settings

        addPreference(
            settingArray,
            PLAYER_COMPONENTS
        )

        // endregion

    }
}
