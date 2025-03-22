package app.revanced.patches.youtube.utils.returnyoutubedislike

import app.revanced.patcher.extensions.InstructionExtensions.addInstructions
import app.revanced.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.revanced.patcher.extensions.InstructionExtensions.getInstruction
import app.revanced.patcher.extensions.InstructionExtensions.removeInstruction
import app.revanced.patcher.patch.bytecodePatch
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.shared.dislikeFingerprint
import app.revanced.patches.shared.likeFingerprint
import app.revanced.patches.shared.litho.addLithoFilter
import app.revanced.patches.shared.litho.lithoFilterPatch
import app.revanced.patches.shared.removeLikeFingerprint
import app.revanced.patches.shared.textcomponent.hookSpannableString
import app.revanced.patches.shared.textcomponent.hookTextComponent
import app.revanced.patches.shared.textcomponent.textComponentPatch
import app.revanced.patches.youtube.utils.compatibility.Constants.COMPATIBLE_PACKAGE
import app.revanced.patches.youtube.utils.extension.Constants.COMPONENTS_PATH
import app.revanced.patches.youtube.utils.extension.Constants.UTILS_PATH
import app.revanced.patches.youtube.utils.patch.PatchList.RETURN_YOUTUBE_DISLIKE
import app.revanced.patches.youtube.utils.playservice.is_18_34_or_greater
import app.revanced.patches.youtube.utils.playservice.is_18_49_or_greater
import app.revanced.patches.youtube.utils.playservice.versionCheckPatch
import app.revanced.patches.youtube.utils.rollingNumberTextViewAnimationUpdateFingerprint
import app.revanced.patches.youtube.utils.rollingNumberTextViewFingerprint
import app.revanced.patches.youtube.utils.settings.ResourceUtils.addPreference
import app.revanced.patches.youtube.utils.settings.settingsPatch
import app.revanced.patches.youtube.video.information.hookShortsVideoInformation
import app.revanced.patches.youtube.video.information.videoInformationPatch
import app.revanced.patches.youtube.video.videoid.hookPlayerResponseVideoId
import app.revanced.patches.youtube.video.videoid.hookVideoId
import app.revanced.util.findMethodOrThrow
import app.revanced.util.fingerprint.matchOrThrow
import app.revanced.util.fingerprint.methodOrThrow
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstructionOrThrow
import app.revanced.util.indexOfFirstInstructionReversedOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val EXTENSION_RYD_CLASS_DESCRIPTOR =
    "$UTILS_PATH/ReturnYouTubeDislikePatch;"

