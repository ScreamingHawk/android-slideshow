package link.standen.michael.slideshow;

import android.annotation.SuppressLint;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import java.util.Locale;

public class CreditsActivity extends AppCompatActivity {

	private static final String TAG = CreditsActivity.class.getName();
	private static final String DEFAULT_LANGUAGE = new Locale("en").getLanguage();

	@SuppressLint("SetTextI18n")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_credits);

		Toolbar toolbar = findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);
		if (getSupportActionBar() != null) {
			getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		}

		setTitle(R.string.title_activity_credits);

		// Version
		try {
			PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
			((TextView)findViewById(R.id.credits_version)).setText("v"+pInfo.versionName);
		} catch (PackageManager.NameNotFoundException e) {
			Log.e(TAG, "Unable to get package version", e);
		}

		// Set up links
		int[] toLink = new int[]{
				R.id.credits_creator,
				R.id.credits_content1,
				R.id.credits_content2,
				R.id.credits_content3,
				R.id.credits_content4,
				R.id.credits_content5,
				R.id.credits_content6,
		};
		for (int id : toLink){
			linkify(id);
		}
		if (getCurrentLocale().getLanguage().equals(DEFAULT_LANGUAGE)){
			findViewById(R.id.credits_content6).setVisibility(View.GONE);
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == android.R.id.home) {
			onBackPressed();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * Add movement method linkifier.
	 */
	private void linkify(int id){
		((TextView)findViewById(id)).setMovementMethod(LinkMovementMethod.getInstance());
	}

	/**
	 * A version save way to get the current locale.
	 */
	private Locale getCurrentLocale(){
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N){
			return getResources().getConfiguration().getLocales().get(0);
		} else {
			//noinspection deprecation
			return getResources().getConfiguration().locale;
		}
	}
}
