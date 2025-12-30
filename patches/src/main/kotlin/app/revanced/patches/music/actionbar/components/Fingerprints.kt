package app.revanced.patches.music.actionbar.components

import app.revanced.patches.music.utils.resourceid.elementsLottieAnimationViewTagId
import app.revanced.patches.music.utils.resourceid.likeDislikeContainer
import app.revanced.util.fingerprint.legacyFingerprint
import app.revanced.util.or
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal val actionBarComponentFingerprint = legacyFingerprint(
    name = "actionBarComponentFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("L", "L"),
    opcodes = listOf(
        Opcode.AND_INT_LIT16,
        Opcode.IF_EQZ,
        Opcode.IGET_OBJECT,
        Opcode.IF_NEZ,
        Opcode.SGET_OBJECT,
        Opcode.SGET_OBJECT
    ),
    literals = listOf(99180L),
)

internal val likeDislikeContainerFingerprint = legacyFingerprint(
    name = "likeDislikeContainerFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.CONSTRUCTOR,
    literals = listOf(likeDislikeContainer)
)

internal val lottieAnimationViewTagFingerprint = legacyFingerprint(
    name = "lottieAnimationViewTagFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PROTECTED or AccessFlags.FINAL,
    opcodes = listOf(
        Opcode.INVOKE_INTERFACE,
        Opcode.MOVE_RESULT_OBJECT,
    ),
    literals = listOf(elementsLottieAnimationViewTagId)
)

internal val commandResolverFingerprint = legacyFingerprint(
    name = "commandResolverFingerprint",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.STATIC or AccessFlags.FINAL,
    returnType = "Z",
    parameters = listOf("L", "L", "Ljava/util/Map;"),
    strings = listOf("CommandResolver threw exception during resolution")
)

internal val offlineVideoEndpointFingerprint = legacyFingerprint(
    name = "offlineVideoEndpointFingerprint",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    returnType = "V",
    parameters = listOf("L", "Ljava/util/Map;"),
    strings = listOf("Object is not an offlineable video: %s")
)
