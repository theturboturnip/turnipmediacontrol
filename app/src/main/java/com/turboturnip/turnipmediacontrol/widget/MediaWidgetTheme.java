package com.turboturnip.turnipmediacontrol.widget;

import android.graphics.Color;
import android.util.Log;

import com.turboturnip.turnipmediacontrol.Util;

public class MediaWidgetTheme {
    // Used for the widget background
    public final int backgroundColor;
    // Used for the text and buttons
    public final int standoutColor;
    // Used for the navigation button backgrounds
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

    private static int generateStandoutColor(int backgroundColor) {
        // Take brightness of background, generate either white or black

        // Get "perceived" brightness using technique found here: http://alienryderflex.com/hsp.html
        int perceivedBrightness = Util.getPerceivedBrightness(backgroundColor);
        int inverseBrightness = Util.getInverseBrightness(perceivedBrightness);
        Log.i("turnipmediacolor", "Perceived Brightness of " + backgroundColor + ": " + perceivedBrightness + " inverse: " + inverseBrightness);

        // Disregard alpha here, standout stuff should always be visible
        return Color.rgb(inverseBrightness, inverseBrightness, inverseBrightness);
    }
    private static int generateMutedColor(int source, int standout) {
        // channel-relative interpolation between source and standout
        float factor = 1 - Util.inverseTippingRatio;
        int r = Math.round(lerp(Color.red(source), Color.red(standout), factor));
        int g = Math.round(lerp(Color.green(source), Color.green(standout), factor));
        int b = Math.round(lerp(Color.blue(source), Color.blue(standout), factor));
        Log.i("turnipmediacolor", "muted " + r + " " + g + " " + b);

        // TODO: Figure out alpha for this
        return Color.argb(255, r, g, b);
        // Pick a color with the same hue/saturation as the source but with a brightness 25% between it and it's inverse
        // Use HSV because manually writing out the HSP calculations is boring
        /*float[] hsv = new float[3];
        Color.colorToHSV(source, hsv);
        //hsv[2] = 0.8f;
        Log.i("turnipmediacolor", hsv[0] + " : " + hsv[1] + " : " + hsv[2]);
        hsv[1] = hsv[1] + (Util.get01InverseBrightness(hsv[1]) - hsv[1]) * 0.5f;
        Log.i("turnipmediacolor", hsv[0] + " : " + hsv[1] + " : " + hsv[2]);
        return Color.HSVToColor(hsv);*/
        //Log.i("turnipmediacolor", "Value of " + source + ": " + hsv[2] + " inverse: " + Util.getInverseBrightness(hsv[2]));
        /*float perceivedBrightness = Util.getPerceivedBrightness(source);
        float inverseBrightness = Util.getInverseBrightness(perceivedBrightness);
        float intendedBrightness = perceivedBrightness + (inverseBrightness - perceivedBrightness) * 0.5f;
        Log.i("turnipmediacolor", "Perceived Brightness: " + perceivedBrightness + " inverse: " + inverseBrightness + " intended: " + intendedBrightness);

        // This is not the right way to do this, but its pretty late at night so whatever
        // scale the color while maintaining the r:g:b ratios
        // and trying to get the HSP value close to the intended
        // HSP = r^2 * w_r + g^2 * w_g + b^2 * w_b
        // where w_r + w_g + w_b = 1
        // => when r,g, and b are multiplied by a constant x and incremented by y
        // r^2 => (xr + y)^2 = x^2r^2 + 2xry + y^2
        // r^2 * w_r => (x^2r^2 + 2xry + y^2) * w_r = x^2 * r^2 * w_r + y^2 * w_r + 2xry*w_r
        // r^2 * w_r + g^2 * w_g + b^2 * w_b =>
        //     x^2 * (r^2 * w_r + b^2 * w_b + g^2 * w_g) + y^2 + 2xy(r * w_r + b * w_b + g * w_g)
        // HSP_new = sqrt(r^2...)
        //        => sqrt(x^2 * (HSP_old^2) + y^2 + 2xy(..))
        // HSP_new^2 = (x * HSP_old)^2 + y*(y + 2x(..))

        // Choosing x: try the simple form where y = 0 (x = sqrt(HSP_new / HSP_old))
        // If this takes any component > 100%, problems!!!

        // HSP1 = sqrt(x^2 * r^2 * wr + x^2 * g^2 * wg + x^2 * b^2 * wb)
        //      = sqrt(x^2 * (r^2 * wr + g^2 * wg + b^2 * wb))
        //      = x * HSP_original
        // => for a target and initial HSP value, x = sqrt(HSP_target / HSP_original)
        float x = (intendedBrightness / perceivedBrightness);
        byte newR = (byte)Math.round(Color.red(source) * x * 255);
        byte newG = (byte)Math.round(Color.green(source) * x * 255);
        byte newB = (byte)Math.round(Color.blue(source) * x * 255);
        int newColor = Color.rgb(newR, newG, newB);
        Log.i("turnipmediacolor", "Final Perceived Brightness: " + Util.getPerceivedBrightness(newColor));
        return newColor;*/
    }
    private static float lerp(float a, float b, float c){
        Log.i("turnipmediacolor", "lerp: " + a + " -> " + b + "  by " + c);
        Log.i("turnipmediacolor", "lerp: " + a + " + " + ((b -a) * c));
        return a + ((b - a) * c);
    }
}
