package app.revanced.extension.youtube.patches.components;

import androidx.annotation.Nullable;

import app.revanced.extension.shared.patches.components.Filter;
import app.revanced.extension.shared.patches.components.StringFilterGroup;
import app.revanced.extension.youtube.settings.Settings;

@SuppressWarnings("unused")
public final class LayoutComponentsFilter extends Filter {
    private static final String ACCOUNT_HEADER_PATH = "account_header.eml";
    private static final String HANDLE_PATH = "|CellType|ContainerType|ContainerType|ContainerType|TextType|";

    public LayoutComponentsFilter() {
        addIdentifierCallbacks(
                new StringFilterGroup(
                        Settings.HIDE_VISUAL_SPACERS,
                        "cell_divider"
                )
        );

        addPathCallbacks(
                new StringFilterGroup(
                        Settings.HIDE_HANDLE,
                        ACCOUNT_HEADER_PATH
                )
        );
    }

    @Override
    public boolean isFiltered(String path, @Nullable String identifier, String allValue, byte[] protobufBufferArray,
                              StringFilterGroup matchedGroup, FilterContentType contentType, int contentIndex) {
        if (contentType == FilterContentType.PATH && (contentIndex != 0 || !path.contains(HANDLE_PATH))) {
            return false;
        }

        return super.isFiltered(path, identifier, allValue, protobufBufferArray, matchedGroup, contentType, contentIndex);
    }
}
