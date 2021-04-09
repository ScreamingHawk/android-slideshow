package link.standen.michael.slideshow;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.SwitchPreference;
import android.support.v7.app.ActionBar;
import android.preference.PreferenceFragment;
import android.view.MenuItem;

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
	/**
	 * A preference value change listener that updates the preference's summary
	 * to reflect its new value.
	 */
	private final static Preference.OnPreferenceChangeListener listSummaryListener = new Preference.OnPreferenceChangeListener() {
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

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setupActionBar();
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
	 * This method stops fragment injection in malicious applications.
	 * Make sure to deny any unknown fragments here.
	 */
	protected boolean isValidFragment(String fragmentName) {
		return PreferenceFragment.class.getName().equals(fragmentName)
				|| SlideshowPreferenceFragment.class.getName().equals(fragmentName);
	}

	/**
	 * This fragment shows general preferences only.
	 */
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public static class SlideshowPreferenceFragment extends PreferenceFragment {
		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			addPreferencesFromResource(R.xml.preferences);
			setHasOptionsMenu(true);

			final SwitchPreference reverseOrderPref = (SwitchPreference)findPreference("reverse_order");
			final SwitchPreference randomOrderPref = (SwitchPreference)findPreference("random_order");
			// Enabling reverse disables random
			reverseOrderPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
				@Override
				public boolean onPreferenceChange(Preference preference, Object newValue) {
					if (Boolean.TRUE.equals(newValue)){
						randomOrderPref.setChecked(false);
					}
					return true;
				}
			});
			// Enabling random disables reverse
			randomOrderPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
				@Override
				public boolean onPreferenceChange(Preference preference, Object newValue) {
					if (Boolean.TRUE.equals(newValue)){
						reverseOrderPref.setChecked(false);
					}
					return true;
				}
			});

			final SwitchPreference rememberLocationPref = (SwitchPreference)findPreference("remember_location");
			final SwitchPreference autoStartPref = (SwitchPreference)findPreference("auto_start");
			// Disabling remember location disables auto start
			rememberLocationPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
				@Override
				public boolean onPreferenceChange(Preference preference, Object newValue) {
					if (Boolean.FALSE.equals(newValue)){
						autoStartPref.setChecked(false);
					}
					return true;
				}
			});

			final SwitchPreference glideSupportPref = (SwitchPreference)findPreference("glide_image_strategy");
			final SwitchPreference preloadPref = (SwitchPreference)findPreference("preload_images");
			final SwitchPreference gifPref = (SwitchPreference)findPreference("enable_gif_support");
			// Disabling glide support disables preloading and GIF support
			glideSupportPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
				@Override
				public boolean onPreferenceChange(Preference preference, Object newValue) {
					if (Boolean.FALSE.equals(newValue)){
						preloadPref.setChecked(false);
						gifPref.setChecked(false);
					}
					return true;
				}
			});

			final SwitchPreference imageDetailsPref = (SwitchPreference)findPreference("image_details");
			final SwitchPreference imageDetailsDuringPref = (SwitchPreference)findPreference("image_details_during");
			// Disabling remember location disables auto start
			imageDetailsPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
				@Override
				public boolean onPreferenceChange(Preference preference, Object newValue) {
					if (Boolean.FALSE.equals(newValue)){
						imageDetailsDuringPref.setChecked(false);
					}
					return true;
				}
			});

			// Bind the summaries of List Preferences
			ListPreference onCompletePref = (ListPreference)findPreference("action_on_complete");
			onCompletePref.setOnPreferenceChangeListener(listSummaryListener);
			listSummaryListener.onPreferenceChange(onCompletePref, onCompletePref.getValue());
		}

		@Override
		public boolean onOptionsItemSelected(MenuItem item) {
			int id = item.getItemId();
			if (id == android.R.id.home) {
				getActivity().onBackPressed();
				return true;
			}
			return super.onOptionsItemSelected(item);
		}
	}
}
