package app.revanced.patches.youtube.utils.compatibility

import app.revanced.patcher.patch.PackageName
import app.revanced.patcher.patch.VersionName

internal object Constants {
    internal const val YOUTUBE_PACKAGE_NAME = "com.google.android.youtube"

    val COMPATIBLE_PACKAGE: Pair<PackageName, Set<VersionName>?> = Pair(
        YOUTUBE_PACKAGE_NAME,
        setOf(
            "18.29.38", // This is the last version where the 'Zoomed to fill' setting works.
            "18.33.40", // This is the last version that do not use litho components in Shorts.
            "18.38.44", // This is the last version with no delay in applying video quality on the server side.
            "18.48.39", // This is the last version that do not use Rolling Number.
            "19.05.36", // This is the last version with the least YouTube experimental flag.
            "19.16.39", // This is the last version where the 'Restore old seekbar thumbnails' setting works.
            "19.44.39", // This is the latest version supported by the RVX patch.
        )
    )
}