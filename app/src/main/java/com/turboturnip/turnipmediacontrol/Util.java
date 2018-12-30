package com.turboturnip.turnipmediacontrol;

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

    public static String toString(Object a) {
        if (a == null)
            return "null";
        return a.toString();
    }
}
