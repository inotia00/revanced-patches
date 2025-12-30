package app.revanced.patches.music.player.components

import app.revanced.patches.music.utils.extension.Constants.PLAYER_CLASS_DESCRIPTOR
import app.revanced.patches.music.utils.playservice.is_7_18_or_greater
import app.revanced.patches.music.utils.resourceid.colorGrey
import app.revanced.patches.music.utils.resourceid.darkBackground
import app.revanced.patches.music.utils.resourceid.inlineTimeBarProgressColor
import app.revanced.patches.music.utils.resourceid.miniPlayerDefaultText
import app.revanced.patches.music.utils.resourceid.miniPlayerMdxPlaying
import app.revanced.patches.music.utils.resourceid.miniPlayerPlayPauseReplayButton
import app.revanced.patches.music.utils.resourceid.miniPlayerViewPager
import app.revanced.patches.music.utils.resourceid.playerViewPager
import app.revanced.patches.music.utils.resourceid.remixGenericButtonSize
import app.revanced.patches.music.utils.resourceid.tapBloomView
import app.revanced.util.containsLiteralInstruction
import app.revanced.util.fingerprint.legacyFingerprint
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstruction
import app.revanced.util.or
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.TypeReference

const val AUDIO_VIDEO_SWITCH_TOGGLE_VISIBILITY =
    "/AudioVideoSwitcherToggleView;->setVisibility(I)V"

internal const val AUDIO_VIDEO_SWITCH_TOGGLE_FEATURE_FLAG = 45671274L

internal val audioVideoSwitchToggleFeatureFlagsFingerprint = legacyFingerprint(
    name = "audioVideoSwitchToggleFeatureFlagsFingerprint",
    returnType = "Z",
    parameters = emptyList(),
    literals = listOf(AUDIO_VIDEO_SWITCH_TOGGLE_FEATURE_FLAG),
)

internal val audioVideoSwitchToggleFingerprint = legacyFingerprint(
    name = "audioVideoSwitchToggleFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = emptyList(),
    customFingerprint = { method, _ ->
        method.indexOfFirstInstruction {
            opcode == Opcode.INVOKE_VIRTUAL &&
                    getReference<MethodReference>()
                        ?.toString()
                        ?.endsWith(AUDIO_VIDEO_SWITCH_TOGGLE_VISIBILITY) == true
        } >= 0
    }
)

internal val engagementPanelHeightFingerprint = legacyFingerprint(
    name = "engagementPanelHeightFingerprint",
    returnType = "L",
    // In YouTube Music 7.21.50+, there are two methods with similar structure, so this Opcode pattern must be used.
    opcodes = listOf(
        Opcode.IGET_OBJECT,
        Opcode.IGET_OBJECT,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT,
    ),
    parameters = emptyList(),
    customFingerprint = { method, _ ->
        AccessFlags.FINAL.isSet(method.accessFlags) &&
                method.containsLiteralInstruction(1) &&
                method.indexOfFirstInstruction {
                    opcode == Opcode.INVOKE_VIRTUAL &&
                            getReference<MethodReference>()?.name == "booleanValue"
                } >= 0
    }
)

internal val engagementPanelHeightParentFingerprint = legacyFingerprint(
    name = "engagementPanelHeightParentFingerprint",
    returnType = "L",
    opcodes = listOf(Opcode.NEW_ARRAY),
    parameters = emptyList(),
    customFingerprint = custom@{ method, _ ->
        if (method.definingClass.startsWith("Lcom/")) {
            return@custom false
        }
        if (method.returnType == "Ljava/lang/Object;") {
            return@custom false
        }
        if (!AccessFlags.FINAL.isSet(method.accessFlags)) {
            return@custom false
        }
        method.indexOfFirstInstruction {
            opcode == Opcode.CHECK_CAST &&
                    getReference<TypeReference>()?.type == "Lcom/google/android/libraries/youtube/engagementpanel/size/EngagementPanelSizeBehavior;"
        } >= 0
    }
)

internal val handleSearchRenderedFingerprint = legacyFingerprint(
    name = "handleSearchRenderedFingerprint",
    returnType = "V",
    parameters = listOf("L"),
    customFingerprint = { method, _ -> method.name == "handleSearchRendered" }
)

internal val handleSignInEventFingerprint = legacyFingerprint(
    name = "handleSignInEventFingerprint",
    returnType = "V",
    parameters = listOf("L"),
    opcodes = listOf(
        Opcode.INVOKE_VIRTUAL,
        Opcode.RETURN_VOID
    ),
    customFingerprint = { method, _ -> method.name == "handleSignInEvent" }
)

internal val interactionLoggingEnumFingerprint = legacyFingerprint(
    name = "interactionLoggingEnumFingerprint",
    returnType = "V",
    strings = listOf("INTERACTION_LOGGING_GESTURE_TYPE_SWIPE")
)

