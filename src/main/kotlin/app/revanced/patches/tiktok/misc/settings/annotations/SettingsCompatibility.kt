package app.revanced.patches.tiktok.misc.settings.annotations

import app.revanced.patcher.annotation.Compatibility
import app.revanced.patcher.annotation.Package

@Compatibility(
    [
        Package("com.ss.android.ugc.trill", arrayOf("27.8.3", "29.3.4")),
        Package("com.zhiliaoapp.musically", arrayOf("27.8.3", "29.3.4"))
    ]
)
@Target(AnnotationTarget.CLASS)
internal annotation class SettingsCompatibility