package app.revanced.patches.shared.ads

import app.revanced.patches.shared.extension.Constants.PATCHES_PATH
import app.revanced.util.fingerprint.legacyFingerprint
import app.revanced.util.or
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal const val EXTENSION_CLASS_DESCRIPTOR =
    "$PATCHES_PATH/FullscreenAdsPatch;"

internal val interstitialsContainerFingerprint = legacyFingerprint(
    name = "interstitialsContainerFingerprint",
    returnType = "V",
    strings = listOf("overlay_controller_param"),
    literals = listOf(interstitialsContainer)
)

/**
 * Simply injecting into [videoAdsFingerprint] can block video ads.
 *
 * Nevertheless, if this Method is not injected,
 * Video ad stream is downloaded, consuming unnecessary network resources.
 */
internal val playerBytesAdLayoutFingerprint = legacyFingerprint(
    name = "playerBytesAdLayoutFingerprint",
    returnType = "V",
    parameters = listOf("L"),
    strings = listOf(
        "Bootstrapped layout construction resulted in non PlayerBytesLayout. PlayerAds count: "
    )
)

internal val showDialogCommandFingerprint = legacyFingerprint(
    name = "showDialogCommandFingerprint",
    returnType = "V",
    opcodes = listOf(
        Opcode.INVOKE_VIRTUAL,
        Opcode.IGET, // get dialog code
    ),
    literals = listOf(slidingDialogAnimation),
    // YouTube 18.43 / YouTube Music 6.26 and earlier has a different first parameter.
    // Since this fingerprint is somewhat weak, work around by checking for both method parameter signatures.
    customFingerprint = { method, _ ->
        // YouTube 18.43 / YouTube Music 6.26 and earlier parameters are: "L", "L"
        // YouTube 18.44+ / YouTube Music 6.27+ parameters are "[B", "L"
        val parameterTypes = method.parameterTypes

        parameterTypes.size == 2 && parameterTypes[1].startsWith("L")
    },
)

internal val videoAdsFingerprint = legacyFingerprint(
    name = "videoAdsFingerprint",
    returnType = "V",
    strings = listOf("markFillRequested", "requestEnterSlot")
)

/**
 * This method seems to be deprecated, but I'm not sure.
 */
internal val videoAdsLegacyFingerprint = legacyFingerprint(
    name = "videoAdsLegacyFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = emptyList(),
    opcodes = listOf(
        Opcode.CONST_WIDE_16,
        Opcode.IPUT_WIDE,
        Opcode.CONST_WIDE_16,
        Opcode.IPUT_WIDE,
        Opcode.IPUT_WIDE,
        Opcode.IPUT_WIDE,
        Opcode.IPUT_WIDE,
        Opcode.CONST_4,
    ),
    literals = listOf(4L)
)
