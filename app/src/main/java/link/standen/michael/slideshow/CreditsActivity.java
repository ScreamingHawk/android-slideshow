package link.standen.michael.slideshow;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.widget.TextView;

public class CreditsActivity extends AppCompatActivity {

	private static final String TAG = CreditsActivity.class.getName();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_credits);
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
				R.id.credits_content6
		};
		for (int id : toLink){
			linkify(id);
		}
	}

	/**
	 * Add movement method linkifier.
	 */
	private void linkify(int id){
		((TextView)findViewById(id)).setMovementMethod(LinkMovementMethod.getInstance());
	}
}