internal val minimizedPlayerFingerprint = legacyFingerprint(
    name = "minimizedPlayerFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("L", "L"),
    opcodes = listOf(
        Opcode.MOVE_RESULT,
        Opcode.IF_NEZ,
        Opcode.IF_EQZ
    ),
    strings = listOf("w_st")
)

internal val miniPlayerConstructorFingerprint = legacyFingerprint(
    name = "miniPlayerConstructorFingerprint",
    returnType = "V",
    strings = listOf("sharedToggleMenuItemMutations"),
    literals = listOf(colorGrey, miniPlayerPlayPauseReplayButton)
)

internal val miniPlayerDefaultTextFingerprint = legacyFingerprint(
    name = "miniPlayerDefaultTextFingerprint",
    returnType = "V",
    parameters = listOf("Ljava/lang/Object;"),
    opcodes = listOf(
        Opcode.SGET_OBJECT,
        Opcode.IF_NE
    ),
    literals = listOf(miniPlayerDefaultText)
)

internal val miniPlayerDefaultViewVisibilityFingerprint = legacyFingerprint(
    name = "miniPlayerDefaultViewVisibilityFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("Landroid/view/View;", "F"),
    opcodes = listOf(
        Opcode.IGET_OBJECT,
        Opcode.SUB_FLOAT_2ADDR,
        Opcode.SGET_OBJECT,
        Opcode.INVOKE_VIRTUAL
    ),
    customFingerprint = { method, classDef ->
        method.name == "a" &&
                classDef.methods.count() == 3
    }
)

internal val miniPlayerParentFingerprint = legacyFingerprint(
    name = "miniPlayerParentFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = emptyList(),
    literals = listOf(miniPlayerMdxPlaying)
)

internal val mppWatchWhileLayoutFingerprint = legacyFingerprint(
    name = "mppWatchWhileLayoutFingerprint",
    returnType = "V",
    opcodes = listOf(Opcode.NEW_ARRAY),
    literals = listOf(miniPlayerPlayPauseReplayButton),
    customFingerprint = custom@{ method, _ ->
        if (!method.definingClass.endsWith("/MppWatchWhileLayout;")) {
            return@custom false
        }
        if (method.name != "onFinishInflate") {
            return@custom false
        }
        if (!is_7_18_or_greater) {
            return@custom true
        }

        indexOfCallableInstruction(method) >= 0
    }
)

internal fun indexOfCallableInstruction(method: Method) =
    method.indexOfFirstInstruction {
        val reference = getReference<MethodReference>()
        opcode == Opcode.INVOKE_VIRTUAL &&
                reference?.returnType == "V" &&
                reference.parameterTypes.size == 1 &&
                reference.parameterTypes.firstOrNull() == "Ljava/util/concurrent/Callable;"
    }

internal val musicActivityWidgetFingerprint = legacyFingerprint(
    name = "musicActivityWidgetFingerprint",
    literals = listOf(79500L),
    customFingerprint = { method, _ ->
        method.definingClass.endsWith("/MusicActivity;")
    }
)

internal val musicPlaybackControlsFingerprint = legacyFingerprint(
    name = "musicPlaybackControlsFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("Z"),
    opcodes = listOf(
        Opcode.IPUT_BOOLEAN,
        Opcode.INVOKE_VIRTUAL,
        Opcode.RETURN_VOID
    ),
    customFingerprint = { method, _ ->
        method.definingClass.endsWith("/MusicPlaybackControls;")
    }
)

internal val nextButtonVisibilityFingerprint = legacyFingerprint(
    name = "nextButtonVisibilityFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = emptyList(),
    opcodes = listOf(
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT,
        Opcode.CONST_16,
        Opcode.IF_EQZ
    )
)

internal const val OLD_ENGAGEMENT_PANEL_FEATURE_FLAG = 45427672L

internal val oldEngagementPanelFingerprint = legacyFingerprint(
    name = "oldEngagementPanelFingerprint",
    returnType = "Z",
    parameters = emptyList(),
    literals = listOf(OLD_ENGAGEMENT_PANEL_FEATURE_FLAG),
)

internal const val OLD_PLAYER_BACKGROUND_FEATURE_FLAG = 45415319L

/**
 * Deprecated in YouTube Music v6.34.51+
 */
internal val oldPlayerBackgroundFingerprint = legacyFingerprint(
    name = "oldPlayerBackgroundFingerprint",
    returnType = "Z",
    parameters = emptyList(),
    literals = listOf(OLD_PLAYER_BACKGROUND_FEATURE_FLAG),
)

internal const val OLD_PLAYER_LAYOUT_FEATURE_FLAG = 45399578L

/**
 * Deprecated in YouTube Music v6.31.55+
 */
internal val oldPlayerLayoutFingerprint = legacyFingerprint(
    name = "oldPlayerLayoutFingerprint",
    returnType = "Z",
    parameters = emptyList(),
    literals = listOf(OLD_PLAYER_LAYOUT_FEATURE_FLAG),
)

