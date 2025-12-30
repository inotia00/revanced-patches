@file:Suppress("ktlint:standard:property-naming")

package app.revanced.patches.music.utils.playservice

import app.revanced.patcher.patch.resourcePatch
import app.revanced.util.findElementByAttributeValueOrThrow

var is_6_27_or_greater = false
    private set
var is_6_36_or_greater = false
    private set
var is_6_39_or_greater = false
    private set
var is_6_42_or_greater = false
    private set
var is_6_43_or_greater = false
    private set
var is_6_48_or_greater = false
    private set
var is_7_03_or_greater = false
    private set
var is_7_06_or_greater = false
    private set
var is_7_13_or_greater = false
    private set
var is_7_16_or_greater = false
    private set
var is_7_17_or_greater = false
    private set
var is_7_18_or_greater = false
    private set
var is_7_20_or_greater = false
    private set
var is_7_23_or_greater = false
    private set
var is_7_25_or_greater = false
    private set
var is_7_27_or_greater = false
    private set
var is_7_28_or_greater = false
    private set
var is_7_29_or_greater = false
    private set
var is_7_33_or_greater = false
    private set
var is_8_03_or_greater = false
    private set
var is_8_05_or_greater = false
    private set
var is_8_07_or_greater = false
    private set
var is_8_12_or_greater = false
    private set
var is_8_15_or_greater = false
    private set
var is_8_28_or_greater = false
    private set
var is_8_29_or_greater = false
    private set
var is_8_30_or_greater = false
    private set
var is_8_33_or_greater = false
    private set

val versionCheckPatch = resourcePatch(
    description = "versionCheckPatch",
) {
    execute {
        // The app version is missing from the decompiled manifest,
        // so instead use the Google Play services version and compare against specific releases.
        val playStoreServicesVersion = document("res/values/integers.xml").use { document ->
            document.documentElement.childNodes.findElementByAttributeValueOrThrow(
                "name",
                "google_play_services_version",
            ).textContent.toInt()
        }

        fun isGreaterThan(targetVersion: Int) =
            targetVersion <= playStoreServicesVersion

        // All bug fix releases always seem to use the same play store version as the minor version.
        is_6_27_or_greater = isGreaterThan(234412000)
        is_6_36_or_greater = isGreaterThan(240399000)
        is_6_39_or_greater = isGreaterThan(240699000)
        is_6_42_or_greater = isGreaterThan(240999000)
        is_6_43_or_greater = isGreaterThan(241099000)
        is_6_48_or_greater = isGreaterThan(241599000)
        is_7_03_or_greater = isGreaterThan(242199000)
        is_7_06_or_greater = isGreaterThan(242499000)
        is_7_13_or_greater = isGreaterThan(243199000)
        is_7_16_or_greater = isGreaterThan(243499000)
        is_7_17_or_greater = isGreaterThan(243530000)
        is_7_18_or_greater = isGreaterThan(243699000)
        is_7_20_or_greater = isGreaterThan(243899000)
        is_7_23_or_greater = isGreaterThan(244199000)
        is_7_25_or_greater = isGreaterThan(244399000)
        is_7_27_or_greater = isGreaterThan(244515000)
        is_7_28_or_greater = isGreaterThan(244699000)
        is_7_29_or_greater = isGreaterThan(244799000)
        is_7_33_or_greater = isGreaterThan(245199000)
        is_8_03_or_greater = isGreaterThan(250399000)
        is_8_05_or_greater = isGreaterThan(250599000)
        is_8_07_or_greater = isGreaterThan(250799000)
        is_8_12_or_greater = isGreaterThan(251299000)
        is_8_15_or_greater = isGreaterThan(251530000)
        is_8_28_or_greater = isGreaterThan(252830000)
        is_8_29_or_greater = isGreaterThan(252930000)
        is_8_30_or_greater = isGreaterThan(253020000)
        is_8_33_or_greater = isGreaterThan(253380000)
    }
}
