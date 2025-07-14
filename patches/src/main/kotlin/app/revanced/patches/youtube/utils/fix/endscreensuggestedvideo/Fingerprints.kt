package app.revanced.patches.youtube.utils.fix.endscreensuggestedvideo

import app.revanced.util.fingerprint.legacyFingerprint
import app.revanced.util.getReference
import app.revanced.util.indexOfFirstInstruction
import app.revanced.util.or
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

internal val autoNavConstructorFingerprint = legacyFingerprint(
    name = "autoNavConstructorFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.CONSTRUCTOR,
    strings = listOf("main_app_autonav"),
)

internal val autoNavStatusFingerprint = legacyFingerprint(
    name = "autoNavStatusFingerprint",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    returnType = "Z",
    parameters = emptyList()
)

/**
 * This fingerprint is also compatible with very old YouTube versions.
 * Tested on YouTube v16.40.36, v18.29.38, v19.16.39.
 */
internal val removeOnLayoutChangeListenerFingerprint = legacyFingerprint(
    name = "removeOnLayoutChangeListenerFingerprint",
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = emptyList(),
    opcodes = listOf(
        Opcode.IPUT,
        Opcode.INVOKE_VIRTUAL
    ),
    // This is the only reference present in the entire smali.
    customFingerprint = { method, _ ->
        method.indexOfFirstInstruction {
            getReference<MethodReference>()?.toString()
                ?.endsWith("YouTubePlayerOverlaysLayout;->removeOnLayoutChangeListener(Landroid/view/View${'$'}OnLayoutChangeListener;)V") == true
        } >= 0
    }
)