private val returnYouTubeDislikeRollingNumberPatch = bytecodePatch(
    description = "returnYouTubeDislikeRollingNumberPatch"
) {
    dependsOn(versionCheckPatch)

    execute {
        if (!is_18_49_or_greater) {
            return@execute
        }

        rollingNumberSetterFingerprint.matchOrThrow().let {
            it.method.apply {
                val rollingNumberClassIndex = it.patternMatch!!.startIndex
                val rollingNumberClassReference =
                    getInstruction<ReferenceInstruction>(rollingNumberClassIndex).reference.toString()
                val rollingNumberConstructorMethod =
                    findMethodOrThrow(rollingNumberClassReference)
                val charSequenceFieldReference = with(rollingNumberConstructorMethod) {
                    getInstruction<ReferenceInstruction>(
                        indexOfFirstInstructionOrThrow(Opcode.IPUT_OBJECT)
                    ).reference
                }

                val insertIndex = rollingNumberClassIndex + 1
                val charSequenceInstanceRegister =
                    getInstruction<OneRegisterInstruction>(rollingNumberClassIndex).registerA
                val registerCount = implementation!!.registerCount

                // This register is being overwritten, so it is free to use.
                val freeRegister = registerCount - 1
                val conversionContextRegister = registerCount - parameters.size + 1

                addInstructions(
                    insertIndex, """
                        iget-object v$freeRegister, v$charSequenceInstanceRegister, $charSequenceFieldReference
                        invoke-static {v$conversionContextRegister, v$freeRegister}, $EXTENSION_RYD_CLASS_DESCRIPTOR->onRollingNumberLoaded(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/String;
                        move-result-object v$freeRegister
                        iput-object v$freeRegister, v$charSequenceInstanceRegister, $charSequenceFieldReference
                        """
                )
            }
        }

        // Rolling Number text views use the measured width of the raw string for layout.
        // Modify the measure text calculation to include the left drawable separator if needed.
        rollingNumberMeasureAnimatedTextFingerprint.matchOrThrow().let {
            it.method.apply {
                val endIndex = it.patternMatch!!.endIndex
                val measuredTextWidthIndex = endIndex - 2
                val measuredTextWidthRegister =
                    getInstruction<TwoRegisterInstruction>(measuredTextWidthIndex).registerA

                addInstructions(
                    endIndex + 1, """
                        invoke-static {p1, v$measuredTextWidthRegister}, $EXTENSION_RYD_CLASS_DESCRIPTOR->onRollingNumberMeasured(Ljava/lang/String;F)F
                        move-result v$measuredTextWidthRegister
                        """
                )

                val ifGeIndex = indexOfFirstInstructionOrThrow(Opcode.IF_GE)
                val ifGeInstruction = getInstruction<TwoRegisterInstruction>(ifGeIndex)

                removeInstruction(ifGeIndex)
                addInstructionsWithLabels(
                    ifGeIndex, """
                        if-ge v${ifGeInstruction.registerA}, v${ifGeInstruction.registerB}, :jump
                        """, ExternalLabel("jump", getInstruction(endIndex))
                )
            }
        }

        rollingNumberMeasureStaticLabelFingerprint.matchOrThrow(
            rollingNumberMeasureTextParentFingerprint
        ).let {
            it.method.apply {
                val measureTextIndex = it.patternMatch!!.startIndex + 1
                val freeRegister = getInstruction<TwoRegisterInstruction>(0).registerA

                addInstructions(
                    measureTextIndex + 1, """
                        move-result v$freeRegister
                        invoke-static {p1, v$freeRegister}, $EXTENSION_RYD_CLASS_DESCRIPTOR->onRollingNumberMeasured(Ljava/lang/String;F)F
                        """
                )
            }
        }

        // The rolling number Span is missing styling since it's initially set as a String.
        // Modify the UI text view and use the styled like/dislike Span.
        arrayOf(
            // Initial TextView is set in this method.
            rollingNumberTextViewFingerprint
                .methodOrThrow(),

            // Video less than 24 hours after uploaded, like counts will be updated in real time.
            // Whenever like counts are updated, TextView is set in this method.
            rollingNumberTextViewAnimationUpdateFingerprint
                .methodOrThrow(rollingNumberTextViewFingerprint)
        ).forEach { method ->
            method.apply {
                val setTextIndex = indexOfFirstInstructionOrThrow {
                    getReference<MethodReference>()?.name == "setText"
                }
                val textViewRegister =
                    getInstruction<FiveRegisterInstruction>(setTextIndex).registerC
                val textSpanRegister =
                    getInstruction<FiveRegisterInstruction>(setTextIndex).registerD

                addInstructions(
                    setTextIndex, """
                        invoke-static {v$textViewRegister, v$textSpanRegister}, $EXTENSION_RYD_CLASS_DESCRIPTOR->updateRollingNumber(Landroid/widget/TextView;Ljava/lang/CharSequence;)Ljava/lang/CharSequence;
                        move-result-object v$textSpanRegister
                        """
                )
            }
        }
    }
}

