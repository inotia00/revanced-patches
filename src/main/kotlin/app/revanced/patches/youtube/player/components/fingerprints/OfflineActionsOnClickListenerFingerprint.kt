package app.revanced.patches.youtube.player.components.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.OfflineActionsVideoDeletedUndoSnackbarText
import app.revanced.util.fingerprint.LiteralValueFingerprint
import com.android.tools.smali.dexlib2.AccessFlags

internal object OfflineActionsOnClickListenerFingerprint : LiteralValueFingerprint(
    returnType = "V",
    accessFlags = AccessFlags.PUBLIC or AccessFlags.FINAL,
    parameters = listOf("Ljava/lang/String;"),
    literalSupplier = { OfflineActionsVideoDeletedUndoSnackbarText },
)