package com.turboturnip.turnipmediacontrol;

public class Util {
    public static boolean objectsEqual(Object a, Object b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.equals(b);
    }
}
