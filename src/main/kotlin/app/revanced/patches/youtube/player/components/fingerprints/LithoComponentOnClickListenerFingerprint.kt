package app.revanced.patches.youtube.player.components.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.ComponentLongClickListener
import app.revanced.util.fingerprint.LiteralValueFingerprint
import com.android.tools.smali.dexlib2.AccessFlags

@Suppress("SpellCheckingInspection")
internal object LithoComponentOnClickListenerFingerprint : LiteralValueFingerprint(
    returnType = "V",
    accessFlags = AccessFlags.PRIVATE or AccessFlags.STATIC,
    parameters = listOf("L"),
    literalSupplier = { ComponentLongClickListener },
)