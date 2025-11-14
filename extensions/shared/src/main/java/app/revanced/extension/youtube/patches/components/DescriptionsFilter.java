package app.revanced.extension.youtube.patches.components;

import app.revanced.extension.shared.patches.components.ByteArrayFilterGroup;
import app.revanced.extension.shared.patches.components.ByteArrayFilterGroupList;
import app.revanced.extension.shared.patches.components.Filter;
import app.revanced.extension.shared.patches.components.StringFilterGroup;
import app.revanced.extension.youtube.settings.Settings;
import app.revanced.extension.youtube.shared.EngagementPanel;
import app.revanced.extension.youtube.shared.RootView;

@SuppressWarnings("unused")
public final class DescriptionsFilter extends Filter {
    private final ByteArrayFilterGroupList macroMarkerShelfGroupList = new ByteArrayFilterGroupList();

    private final StringFilterGroup howThisWasMadeSection;
    private final StringFilterGroup horizontalShelf;
    private final StringFilterGroup infoCardsSection;
    private final StringFilterGroup macroMarkerShelf;

    public DescriptionsFilter() {
        final StringFilterGroup askSection = new StringFilterGroup(
                Settings.HIDE_ASK_SECTION,
                "youchat_entrypoint."
        );

        final StringFilterGroup attributesSection = new StringFilterGroup(
                Settings.HIDE_ATTRIBUTES_SECTION,
                "video_attributes_section.",
                // It appears to be deprecated, but it can still be used in YouTube 19.05.36.
                "gaming_section.",
                "music_section.",
                "place_section."
        );

        final StringFilterGroup podcastSection = new StringFilterGroup(
                Settings.HIDE_EXPLORE_PODCAST_SECTION,
                "playlist_section."
        );

        final StringFilterGroup transcriptSection = new StringFilterGroup(
                Settings.HIDE_TRANSCRIPT_SECTION,
                "transcript_section."
        );

        final StringFilterGroup videoSummarySection = new StringFilterGroup(
                Settings.HIDE_AI_GENERATED_VIDEO_SUMMARY_SECTION,
                "cell_expandable_metadata."
        );

        addIdentifierCallbacks(
                askSection,
                attributesSection,
                podcastSection,
                transcriptSection,
                videoSummarySection
        );

        howThisWasMadeSection = new StringFilterGroup(
                Settings.HIDE_HOW_THIS_WAS_MADE_SECTION,
                "how_this_was_made_section."
        );

        // In the latest YouTube, the Attribute section has the same path as the Carousel shelf.
        // To hide only the Attribute section, check if the Description panel is open.
        horizontalShelf = new StringFilterGroup(
                Settings.HIDE_ATTRIBUTES_SECTION,
                "horizontal_shelf."
        );

        final StringFilterGroup hypePointsSection = new StringFilterGroup(
                Settings.HIDE_HYPE_POINTS_SECTION,
                "hype_points_factoid."
        );

        infoCardsSection = new StringFilterGroup(
                null,
                "infocards_section."
        );

        macroMarkerShelf = new StringFilterGroup(
                null,
                "macro_markers_carousel."
        );

        macroMarkerShelfGroupList.addAll(
                new ByteArrayFilterGroup(
                        Settings.HIDE_CHAPTERS_SECTION,
                        "chapters_horizontal_shelf"
                ),
                new ByteArrayFilterGroup(
                        Settings.HIDE_KEY_CONCEPTS_SECTION,
                        "learning_concept_macro_markers_carousel_shelf"
                )
        );

        addPathCallbacks(
                howThisWasMadeSection,
                horizontalShelf,
                hypePointsSection,
                infoCardsSection,
                macroMarkerShelf
        );
    }

    private boolean hideInfoCards(String path, int contentIndex) {
        if (contentIndex != 0) {
            return false;
        }

        final boolean hideCreatorSection = Settings.HIDE_CREATOR_SECTION.get();
        final boolean hideFeaturedSection = Settings.HIDE_FEATURED_SECTION.get();

        if (hideCreatorSection && hideFeaturedSection) {
            return true;
        }

        if (!hideCreatorSection && !hideFeaturedSection) {
            return false;
        }

        // "media_lockup.", "structured_description_video_lockup."
        if (path.contains("compact_infocard.")) {
            return hideFeaturedSection;
        }

        return hideCreatorSection;
    }

    @Override
    public boolean isFiltered(String path, String identifier, String allValue, byte[] buffer,
                              StringFilterGroup matchedGroup, FilterContentType contentType, int contentIndex) {
        // Check for the index because of likelihood of false positives.
        if (matchedGroup == howThisWasMadeSection) {
            return contentIndex == 0;
        } else if (matchedGroup == infoCardsSection) {
            return hideInfoCards(path, contentIndex);
        } else if (matchedGroup == macroMarkerShelf) {
            if (contentIndex != 0) {
                return false;
            }
            return macroMarkerShelfGroupList.check(buffer).isFiltered();
        } else if (matchedGroup == horizontalShelf) {
            if (contentIndex != 0) {
                return false;
            }
            if (!RootView.isPlayerActive()) {
                return false;
            }
            return EngagementPanel.isDescription();
        }

        return true;
    }
}
