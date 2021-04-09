package link.standen.michael.slideshow;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import java.io.File;

import link.standen.michael.slideshow.adapter.FileItemArrayAdapter;
import link.standen.michael.slideshow.model.FileItem;
import link.standen.michael.slideshow.model.FileItemViewHolder;
import link.standen.michael.slideshow.util.FileItemHelper;

/**
 * Slideshow main activity.
 */
public class MainActivity extends BaseActivity {

	private static final String TAG = MainActivity.class.getName();

	private String rootLocation;

	private static final String LIST_STATE = "listState";
	private Parcelable listState;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		Toolbar toolbar = findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

		// Get path
		rootLocation = getRootLocation();

		// Path is external absolute directory
		if (currentPath == null) {
			currentPath = Environment.getExternalStorageDirectory().getAbsolutePath();
		}
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
		if (preferences.getBoolean("remember_location", false)){
			// Override using remembered location
			currentPath = preferences.getString("remembered_location", currentPath);
			if (preferences.getBoolean("auto_start", false)){
				// Start the slideshow automatically
				startSlideshowAt(currentPath, preferences.getString("remembered_image", null), true);
				return;
			}
		}
		if (getIntent().hasExtra("path")){
			// Override using passed value
			currentPath = getIntent().getStringExtra("path");
		}
	}

	@Override
	protected void onStart() {
		super.onStart();

		showChangeLog(false);

		// Permission check
		if (isStoragePermissionGranted()){
			updateListView();
		}
		// else wait for permission handler to continue
	}

	@Override
	protected void onResume(){
		super.onResume();
		// Update the root location in case preferences changed
		rootLocation = getRootLocation();
		if (!currentPath.contains(rootLocation)){
			// Changed from root to non-root preference while in an upper directory. Reset
			currentPath = rootLocation;
			updateListView();
		}
		// Restore the list view scroll location
		if (listState != null) {
			((ListView) findViewById(android.R.id.list)).onRestoreInstanceState(listState);
		}
		listState = null;
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState){
		super.onRestoreInstanceState(savedInstanceState);
		listState = savedInstanceState.getParcelable(LIST_STATE);
	}

	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		listState = ((ListView) findViewById(android.R.id.list)).onSaveInstanceState();
		outState.putParcelable(LIST_STATE, listState);
	}

	private void updateListView(){
		fileList = new FileItemHelper(this).getFileList(currentPath);
		FileItemHelper fileItemHelper = new FileItemHelper(this);

		if (currentPath.equals(Environment.getExternalStorageDirectory().getAbsolutePath())) {
			// Put a star on special folders
			String[] specialPaths = new String[]{
					Environment.DIRECTORY_DCIM,
					Environment.DIRECTORY_PICTURES
			};
			for (String path : specialPaths) {
				FileItem specialItem = fileItemHelper.createFileItem(
						Environment.getExternalStoragePublicDirectory(path));
				int index = fileList.indexOf(specialItem);
				if (index != -1) {
					fileList.get(index).setIsSpecial();
				}
			}
		}

		// Set title
		this.setTitle(currentPath.replace(rootLocation, "") + File.separatorChar);
		if (!new File(currentPath).canRead()){
			this.setTitle(String.format("%s %s",
					getTitle(),
					getResources().getString(R.string.inaccessible)));

			// Add Go Home item
			fileList.clear();
			fileList.add(fileItemHelper.createGoHomeFileItem());
		} else if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean("play_from_here", false)) {
			fileList.add(0, fileItemHelper.createPlayFileItem());
		}

		ListView listView = findViewById(android.R.id.list);
		listView.setAdapter(new FileItemArrayAdapter(this, fileList));
		listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				FileItem fileItem = ((FileItemViewHolder) view.getTag()).getFileItem();
				if (fileItem.getIsDirectory()) {
					currentPath = fileItem.getPath();
					updateListView();
				} else if (fileItem.getIsSpecial() || new FileItemHelper(MainActivity.this).isImage(fileItem)){
					// Only open images
					startSlideshowAt(currentPath, fileItem.getPath(), false);
				}
			}
		});
	}

	/**
	 * Begin a slideshow at the given point
	 * @param folderPath The folder location
	 * @param filePath The file path
	 */
	private void startSlideshowAt(String folderPath, String filePath, boolean autoStart){
		Log.i(TAG, String.format("Calling slideshow at %s %s", folderPath, filePath));
		Intent intent = new Intent(MainActivity.this, ImageActivity.class);
		intent.putExtra("currentPath", folderPath);
		intent.putExtra("imagePath", filePath);
		intent.putExtra("autoStart", autoStart);
		this.startActivity(intent);
	}

	/**
	 * Goes up a directory, unless at the top, then exits
	 */
	@Override
	public void onBackPressed(){
		if (currentPath.equals(rootLocation)) {
			super.onBackPressed();
		} else {
			currentPath = currentPath.substring(0, currentPath.lastIndexOf(File.separatorChar));
			updateListView();
		}
	}

	/**
	 * Permissions checker
	 */
	private boolean isStoragePermissionGranted() {
		if (Build.VERSION.SDK_INT >= 23) {
			if (checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
				Log.v(TAG,"Permission is granted");
				return true;
			} else {
				Log.v(TAG,"Permission is revoked");
				requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
				return false;
			}
		} else {
			// Permission is automatically granted on sdk<23 upon installation
			Log.v(TAG,"Permission is granted");
			return true;
		}
	}

	/**
	 * Permissions handler
	 */
	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		if (permissions.length > 0) {
			Log.v(TAG, "Permission: " + permissions[0] + " was " + grantResults[0]);
			if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
				updateListView();
			}
		}
	}

}
