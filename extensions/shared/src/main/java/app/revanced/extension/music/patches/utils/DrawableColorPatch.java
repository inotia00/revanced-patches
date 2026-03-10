package app.revanced.extension.music.patches.utils;

import static app.revanced.extension.shared.utils.Utils.isSDKAbove;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import org.apache.commons.lang3.ArrayUtils;

import app.revanced.extension.shared.utils.ResourceUtils;

@SuppressWarnings("unused")
public class DrawableColorPatch {
    private static final int[] DARK_COLORS = {
            0xFF212121, // comments box background
            0xFF030303, // button container background in album
            0xFF000000, // button container background in playlist
    };

    // background colors
    private static final Drawable headerGradient =
            ResourceUtils.getDrawable("revanced_header_gradient");
    private static final Drawable transparentDrawable =
            new ColorDrawable(Color.TRANSPARENT);
    private static final int blackColor =
            ResourceUtils.getColor("yt_black1");

    public static int getLithoColor(int colorValue) {
        return ArrayUtils.contains(DARK_COLORS, colorValue)
                ? blackColor
                : colorValue;
    }

    public static void setHeaderGradient(ViewGroup viewGroup) {
        viewGroup.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            if (!(viewGroup.getChildAt(0) instanceof ViewGroup firstChildView))
                return;
            View secondChildView = firstChildView.getChildAt(0);

            if (secondChildView instanceof ImageView gradientView) {
                // Album
                setHeaderGradient(gradientView);
            } else if (secondChildView instanceof ViewGroup thirdChildView &&
                    thirdChildView.getChildCount() == 1 &&
                    thirdChildView.getChildAt(0) instanceof ImageView gradientView) {
                // Playlist
                setHeaderGradient(gradientView);
            }
        });
    }

    private static void setHeaderGradient(ImageView gradientView) {
        // headerGradient is litho, so this view is sometimes used elsewhere, like the button of the action bar.
        // In order to prevent the gradient to be applied to the button of the action bar,
        // Add a layout listener to the ImageView.
        if (isSDKAbove(23) && headerGradient != null && gradientView.getForeground() == null) {
            gradientView.setForeground(headerGradient);
            gradientView.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
                if (gradientView.getParent() instanceof View view &&
                        view.getContentDescription() != null &&
                        gradientView.getForeground() == headerGradient
                ) {
                    gradientView.setForeground(transparentDrawable);
                }
            });
        }
    }
}


