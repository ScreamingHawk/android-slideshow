package link.standen.michael.slideshow;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ListView;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import link.standen.michael.slideshow.adapter.FileItemArrayAdapter;
import link.standen.michael.slideshow.model.FileItem;
import link.standen.michael.slideshow.model.FileItemViewHolder;

/**
 * Slideshow main activity.
 */
public class MainActivity extends AppCompatActivity {

	private static final String TAG = "MainActivity";

	private String absPath = Environment.getExternalStorageDirectory().getAbsolutePath();
	private String currentPath;
	private ListView listView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

		// Get path
		currentPath = absPath;
		if (getIntent().hasExtra("path")){
			currentPath = getIntent().getStringExtra("path");
		}

		// Permission check
		if (isStoragePermissionGranted()){
			updateFileList();
		}
		// else wait for permission handler to continue
	}

	private void updateFileList(){
		Log.d(TAG, "currentpath: "+currentPath);

		// Set title
		setTitle(currentPath.replace(absPath, ""));

		// Create file list
		final List<FileItem> fileList = new ArrayList<>();
		File dir = new File(currentPath);
		if (!dir.canRead()){
			setTitle(getTitle() + getResources().getString(R.string.inaccessible));
		}
		File[] files = dir.listFiles();
		if (files != null){
			for (File file : files){
				//TODO Thumbnail
				FileItem item = new FileItem();
				item.setName(file.getName());
				item.setPath(file.getAbsolutePath());
				item.setIsDirectory(file.isDirectory());
				if (!item.getIsDirectory()){
					item.setThumbnail(ThumbnailUtils.extractThumbnail(
							BitmapFactory.decodeFile(item.getPath()),
							(int)getResources().getDimension(R.dimen.file_image_width),
							(int)getResources().getDimension(R.dimen.file_image_height)));
				}
				fileList.add(item);
			}
		}
		Collections.sort(fileList);

		listView = (ListView) findViewById(android.R.id.list);
		listView.setAdapter(new FileItemArrayAdapter(this, R.layout.file_item, fileList));
		listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				FileItem fileItem = ((FileItemViewHolder) view.getTag()).getFileItem();
				if (fileItem.getIsDirectory()){
					currentPath = fileItem.getPath();
					updateFileList();
				} else {
					//TODO Show image
					Intent intent = new Intent(MainActivity.this, ImageActivity.class);
					intent.putExtra("image", fileItem.getPath());
					MainActivity.this.startActivity(intent);
				}
			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.menu_main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();

		//noinspection SimplifiableIfStatement
		if (id == R.id.action_settings) {
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	/**
	 * Goes up a directory, unless at the top, then exits
	 */
	@Override
	public void onBackPressed(){
		if (currentPath.equals(absPath)) {
			super.onBackPressed();
		} else {
			currentPath = currentPath.substring(0, currentPath.lastIndexOf(File.separatorChar));
			updateFileList();
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
			updateFileList();
		}
	}

}
