package app.revanced.extension.youtube.patches.ads;

import static app.revanced.extension.shared.utils.Utils.hideViewBy0dpUnderCondition;
import static app.revanced.extension.shared.utils.Utils.hideViewUnderCondition;

import android.view.View;
import android.widget.TextView;

import java.util.List;

import app.revanced.extension.shared.utils.Logger;
import app.revanced.extension.youtube.settings.Settings;
import app.revanced.extension.youtube.utils.ExtendedUtils;

@SuppressWarnings("unused")
public class AdsPatch {
    private static final boolean HIDE_END_SCREEN_STORE_BANNER =
            Settings.HIDE_END_SCREEN_STORE_BANNER.get();
    private static final boolean HIDE_GENERAL_ADS =
            Settings.HIDE_GENERAL_ADS.get();
    // https://github.com/ReVanced/revanced-patches/issues/1091
    private static final boolean HIDE_GET_PREMIUM =
            Settings.HIDE_YOUTUBE_PREMIUM_PROMOTION.get();
    private static final boolean HIDE_VIDEO_ADS =
            Settings.HIDE_VIDEO_ADS.get();

    // https://encrypted-tbn0.gstatic.com/shopping?q=tbn
    private static final String STORE_BANNER_DOMAIN =
            "gstatic.com/shopping";

    /**
     * Injection point.
     * Hide the view, which shows ads in the homepage.
     *
     * @param view The view, which shows ads.
     */
    public static void hideAdAttributionView(View view) {
        hideViewBy0dpUnderCondition(HIDE_GENERAL_ADS, view);
    }

    /**
     * Injection point.
     *
     * @param elementsList List of components of the end screen container.
     * @param protobufList Component (ProtobufList).
     */
    public static void hideEndScreenStoreBanner(List<Object> elementsList, Object protobufList) {
        if (HIDE_END_SCREEN_STORE_BANNER &&
                protobufList.toString().contains(STORE_BANNER_DOMAIN)) {
            Logger.printDebug(() -> "Hiding store banner");
            return;
        }

        elementsList.add(protobufList);
    }

    /**
     * Injection point.
     */
    public static boolean hideGetPremium() {
        return HIDE_GET_PREMIUM;
    }

    /**
     * Injection point.
     */
    public static boolean hideShortsAds(boolean original) {
        return HIDE_VIDEO_ADS || original;
    }

    /**
     * Injection point.
     */
    public static boolean hideShortsPaidPromotionLabel() {
        return Settings.HIDE_PAID_PROMOTION_LABEL.get();
    }

    /**
     * Injection point.
     */
    public static void hideShortsPaidPromotionLabel(TextView textView) {
        hideViewUnderCondition(Settings.HIDE_PAID_PROMOTION_LABEL.get(), textView);
    }

    /**
     * Injection point.
     */
    public static boolean hideVideoAds() {
        return HIDE_VIDEO_ADS;
    }

    /**
     * Injection point.
     * <p>
     * Only used by old clients.
     * It is presumed to have been deprecated, and if it is confirmed that it is no longer used, remove it.
     */
    public static boolean hideVideoAds(boolean original) {
        return !HIDE_VIDEO_ADS && original;
    }

    /**
     * Injection point.
     * <p>
     * Toolbar buttons (including the YouTube logo) and navigation bar buttons depend on the
     * '<a href="https://www.youtube.com/youtubei/v1/guide">'/guide' endpoint</a>' requests.
     * <p>
     * Therefore, the patch works if the 'osName' value is spoofed only in '/guide' endpoint requests.
     *
     * @return osName.
     */
    public static String overrideOSName() {
        return ExtendedUtils.getOSName();
    }
}
