package com.turboturnip.turnipmediacontrol;

import android.util.Log;

public class LogHelper {
    public static String getTag(Class c) {
        return c.getCanonicalName();
    }

    public enum LogLevel {
        Verbose(0),
        Info(1),
        Debug(2),
        Error(3),
        NoLog(4);

        private final int priority;
        LogLevel(int priority){
            this.priority = priority;
        }

        public boolean aboveThreshold(LogLevel threshold) {
            return priority >= threshold.priority;
        }
    }
    public static LogLevel logThreshold = LogLevel.Verbose;
    private LogHelper(){}

    public static void v(String tag, Object message) {
        if (LogLevel.Verbose.aboveThreshold(logThreshold))
            Log.v(tag, message.toString());
    }
    public static void i(String tag, Object message) {
        if (LogLevel.Info.aboveThreshold(logThreshold))
            Log.i(tag, message.toString());
    }
    public static void d(String tag, Object message) {
        if (LogLevel.Debug.aboveThreshold(logThreshold))
            Log.d(tag, message.toString());
    }
    public static void e(String tag, Object message) {
        if (LogLevel.Error.aboveThreshold(logThreshold))
            Log.e(tag, message.toString());
    }
}
