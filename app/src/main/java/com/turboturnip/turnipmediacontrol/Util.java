package com.turboturnip.turnipmediacontrol;

import android.graphics.Color;
import android.support.annotation.IntRange;
import android.util.Log;

public class Util {
    private Util(){}

    public static boolean objectsEqual(Object a, Object b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.equals(b);
    }
    public static boolean objectsEqual(CharSequence a, CharSequence b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.toString().equals(b.toString());
    }

    public static final float inverseTippingRatio = 0.75f;
    @IntRange(from = 0, to = 255)
    public static int getInverseBrightness(int brightness) {
        return (brightness > inverseTippingRatio * 255) ? 0 : 255;
    }
    public static float getInverseBrightness(float brightness) {
        return (brightness > inverseTippingRatio * 255) ? 0.0f : 255.0f;
    }
    public static float get01InverseBrightness(float brightness) {
        return (brightness > inverseTippingRatio) ? 0.0f : 1.0f;
    }

    @IntRange(from = 0, to = 255)
    public static int getPerceivedBrightness(int c){
        float r = Color.red(c) / 255.0f;
        float g = Color.green(c) / 255.0f;
        float b = Color.blue(c) / 255.0f;
        Log.i("turnipmediacolor", "For " + c + " R: " + r + " G: " + g + " B: " + b);

        // The weights sum to 1 and each component is in the range [0, 1]
        // so the square rooted value will be in the range [0, 1]
        // and thus the final value will also be in the range [0, 1]
        return (int)Math.round(
                Math.sqrt(
                        r*r * 0.299
                        + g*g * 0.587
                        + b*b * 0.114
                ) * 255.0
        );
    }
}
