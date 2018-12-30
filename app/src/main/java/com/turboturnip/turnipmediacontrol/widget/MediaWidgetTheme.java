package com.turboturnip.turnipmediacontrol.widget;

import android.graphics.Color;
import android.support.annotation.ColorInt;
import android.support.annotation.IntRange;
import android.support.annotation.Nullable;

public class MediaWidgetTheme {
    // Used for the widget background
    @ColorInt
    public final int backgroundColor;
    // Used for the text and buttons
    @ColorInt
    public final int standoutColor;
    // Used for the navigation button backgrounds
    @ColorInt
    public final int mutedBackgroundColor;

    public MediaWidgetTheme(int backgroundColor) {
        this(backgroundColor, generateStandoutColor(backgroundColor));
    }

    public MediaWidgetTheme(int backgroundColor, int standoutColor) {
        this(backgroundColor, standoutColor, generateMutedColor(backgroundColor, standoutColor));
    }

    public MediaWidgetTheme(int backgroundColor, int standoutColor, int mutedBackgroundColor) {
        this.backgroundColor = backgroundColor;
        this.standoutColor = standoutColor;
        this.mutedBackgroundColor = mutedBackgroundColor;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (!(obj instanceof MediaWidgetTheme)) {
            return false;
        }

        MediaWidgetTheme other = (MediaWidgetTheme)obj;
        if (backgroundColor != other.backgroundColor)
            return false;
        if (standoutColor != other.standoutColor)
            return false;
        if (mutedBackgroundColor != other.mutedBackgroundColor)
            return false;
        return true;
    }

    @ColorInt
    private static int generateStandoutColor(int backgroundColor) {
        // Take brightness of background, choose between white or black depending on the inverse brightness

        // Get "perceived" brightness using technique found here: http://alienryderflex.com/hsp.html
        int perceivedBrightness = getPerceivedBrightness(backgroundColor);
        int inverseBrightness = getInverseBrightness(perceivedBrightness);

        // Disregard alpha here, standout stuff should always be visible
        return Color.rgb(inverseBrightness, inverseBrightness, inverseBrightness);
    }
    @ColorInt
    private static int generateMutedColor(int source, int standout) {
        int r, g, b, a = 255;
        if (Color.alpha(source) < 200) {
            r = Color.red(source);
            g = Color.green(source);
            b = Color.blue(source);
            a = Math.round(lerp(Color.alpha(source), 255, 0.75f));
        } else {
            // Channel-wise interpolation between source and standout
            // This factor feels right for inverseTippingRatio of 0.25, might want to make it not relative
            float factor = 1 - inverseTippingRatio;
            r = Math.round(lerp(Color.red(source), Color.red(standout), factor));
            g = Math.round(lerp(Color.green(source), Color.green(standout), factor));
            b = Math.round(lerp(Color.blue(source), Color.blue(standout), factor));
        }

        return Color.argb(a, r, g, b);
    }

    private static float lerp(float a, float b, float c){
        return a + ((b - a) * c);
    }

    private static final float inverseTippingRatio = 0.75f;
    @IntRange(from = 0, to = 255)
    private static int getInverseBrightness(int brightness) {
        return (brightness > inverseTippingRatio * 255) ? 0 : 255;
    }

    @IntRange(from = 0, to = 255)
    private static int getPerceivedBrightness(int c){
        float r = Color.red(c) / 255.0f;
        float g = Color.green(c) / 255.0f;
        float b = Color.blue(c) / 255.0f;

        // The weights sum to 1 and each component is in the range [0, 1]
        // so the square rooted value will be in the range [0, 1]
        // and thus the value of the square root will also be in the range [0, 1]
        return (int)Math.round(
                Math.sqrt(
                        r*r * 0.299 +
                        g*g * 0.587 +
                        b*b * 0.114
                ) * 255.0
        );
    }
}
