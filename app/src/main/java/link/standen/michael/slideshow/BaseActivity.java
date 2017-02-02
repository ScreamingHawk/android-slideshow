package link.standen.michael.slideshow;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;

import java.util.ArrayList;
import java.util.List;

import link.standen.michael.slideshow.model.FileItem;

/**
 * Slideshow base activity.
 */
public abstract class BaseActivity extends AppCompatActivity {

	protected String currentPath;
	List<FileItem> fileList = new ArrayList<>();

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
			return true;
		} else if (id == R.id.action_credits) {
			startActivity(new Intent(this, CreditsActivity.class));
		}
		return super.onOptionsItemSelected(item);
	}
}
