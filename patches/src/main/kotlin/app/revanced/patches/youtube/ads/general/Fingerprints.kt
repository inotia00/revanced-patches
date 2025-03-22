package app.revanced.patches.youtube.ads.general

import app.revanced.patches.youtube.utils.resourceid.fullScreenEngagementAdContainer
import app.revanced.patches.youtube.utils.resourceid.interstitialsContainer
import app.revanced.patches.youtube.utils.resourceid.slidingDialogAnimation
import app.revanced.util.fingerprint.legacyFingerprint
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstructionReversed
import app.revanced.util.or
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

internal val compactYpcOfferModuleViewFingerprint = legacyFingerprint(
    name = "compactYpcOfferModuleViewFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PROTECTED or AccessFlags.FINAL,
    parameters = listOf("I", "I"),
    opcodes = listOf(
        Opcode.ADD_INT_2ADDR,
        Opcode.ADD_INT_2ADDR,
        Opcode.INVOKE_VIRTUAL,
        Opcode.RETURN_VOID
    ),
    customFingerprint = { method, _ ->
        method.definingClass.endsWith("/CompactYpcOfferModuleView;") &&
                method.name == "onMeasure"
    }
)

internal val fullScreenEngagementAdContainerFingerprint = legacyFingerprint(
    name = "fullScreenEngagementAdContainerFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = emptyList(),
    literals = listOf(fullScreenEngagementAdContainer),
    customFingerprint = { method, _ ->
        indexOfAddListInstruction(method) >= 0
    }
)

internal fun indexOfAddListInstruction(method: Method) =
    method.indexOfFirstInstructionReversed {
        opcode == Opcode.INVOKE_VIRTUAL &&
                getReference<MethodReference>()?.name == "add"
    }

internal val interstitialsContainerFingerprint = legacyFingerprint(
    name = "interstitialsContainerFingerprint",
    returnType = "V",
    strings = listOf("overlay_controller_param"),
    literals = listOf(interstitialsContainer)
)

internal val showDialogCommandFingerprint = legacyFingerprint(
    name = "showDialogCommandFingerprint",
    returnType = "V",
    opcodes = listOf(
        Opcode.IF_EQ,
        Opcode.IGET_OBJECT,
        Opcode.INVOKE_VIRTUAL,
        Opcode.IGET, // get dialog code
    ),
    literals = listOf(slidingDialogAnimation),
    // 18.43 and earlier has a different first parameter.
    // Since this fingerprint is somewhat weak, work around by checking for both method parameter signatures.
    customFingerprint = { method, _ ->
        // 18.43 and earlier parameters are: "L", "L"
        // 18.44+ parameters are "[B", "L"
        val parameterTypes = method.parameterTypes

        parameterTypes.size == 2 && parameterTypes[1].startsWith("L")
    },
)
