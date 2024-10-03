package app.revanced.patches.music.misc.share.fingerprints

import app.revanced.patcher.extensions.or
import app.revanced.patches.music.utils.resourceid.SharedResourceIdPatch.BottomSheetRecyclerView
import app.revanced.util.fingerprint.LiteralValueFingerprint
import com.android.tools.smali.dexlib2.AccessFlags

internal object BottomSheetRecyclerViewFingerprint : LiteralValueFingerprint(
    returnType = "Lj${'$'}/util/Optional;",
    accessFlags = AccessFlags.PROTECTED or AccessFlags.FINAL,
    parameters = emptyList(),
    literalSupplier = { BottomSheetRecyclerView },
)