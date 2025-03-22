package app.revanced.patches.youtube.utils.compatibility

import app.revanced.patcher.patch.PackageName
import app.revanced.patcher.patch.VersionName

internal object Constants {
    internal const val YOUTUBE_PACKAGE_NAME = "com.google.android.youtube"

    val COMPATIBLE_PACKAGE: Pair<PackageName, Set<VersionName>?> = Pair(
        YOUTUBE_PACKAGE_NAME,
        setOf(
            "19.05.36", // This is the last version with the least YouTube experimental flag.
            "19.16.39", // This is the last version where the 'Restore old seekbar thumbnails' setting works.
            "19.44.39", // This is the latest version supported by the RVX patch.
        )
    )
}