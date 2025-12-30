package app.revanced.extension.music.patches.components;

import app.revanced.extension.music.settings.Settings;
import app.revanced.extension.shared.patches.components.ByteArrayFilterGroup;
import app.revanced.extension.shared.patches.components.Filter;
import app.revanced.extension.shared.patches.components.StringFilterGroup;

@SuppressWarnings("unused")
public final class AdsFilter extends Filter {
    private final StringFilterGroup compactBanner;
    private final ByteArrayFilterGroup circleIconButton;

    public AdsFilter() {
        final StringFilterGroup alertBannerPromo = new StringFilterGroup(
                Settings.HIDE_PROMOTION_ALERT_BANNER,
                "alert_banner_promo."
        );

        final StringFilterGroup paidPromotionLabel = new StringFilterGroup(
                Settings.HIDE_PAID_PROMOTION_LABEL,
                "music_paid_content_overlay."
        );

        addIdentifierCallbacks(alertBannerPromo, paidPromotionLabel);

        compactBanner = new StringFilterGroup(
                Settings.HIDE_PROMOTION_ALERT_BANNER,
                "music_compact_banner."
        );

        final StringFilterGroup statementBanner = new StringFilterGroup(
                Settings.HIDE_GENERAL_ADS,
                "statement_banner"
        );

        circleIconButton = new ByteArrayFilterGroup(
                Settings.HIDE_PROMOTION_ALERT_BANNER,
                "music_circle_icon_button."
        );

        addPathCallbacks(compactBanner, statementBanner);
    }

    @Override
    public boolean isFiltered(String path, String identifier, String allValue, byte[] buffer,
                              StringFilterGroup matchedGroup, FilterContentType contentType, int contentIndex) {
        if (matchedGroup == compactBanner) {
            return contentIndex == 0 && circleIconButton.check(buffer).isFiltered();
        }

        return true;
    }
}
