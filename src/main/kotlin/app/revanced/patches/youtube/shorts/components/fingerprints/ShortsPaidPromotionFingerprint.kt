package app.revanced.patches.youtube.shorts.components.fingerprints

import app.revanced.patches.youtube.utils.resourceid.SharedResourceIdPatch.BadgeLabel
import app.revanced.util.fingerprint.LiteralValueFingerprint

/**
 * The method by which patches are applied is different between the minimum supported version and the maximum supported version.
 * There are two classes where R.id.badge_label[BadgeLabel] is used,
 * but due to the structure of ReVanced Patcher, the patch is applied to the method found first.
 */
internal object ShortsPaidPromotionFingerprint : LiteralValueFingerprint(
    literalSupplier = { BadgeLabel },
)