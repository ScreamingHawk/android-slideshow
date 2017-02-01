package link.standen.michael.slideshow;

import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.os.Environment;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

import link.standen.michael.slideshow.model.FileItem;

/**
 * Slideshow base activity.
 */
public abstract class BaseActivity extends AppCompatActivity {

	private static final String TAG = "BaseActivity";

	protected String absPath = Environment.getExternalStorageDirectory().getAbsolutePath();
	protected String currentPath;
	ArrayList<FileItem> fileList = new ArrayList<>();

	protected void updateFileList(){
		Log.d(TAG, "updateFileList currentPath: "+currentPath);

		// Set title
		setTitle(currentPath.replace(absPath, "") + File.separatorChar);

		// Create file list
		fileList = new ArrayList<>();
		File dir = new File(currentPath);
		if (!dir.canRead()){
			setTitle(getTitle() + getResources().getString(R.string.inaccessible));
		}
		File[] files = dir.listFiles();
		if (files != null){
			for (File file : files){
				final FileItem item = new FileItem();
				item.setName(file.getName());
				item.setPath(file.getAbsolutePath());
				item.setIsDirectory(file.isDirectory());
				if (!item.getIsDirectory()){
					new Handler().post(new Runnable() {
						@Override
						public void run() {
							item.setThumbnail(ThumbnailUtils.extractThumbnail(
									BitmapFactory.decodeFile(item.getPath()),
									(int)getResources().getDimension(R.dimen.file_image_width),
									(int)getResources().getDimension(R.dimen.file_image_height)));
						}
					});
				}
				fileList.add(item);
			}
		}
		Collections.sort(fileList);
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
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
}