private val returnYouTubeDislikeShortsPatch = bytecodePatch(
    description = "returnYouTubeDislikeShortsPatch"
) {
    dependsOn(
        textComponentPatch,
        versionCheckPatch
    )

    execute {
        shortsTextViewFingerprint.matchOrThrow().let {
            it.method.apply {
                val startIndex = it.patternMatch!!.startIndex

                val isDisLikesBooleanIndex =
                    indexOfFirstInstructionReversedOrThrow(startIndex, Opcode.IGET_BOOLEAN)
                val textViewFieldIndex =
                    indexOfFirstInstructionReversedOrThrow(startIndex, Opcode.IGET_OBJECT)

                // If the field is true, the TextView is for a dislike button.
                val isDisLikesBooleanReference =
                    getInstruction<ReferenceInstruction>(isDisLikesBooleanIndex).reference

                val textViewFieldReference = // Like/Dislike button TextView field
                    getInstruction<ReferenceInstruction>(textViewFieldIndex).reference

                // Check if the hooked TextView object is that of the dislike button.
                // If RYD is disabled, or the TextView object is not that of the dislike button, the execution flow is not interrupted.
                // Otherwise, the TextView object is modified, and the execution flow is interrupted to prevent it from being changed afterward.
                val insertIndex = indexOfFirstInstructionOrThrow(Opcode.CHECK_CAST) + 1

                addInstructionsWithLabels(
                    insertIndex, """
                    # Check, if the TextView is for a dislike button
                    iget-boolean v0, p0, $isDisLikesBooleanReference
                    if-eqz v0, :ryd_disabled
                    
                    # Hook the TextView, if it is for the dislike button
                    iget-object v0, p0, $textViewFieldReference
                    invoke-static {v0}, $EXTENSION_RYD_CLASS_DESCRIPTOR->setShortsDislikes(Landroid/view/View;)Z
                    move-result v0
                    if-eqz v0, :ryd_disabled
                    return-void
                    """, ExternalLabel("ryd_disabled", getInstruction(insertIndex))
                )
            }
        }

        if (is_18_34_or_greater) {
            hookSpannableString(
                EXTENSION_RYD_CLASS_DESCRIPTOR,
                "onCharSequenceLoaded"
            )
        }
    }
}

private const val FILTER_CLASS_DESCRIPTOR =
    "$COMPONENTS_PATH/ReturnYouTubeDislikeFilterPatch;"

@Suppress("unused")
val returnYouTubeDislikePatch = bytecodePatch(
    RETURN_YOUTUBE_DISLIKE.title,
    RETURN_YOUTUBE_DISLIKE.summary,
) {
    compatibleWith(COMPATIBLE_PACKAGE)

    dependsOn(
        settingsPatch,
        returnYouTubeDislikeRollingNumberPatch,
        returnYouTubeDislikeShortsPatch,
        lithoFilterPatch,
        videoInformationPatch,
    )

    execute {
        mapOf(
            likeFingerprint to Vote.LIKE,
            dislikeFingerprint to Vote.DISLIKE,
            removeLikeFingerprint to Vote.REMOVE_LIKE,
        ).forEach { (fingerprint, vote) ->
            fingerprint.methodOrThrow().addInstructions(
                0,
                """
                    const/4 v0, ${vote.value}
                    invoke-static {v0}, $EXTENSION_RYD_CLASS_DESCRIPTOR->sendVote(I)V
                    """,
            )
        }

        hookTextComponent(EXTENSION_RYD_CLASS_DESCRIPTOR)

        // region Inject newVideoLoaded event handler to update dislikes when a new video is loaded.
        hookVideoId("$EXTENSION_RYD_CLASS_DESCRIPTOR->newVideoLoaded(Ljava/lang/String;)V")

        // Hook the player response video id, to start loading RYD sooner in the background.
        hookPlayerResponseVideoId("$EXTENSION_RYD_CLASS_DESCRIPTOR->preloadVideoId(Ljava/lang/String;Z)V")

        // endregion

        // Player response video id is needed to search for the video ids in Shorts litho components.
        if (is_18_34_or_greater) {
            addLithoFilter(FILTER_CLASS_DESCRIPTOR)
            hookPlayerResponseVideoId("$FILTER_CLASS_DESCRIPTOR->newPlayerResponseVideoId(Ljava/lang/String;Z)V")
            hookShortsVideoInformation("$FILTER_CLASS_DESCRIPTOR->newShortsVideoStarted(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;JZ)V")
        }

        // endregion

        // region add settings

        addPreference(
            arrayOf(
                "PREFERENCE_SCREEN: RETURN_YOUTUBE_DISLIKE"
            ),
            RETURN_YOUTUBE_DISLIKE
        )

        // endregion
    }
}

enum class Vote(val value: Int) {
    LIKE(1),
    DISLIKE(-1),
    REMOVE_LIKE(0),
}