package com.turboturnip.turnipmediacontrol;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.MenuItem;

import com.turboturnip.turnipmediacontrol.widget.MediaWidgetSet;
import com.turboturnip.turnipmediacontrol.widget.MediaWidgetTheme;

import java.util.List;
import java.util.Set;

/**
 * A {@link PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
public class SettingsActivity extends AppCompatPreferenceActivity {
    private static final String TAG = LogHelper.getTag(SettingsActivity.class);
    /**
     * A preference value change listener that updates the preference's summary
     * to reflect its new value.
     */
    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            String stringValue = value.toString();

            if (preference instanceof ListPreference) {
                // For list preferences, look up the correct display value in
                // the preference's 'entries' list.
                ListPreference listPreference = (ListPreference) preference;
                int index = listPreference.findIndexOfValue(stringValue);

                // Set the summary to reflect the new value.
                preference.setSummary(
                        index >= 0
                                ? listPreference.getEntries()[index]
                                : null);
            } else {
                // For all other preferences, set the summary to the value's
                // simple string representation.
                preference.setSummary(stringValue);
            }
            return true;
        }
    };

    /**
     * Helper method to determine if the device has an extra-large screen. For
     * example, 10" tablets are extra-large.
     */
    private static boolean isXLargeTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
    }

    /**
     * Binds a preference's summary to its value. More specifically, when the
     * preference's value is changed, its summary (line of text below the
     * preference title) is updated to reflect the value. The summary is also
     * immediately updated upon calling this method. The exact display format is
     * dependent on the type of preference.
     *
     * @see #sBindPreferenceSummaryToValueListener
     */
    private static void bindPreferenceSummaryToValue(Preference preference) {
        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

        // Trigger the listener immediately with the preference's
        // current value.
        sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                PreferenceManager
                        .getDefaultSharedPreferences(preference.getContext())
                        .getString(preference.getKey(), ""));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupActionBar();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // This works because onPause is called when leaving the app through the home button
        // TODO: Is this hacky?
        LogHelper.e(TAG, "CHANGING THING");
        MediaWidgetSet.instance.updateKnownWidgets();
    }

    /**
     * Set up the {@link android.app.ActionBar}, if the API is available.
     */
    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // Show the Up button in the action bar.
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onIsMultiPane() {
        return isXLargeTablet(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.pref_headers, target);
    }

    /**
     * This method stops fragment injection in malicious applications.
     * Make sure to deny any unknown fragments here.
     */
    protected boolean isValidFragment(String fragmentName) {
        return PreferenceFragment.class.getName().equals(fragmentName)
                || PreferencesFragment.class.getName().equals(fragmentName);
    }


//    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
//    public static class PreferencesFragment extends PreferenceFragment {
//
//        @Override
//        public void onCreatePreferences(Bundle bundle, String s) {
//            setPreferencesFromResource(R.xml.preferences, s);
//
//            bindPreferenceSummaryToValue(findPreference("color_choices_list"));
//        }
//
//        @Override
//        public void onDisplayPreferenceDialog(Preference preference) {
//
//                super.onDisplayPreferenceDialog(preference);
//        }
//    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class PreferencesFragment extends PreferenceFragment {

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferences);
            bindPreferenceSummaryToValue(findPreference("color_choices_list"));
        }



        
    }

    public enum MediaWidgetThemeType {
        LIGHT,
        DARK,
        ALBUM_ART,
        CUSTOM
    }
    public static MediaWidgetThemeType getSelectedTheme(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        String theme = preferences.getString("color_choices_list", null);
        if (theme == null) return MediaWidgetThemeType.LIGHT;
        switch (theme) {
            case "light":
                return MediaWidgetThemeType.LIGHT;
            case "dark":
                return MediaWidgetThemeType.DARK;
            case "album":
                return MediaWidgetThemeType.ALBUM_ART;
            case "custom":
                return MediaWidgetThemeType.CUSTOM;
            default:
                return MediaWidgetThemeType.LIGHT;
        }
    }
    public static MediaWidgetTheme decodeExistingThemeType(Context context, MediaWidgetThemeType themeType) {
        switch(themeType) {
            case ALBUM_ART:
                //throw new UnsupportedOperationException("Can't decode ALBUM_ART, must be done in widget");
            case CUSTOM:
            case LIGHT:
                return new MediaWidgetTheme(context.getResources().getColor(R.color.widgetLightBG, context.getTheme()));
            case DARK:
                return new MediaWidgetTheme(context.getResources().getColor(R.color.widgetDarkBG, context.getTheme()));
            default:
                throw new UnsupportedOperationException();
        }
    }
}
