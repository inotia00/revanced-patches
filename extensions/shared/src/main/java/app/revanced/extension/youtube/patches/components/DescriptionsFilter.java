package app.revanced.extension.youtube.patches.components;

import androidx.annotation.Nullable;

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
        // game section, music section and places section now use the same identifier in the latest version.
        final StringFilterGroup attributesSection = new StringFilterGroup(
                Settings.HIDE_ATTRIBUTES_SECTION,
                "gaming_section.eml",
                "music_section.eml",
                "place_section.eml",
                "video_attributes_section.eml"
        );

        final StringFilterGroup podcastSection = new StringFilterGroup(
                Settings.HIDE_PODCAST_SECTION,
                "playlist_section.eml"
        );

        final StringFilterGroup transcriptSection = new StringFilterGroup(
                Settings.HIDE_TRANSCRIPT_SECTION,
                "transcript_section.eml"
        );

        final StringFilterGroup videoSummarySection = new StringFilterGroup(
                Settings.HIDE_AI_GENERATED_VIDEO_SUMMARY_SECTION,
                "cell_expandable_metadata.eml"
        );

        addIdentifierCallbacks(
                attributesSection,
                podcastSection,
                transcriptSection,
                videoSummarySection
        );

        howThisWasMadeSection = new StringFilterGroup(
                Settings.HIDE_CONTENTS_SECTION,
                "how_this_was_made_section.eml"
        );

        // In the latest YouTube, the Attribute section has the same path as the Carousel shelf.
        // To hide only the Attribute section, check if the Description panel is open.
        horizontalShelf = new StringFilterGroup(
                Settings.HIDE_ATTRIBUTES_SECTION,
                "horizontal_shelf.eml"
        );

        infoCardsSection = new StringFilterGroup(
                Settings.HIDE_INFO_CARDS_SECTION,
                "infocards_section.eml"
        );

        macroMarkerShelf = new StringFilterGroup(
                null,
                "macro_markers_carousel.eml"
        );

        addPathCallbacks(
                howThisWasMadeSection,
                horizontalShelf,
                infoCardsSection,
                macroMarkerShelf
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
    }

    @Override
    public boolean isFiltered(String path, @Nullable String identifier, String allValue, byte[] protobufBufferArray,
                              StringFilterGroup matchedGroup, FilterContentType contentType, int contentIndex) {
        // Check for the index because of likelihood of false positives.
        if (matchedGroup == howThisWasMadeSection || matchedGroup == infoCardsSection) {
            if (contentIndex != 0) {
                return false;
            }
        } else if (matchedGroup == macroMarkerShelf) {
            if (contentIndex != 0) {
                return false;
            }
            if (!macroMarkerShelfGroupList.check(protobufBufferArray).isFiltered()) {
                return false;
            }
        } else if (matchedGroup == horizontalShelf) {
            if (contentIndex != 0) {
                return false;
            }
            if (!RootView.isPlayerActive()) {
                return false;
            }
            if (!EngagementPanel.isDescription()) {
                return false;
            }
        }

        return super.isFiltered(path, identifier, allValue, protobufBufferArray, matchedGroup, contentType, contentIndex);
    }
}
