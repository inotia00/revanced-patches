package app.revanced.extension.music.patches.components;

import app.revanced.extension.music.settings.Settings;
import app.revanced.extension.shared.patches.components.ByteArrayFilterGroup;
import app.revanced.extension.shared.patches.components.ByteArrayFilterGroupList;
import app.revanced.extension.shared.patches.components.Filter;
import app.revanced.extension.shared.patches.components.StringFilterGroup;

@SuppressWarnings("unused")
public final class PlayerFlyoutMenuFilter extends Filter {
    private final StringFilterGroup listItem;
    private final ByteArrayFilterGroupList bufferGroupList = new ByteArrayFilterGroupList();

    public PlayerFlyoutMenuFilter() {
        addIdentifierCallbacks(
                new StringFilterGroup(
                        Settings.HIDE_FLYOUT_MENU_3_COLUMN_COMPONENT,
                        "music_highlight_menu_item_carousel.",
                        "tile_button_carousel."
                )
        );

        listItem = new StringFilterGroup(
                Settings.HIDE_FLYOUT_MENU_DOWNLOAD,
                "list_item."
        );

        bufferGroupList.addAll(
                new ByteArrayFilterGroup(
                        Settings.HIDE_FLYOUT_MENU_DOWNLOAD,
                        "yt_outline_download"
                ),
                new ByteArrayFilterGroup(
                        Settings.HIDE_FLYOUT_MENU_TASTE_MATCH,
                        "yt_outline_circles_overlap"
                )
        );

        addPathCallbacks(listItem);
    }

    @Override
    public boolean isFiltered(String path, String identifier, String allValue, byte[] buffer,
                              StringFilterGroup matchedGroup, FilterContentType contentType, int contentIndex) {
        if (matchedGroup == listItem) {
            return contentIndex == 0 && bufferGroupList.check(buffer).isFiltered();
        }

        return true;
    }
}
