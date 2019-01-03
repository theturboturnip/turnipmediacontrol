package com.turboturnip.turnipmediacontrol;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.support.annotation.ColorInt;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.preference.PreferenceManager;

import com.turboturnip.turnipmediacontrol.widget.MediaWidgetSet;
import com.turboturnip.turnipmediacontrol.widget.MediaWidgetTheme;

import java.util.List;

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

    private static final String THEME_LIST_KEY = "color_choices_list";
    private static final String THEME_QUICK_DIAL_LIST_KEY = "quick_dial_album_art_choices";

    /**
     * Helper method to determine if the device has an extra-large screen. For
     * example, 10" tablets are extra-large.
     */
    private static boolean isXLargeTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
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

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class PreferencesFragment extends PreferenceFragment {
        private PreferenceScreen screen;
        private Preference customColorsGroup;
        private Preference normalThemePref;
        private Preference quickDialThemePref;
        private boolean hasQuickDial;
        private MediaWidgetThemeType normalThemeType;
        private MediaWidgetThemeType quickDialThemeType;

        /**
         * A preference value change listener that updates the preference's summary
         * to reflect its new value.
         */
        private Preference.OnPreferenceChangeListener preferenceListener = new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object value) {
                if (preference.getKey().equals(THEME_LIST_KEY)) {
                    normalThemeType = themeTypeFromString(value.toString());

                    boolean wantsQuickDial = false;
                    boolean wantsCustom = false;
                    if (normalThemeType == MediaWidgetThemeType.CUSTOM)
                        wantsCustom = true;
                    else if (normalThemeType == MediaWidgetThemeType.ALBUM_ART)
                        wantsQuickDial = true;
                    if (quickDialThemeType == MediaWidgetThemeType.CUSTOM)
                        wantsCustom = true;

                    PreferencesFragment.this.updateVisiblePreferences(wantsCustom, wantsQuickDial);
                } else if (preference.getKey().equals(THEME_QUICK_DIAL_LIST_KEY) && hasQuickDial){
                    quickDialThemeType = themeTypeFromString(value.toString());

                    if (quickDialThemeType == MediaWidgetThemeType.CUSTOM) {
                        PreferencesFragment.this.updateVisiblePreferences(true, true);
                    } else {
                        PreferencesFragment.this.updateVisiblePreferences(false, true);
                    }
                }

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

        private void registerPreference(Preference preference) {
            // Set the listener to watch for value changes.
            preference.setOnPreferenceChangeListener(preferenceListener);

            // Trigger the listener immediately with the preference's
            // current value.
            preferenceListener.onPreferenceChange(preference,
                    PreferenceManager
                            .getDefaultSharedPreferences(preference.getContext())
                            .getString(preference.getKey(), ""));
        }

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            addPreferencesFromResource(R.xml.preferences);

            screen = getPreferenceScreen();
            customColorsGroup = findPreference("custom_colors_parent");
            normalThemePref = findPreference(THEME_LIST_KEY);
            quickDialThemePref = findPreference(THEME_QUICK_DIAL_LIST_KEY);

            updateVisiblePreferences(false, false);

            registerPreference(normalThemePref);
            registerPreference(quickDialThemePref);
        }

        void updateVisiblePreferences(boolean shouldHaveCustom, boolean shouldHaveQuickDial){
            screen.addPreference(customColorsGroup);
            screen.addPreference(quickDialThemePref);

            screen.removePreference(customColorsGroup);
            screen.removePreference(quickDialThemePref);

            if (shouldHaveQuickDial)
                screen.addPreference(quickDialThemePref);
            if (shouldHaveCustom)
                screen.addPreference(customColorsGroup);

            hasQuickDial = shouldHaveQuickDial;
        }
    }

    public enum MediaWidgetThemeType {
        LIGHT,
        DARK,
        ALBUM_ART,
        CUSTOM
    }
    private static MediaWidgetThemeType themeTypeFromString(String theme) {
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
    public static MediaWidgetThemeType getSelectedTheme(Context context, boolean quickDial) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);

        String theme = preferences.getString(THEME_LIST_KEY, null);
        MediaWidgetThemeType themeType = themeTypeFromString(theme);
        if (themeType == MediaWidgetThemeType.ALBUM_ART && quickDial) {
            theme = preferences.getString("quick_dial_album_art_choices", null);
            return themeTypeFromString(theme);
        }
        return themeType;
    }
    public static MediaWidgetTheme decodeExistingThemeType(Context context, MediaWidgetThemeType themeType) {
        switch(themeType) {
            case CUSTOM:
            {
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
                @ColorInt int backgroundColor = preferences.getInt("custom_bg_color", Color.WHITE);
                Integer standoutColor = null, mutedBgColor = null;
                if (preferences.contains("custom_emph_color")) {
                    standoutColor = preferences.getInt("custom_emph_color", Color.BLACK);
                }
                if (preferences.contains("custom_mute_bg_color")) {
                    mutedBgColor = preferences.getInt("custom_mute_bg_color", Color.GRAY);
                }

                return new MediaWidgetTheme(backgroundColor, standoutColor, mutedBgColor);
            }
            case ALBUM_ART:
                throw new UnsupportedOperationException("Can't decode ALBUM_ART, must be done in widget");
            case LIGHT:
                return new MediaWidgetTheme(context.getResources().getColor(R.color.widgetLightBG, context.getTheme()));
            case DARK:
                return new MediaWidgetTheme(context.getResources().getColor(R.color.widgetDarkBG, context.getTheme()));
            default:
                throw new UnsupportedOperationException();
        }
    }
}
