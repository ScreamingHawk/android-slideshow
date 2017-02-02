package link.standen.michael.slideshow;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
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

	private static final String TAG = "MainActivity";

	private ListView listView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

		// Get path
		currentPath = FileItemHelper.absPath;
		if (getIntent().hasExtra("path")){
			currentPath = getIntent().getStringExtra("path");
		}
	}

	@Override
	protected void onStart() {
		super.onStart();

		// Permission check
		if (isStoragePermissionGranted()){
			updateListView();
		}
		// else wait for permission handler to continue
	}

	private void updateListView(){
		fileList = new FileItemHelper().getFileList(currentPath, this);

		listView = (ListView) findViewById(android.R.id.list);
		listView.setAdapter(new FileItemArrayAdapter(this, R.layout.file_item, fileList));
		listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				FileItem fileItem = ((FileItemViewHolder) view.getTag()).getFileItem();
				if (fileItem.getIsDirectory()){
					currentPath = fileItem.getPath();
					updateListView();
				} else {
					if (!fileItem.getThumbnailAttempted()){
						// Load thumbnail
						new FileItemHelper().loadThumbnail(fileItem, MainActivity.this);
					}
					if (fileItem.getThumbnail() != null) {
						// Only open images
						Intent intent = new Intent(MainActivity.this, ImageActivity.class);
						intent.putExtra("currentPath", currentPath);
						intent.putExtra("imagePosition", fileList.indexOf(fileItem));
						MainActivity.this.startActivity(intent);
					}
				}
			}
		});
	}

	/**
	 * Goes up a directory, unless at the top, then exits
	 */
	@Override
	public void onBackPressed(){
		if (currentPath.equals(FileItemHelper.absPath)) {
			super.onBackPressed();
		} else {
			currentPath = currentPath.substring(0, currentPath.lastIndexOf(File.separatorChar));
			updateListView();
		}
	}

	/**
	 * Permissions checker
	 */
	public boolean isStoragePermissionGranted() {
		if (Build.VERSION.SDK_INT >= 23) {
			if (checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
				Log.v(TAG,"Permission is granted");
				return true;
			} else {
				Log.v(TAG,"Permission is revoked");
				requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
				return false;
			}
		} else { //permission is automatically granted on sdk<23 upon installation
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
		Log.v(TAG,"Permission: " + permissions[0] + " was " + grantResults[0]);
		if (grantResults[0] == PackageManager.PERMISSION_GRANTED){
			updateListView();
		}
	}

}
