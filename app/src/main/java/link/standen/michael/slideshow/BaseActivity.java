package link.standen.michael.slideshow;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import de.cketti.library.changelog.ChangeLog;
import link.standen.michael.slideshow.dialog.ControlsDialog;
import link.standen.michael.slideshow.model.FileItem;

/**
 * Slideshow base activity.
 */
public abstract class BaseActivity extends AppCompatActivity {

	String currentPath;
	List<FileItem> fileList = new ArrayList<>();
	private Dialog changeLog;

	static final String STATE_PATH = "pathState";

	private static final String CHANGE_LOG_CSS = "body { padding: 0.8em; } " +
			"h1 { margin-left: 0px; font-size: 1.2em; } " +
			"ul { padding-left: 1.2em; } " +
			"li { margin-left: 0px; }";
	private static final String DEFAULT_LANGUAGE = new Locale("en").getLanguage();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// Check whether we're recreating a previously destroyed instance
		if (savedInstanceState != null) {
			// Restore value of members from saved state
			currentPath = savedInstanceState.getString(STATE_PATH);
		} else {
			// Probably initialize members with default values for a new instance
			currentPath = Environment.getExternalStorageDirectory().getAbsolutePath();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.menu_main, menu);
		return true;
	}

	/**
	 * Handle options menu
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();

		if (id == android.R.id.home) {
			// Do the same thing as the back button.
			onBackPressed();
			return true;
		} else if (id == R.id.action_settings) {
			Intent intent = new Intent(this, SettingsActivity.class);
			intent.putExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT, SettingsActivity.SlideshowPreferenceFragment.class.getName());
			intent.putExtra(PreferenceActivity.EXTRA_NO_HEADERS, true);
			startActivity(intent);
			return true;
		} else if (id == R.id.action_credits) {
			startActivity(new Intent(this, CreditsActivity.class));
			return true;
		} else if (id == R.id.action_change_log) {
			showChangeLog(true);
			return true;
		} else if (id == R.id.action_controls) {
			new ControlsDialog().show(getSupportFragmentManager(), ControlsDialog.TAG);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * Get the root location, considering the preferences.
	 */
	String getRootLocation(){
		if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean("use_device_root", false)){
			return "";
		}
		return Environment.getExternalStorageDirectory().getAbsolutePath();
	}

	/**
	 * Show the change log.
	 * Shows the full change log when nothing is in "What's New" log. Shows "What's New" log otherwise.
	 * @param force Force the change log to be displayed, if false only displayed if new content.
	 */
	void showChangeLog(boolean force) {
		// Only show if forced or in English, as change log is only in English
		if ((isEnglish() || force) && changeLog == null) {
			final ChangeLog cl = new ChangeLog(this, CHANGE_LOG_CSS);
			if (force || cl.isFirstRun()) {
				if (cl.getChangeLog(false).size() == 0) {
					// Force the display of the full dialog list.
					changeLog = cl.getFullLogDialog();
				} else {
					// Show only the new stuff.
					changeLog = cl.getLogDialog();
				}
				changeLog.setOnDismissListener(new DialogInterface.OnDismissListener() {
					@Override
					public void onDismiss(DialogInterface dialogInterface) {
						changeLog = null;
					}
				});
				changeLog.show();
			}
		}
	}

	/**
	 * Check application is running in English.
	 */
	private boolean isEnglish(){
		Locale locale;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N){
			locale = getResources().getConfiguration().getLocales().get(0);
		} else {
			//noinspection deprecation
			locale = getResources().getConfiguration().locale;
		}
		return locale.getLanguage().equals(DEFAULT_LANGUAGE);
	}


	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putString(STATE_PATH, currentPath);
		// Always call the superclass so it can save the view hierarchy state
		super.onSaveInstanceState(outState);
	}

}