internal val playerPatchConstructorFingerprint = legacyFingerprint(
    name = "playerPatchConstructorFingerprint",
    returnType = "V",
    customFingerprint = { method, _ ->
        method.definingClass == PLAYER_CLASS_DESCRIPTOR &&
                method.name == "<init>"
    }
)

internal val playerViewPagerConstructorFingerprint = legacyFingerprint(
    name = "playerViewPagerConstructorFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.CONSTRUCTOR,
    literals = listOf(miniPlayerViewPager, playerViewPager),
)

internal val quickSeekOverlayFingerprint = legacyFingerprint(
    name = "quickSeekOverlayFingerprint",
    returnType = "V",
    parameters = emptyList(),
    literals = listOf(darkBackground, tapBloomView),
)

internal val remixGenericButtonFingerprint = legacyFingerprint(
    name = "remixGenericButtonFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.CONSTRUCTOR,
    opcodes = listOf(
        Opcode.CONST,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT,
        Opcode.FLOAT_TO_INT
    ),
    literals = listOf(remixGenericButtonSize),
)

internal val repeatTrackFingerprint = legacyFingerprint(
    name = "repeatTrackFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("L", "L"),
    opcodes = listOf(
        Opcode.IGET_OBJECT,
        Opcode.IGET_OBJECT,
        Opcode.SGET_OBJECT,
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT,
        Opcode.IF_NEZ
    ),
    strings = listOf("w_st")
)

internal const val SHUFFLE_BUTTON_ID = 45468L

internal val shuffleOnClickFingerprint = legacyFingerprint(
    name = "shuffleOnClickFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("Landroid/view/View;"),
    literals = listOf(SHUFFLE_BUTTON_ID),
    customFingerprint = { method, _ ->
        method.name == "onClick"
    }
)

internal val shuffleEnumFingerprint = legacyFingerprint(
    name = "shuffleEnumFingerprint",
    returnType = "V",
    parameters = emptyList(),
    strings = listOf(
        "SHUFFLE_OFF",
        "SHUFFLE_ALL",
        "SHUFFLE_DISABLED",
    ),
    customFingerprint = { method, _ ->
        method.name == "<clinit>"
    }
)

internal const val SMOOTH_TRANSITION_ANIMATION_FEATURE_FLAG = 45679250L

internal val smoothTransitionAnimationFingerprint = legacyFingerprint(
    name = "smoothTransitionAnimationFingerprint",
    returnType = "Z",
    parameters = emptyList(),
    literals = listOf(SMOOTH_TRANSITION_ANIMATION_FEATURE_FLAG),
)

internal val smoothTransitionAnimationInvertedParentFingerprint = legacyFingerprint(
    name = "smoothTransitionAnimationInvertedParentFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PRIVATE or AccessFlags.FINAL,
    parameters = listOf("F", "F", "F", "F", "F"),
    opcodes = listOf(
        Opcode.INVOKE_STATIC,
        Opcode.RETURN_VOID,
    )
)

internal val swipeToCloseFingerprint = legacyFingerprint(
    name = "swipeToCloseFingerprint",
    returnType = "Z",
    parameters = emptyList(),
    literals = listOf(45398432L),
)

internal val switchToggleColorFingerprint = legacyFingerprint(
    name = "switchToggleColorFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PRIVATE or AccessFlags.FINAL,
    parameters = listOf("L", "J"),
    opcodes = listOf(
        Opcode.INVOKE_VIRTUAL,
        Opcode.MOVE_RESULT_OBJECT,
        Opcode.CHECK_CAST,
        Opcode.IGET
    )
)

internal val thickSeekBarColorFingerprint = legacyFingerprint(
    name = "thickSeekBarColorFingerprint",
    returnType = "V",
    parameters = listOf("L"),
    literals = listOf(inlineTimeBarProgressColor),
    customFingerprint = { method, _ ->
        method.definingClass.endsWith("/MusicPlaybackControls;")
    }
)

internal val thickSeekBarFeatureFlagFingerprint = legacyFingerprint(
    name = "thickSeekBarFeatureFlagFingerprint",
    returnType = "Z",
    parameters = emptyList(),
    literals = listOf(45659062L),
)

internal val thickSeekBarInflateFingerprint = legacyFingerprint(
    name = "thickSeekBarInflateFingerprint",
    returnType = "V",
    parameters = emptyList(),
    customFingerprint = { method, _ ->
        method.definingClass.endsWith("/MusicPlaybackControls;") &&
                method.name == "onFinishInflate"
    }
)

internal val zenModeFingerprint = legacyFingerprint(
    name = "zenModeFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("L", "J"),
    opcodes = listOf(
        Opcode.MOVE_RESULT,
        Opcode.INVOKE_STATIC,
        Opcode.MOVE_RESULT,
        Opcode.GOTO,
        Opcode.NOP,
        Opcode.SGET_OBJECT
    )
)