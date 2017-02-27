package link.standen.michael.slideshow;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBar;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.text.format.DateFormat;
import android.text.format.Formatter;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.Collections;

import link.standen.michael.slideshow.listener.OnSwipeTouchListener;
import link.standen.michael.slideshow.model.FileItem;
import link.standen.michael.slideshow.util.FileItemHelper;

/**
 * A full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class ImageActivity extends BaseActivity {

	private static final String TAG = ImageActivity.class.getName();

	private boolean blockPreferenceReload = false;

	private int imagePosition;
	private int firstImagePosition;

	private static boolean STOP_ON_COMPLETE;
	private static boolean REVERSE_ORDER;
	private static boolean RANDOM_ORDER;
	private static int SLIDESHOW_DELAY;
	private static boolean IMAGE_DETAILS;

	private final Handler mSlideshowHandler = new Handler();
	private final Runnable mSlideshowRunnable = new Runnable() {
		@Override
		public void run() {
			followingImage();
			if (!(STOP_ON_COMPLETE && imagePosition == firstImagePosition)) {
				queueSlide();
			} else {
				show();
			}
		}
	};

	/**
	 * Some older devices needs a small delay between UI widget updates
	 * and a change of the status and navigation bar.
	 */
	private static final int UI_ANIMATION_DELAY = 300;
	private final Handler mHideHandler = new Handler();
	private ImageView mContentView;
	private final Runnable mHidePart2Runnable = new Runnable() {
		@SuppressLint("InlinedApi")
		@Override
		public void run() {
			// Delayed removal of status and navigation bar

			// Note that some of these constants are new as of API 16 (Jelly Bean)
			// and API 19 (KitKat). It is safe to use them, as they are inlined
			// at compile-time and do nothing on earlier devices.
			mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
					| View.SYSTEM_UI_FLAG_FULLSCREEN
					| View.SYSTEM_UI_FLAG_LAYOUT_STABLE
					| View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
					| View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
					| View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
			if (IMAGE_DETAILS) {
				mDetailsView.setVisibility(View.VISIBLE);
			}

			// Start slideshow
			startSlideshow();
		}
	};
	private View mDetailsView;
	private View mControlsView;
	private final Runnable mShowPart2Runnable = new Runnable() {
		@Override
		public void run() {
			// Delayed display of UI elements
			ActionBar actionBar = getSupportActionBar();
			if (actionBar != null) {
				actionBar.show();
			}
			mControlsView.setVisibility(View.VISIBLE);
		}
	};
	private boolean mVisible;
	private final Runnable mHideRunnable = new Runnable() {
		@Override
		public void run() {
			hide();
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_image);
		ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			actionBar.setDisplayHomeAsUpEnabled(true);
		}

		mVisible = true;
		mControlsView = findViewById(R.id.fullscreen_content_controls);
		mContentView = (ImageView) findViewById(R.id.fullscreen_content);
		mDetailsView = findViewById(R.id.image_details);

		loadPreferences();
		// Stop resume from reloading the same settings
		blockPreferenceReload = true;

		// Gesture / click detection
		mContentView.setOnTouchListener(new OnSwipeTouchListener(this) {
			@Override
			public void onClick() {
				toggle();
			}

			@Override
			public void onSwipeLeft() {
				nextImage();
				startSlideshowIfFullscreen();
			}

			@Override
			public void onSwipeRight() {
				previousImage();
				startSlideshowIfFullscreen();
			}
		});

		// Configure delete button
		findViewById(R.id.delete_button).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (isStoragePermissionGranted()) {
					deleteImage();
				}
			}
		});

		// Configure the share button
		findViewById(R.id.share_button).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				shareImage();
			}
		});

		// Get starting values
		currentPath = getIntent().getStringExtra("currentPath");
		String imagePath = getIntent().getStringExtra("imagePath");

		// Set up image list
		fileList = new FileItemHelper(this).getFileList(currentPath, false);
		if (RANDOM_ORDER){
			Collections.shuffle(fileList);
		}

		// Find the selected image position
		for (int i = 0; i < fileList.size(); i++){
			if (imagePath.equals(fileList.get(i).getPath())){
				imagePosition = i;
				break;
			}
		}
		firstImagePosition = imagePosition;

		// Show the first image
		loadImage();
	}

	@Override
	protected void onResume() {
		super.onResume();

		// Only reload the settings if not blocked by onCreate
		if (blockPreferenceReload){
			blockPreferenceReload = false;
		} else {
			loadPreferences();
		}
		// Start slideshow if no UI
		startSlideshowIfFullscreen();
	}

	@Override
	protected void onPause(){
		super.onPause();

		// Stop slideshow
		stopSlideshow();
	}

	/**
	 * Load the relevant preferences.
	 */
	private void loadPreferences(){
		// Load preferences
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
		SLIDESHOW_DELAY = (int) Float.parseFloat(preferences.getString("slide_delay", "3")) * 1000;
		STOP_ON_COMPLETE = preferences.getBoolean("stop_on_complete", false);
		REVERSE_ORDER = preferences.getBoolean("reverse_order", false);
		RANDOM_ORDER = preferences.getBoolean("random_order", false);
		IMAGE_DETAILS = preferences.getBoolean("image_details", false);
	}

	/**
	 * Show the next image.
	 */
	private void nextImage(){
		int current = imagePosition;
		do {
			imagePosition++;
			if (imagePosition >= fileList.size()){
				imagePosition = 0;
			}
			if (imagePosition == current){
				// Looped. Exit
				onBackPressed();
				return;
			}
		} while (!testCurrentIsImage());
		loadImage();
	}

	/**
	 * Show the previous image.
	 */
	private void previousImage(){
		int current = imagePosition;
		do {
			imagePosition--;
			if (imagePosition < 0){
				imagePosition = fileList.size() - 1;
			}
			if (imagePosition == current){
				// Looped. Exit
				onBackPressed();
				return;
			}
		} while (!testCurrentIsImage());
		loadImage();
	}

	/**
	 * Show the following image.
	 * This method handles whether or not the slideshow is in reverse order.
	 */
	private void followingImage(){
		if (REVERSE_ORDER) {
			previousImage();
		} else {
			nextImage();
		}
	}

	/**
	 * Tests if the current file item is an image.
	 * @return True if image, false otherwise.
     */
	private boolean testCurrentIsImage(){
		return new FileItemHelper(this).isImage(fileList.get(imagePosition));
	}

	private void loadImage(){
		FileItem item = fileList.get(imagePosition);
		setTitle(item.getName());
		Bitmap image = BitmapFactory.decodeFile(item.getPath());
		mContentView.setImageBitmap(image);

		if (IMAGE_DETAILS) {
			// Update image details
			File file = new File(item.getPath());
			// Dimensions
			((TextView)findViewById(R.id.image_detail_dimensions)).setText(getResources().getString(
					R.string.image_detail_dimensions, image.getWidth(), image.getHeight()));
			// Size
			String size = Formatter.formatShortFileSize(this, file.length());
			((TextView)findViewById(R.id.image_detail_size)).setText(getResources().getString(
					R.string.image_detail_size, size));
			// Modified
			((TextView)findViewById(R.id.image_detail_modified)).setText(getResources().getString(
					R.string.image_detail_modified,
					DateFormat.getDateFormat(this).format(file.lastModified())));
		}
	}

	/**
	 * Delete the current image
	 */
	private void deleteImage(){
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.delete_dialog_title);
		builder.setMessage(R.string.delete_dialog_message);
		builder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
				FileItem item = fileList.get(imagePosition);
				if (new File(item.getPath()).delete()) {
					fileList.remove(item);
					Toast.makeText(ImageActivity.this, R.string.image_deleted, Toast.LENGTH_SHORT).show();
					// Show next image
					imagePosition = imagePosition + (REVERSE_ORDER ? 1 : -1);
					followingImage();
				} else {
					Toast.makeText(ImageActivity.this, R.string.image_not_deleted, Toast.LENGTH_SHORT).show();
				}
			}
		});
		builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});
		builder.create().show();
	}

	/**
	 * Share the current image
	 */
	private void shareImage(){
		FileItem item = fileList.get(imagePosition);
		Intent intent = new Intent(Intent.ACTION_SEND);
		intent.setType(new FileItemHelper(this).getImageMimeType(item));
		intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(new File(item.getPath())));
		startActivity(Intent.createChooser(intent, getResources().getString(R.string.share_via)));
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);

		// Trigger the initial hide() shortly after the activity has been created, to briefly hint
		// to the user that UI controls are available.
		delayedHide(100);
	}

	private void toggle() {
		if (mVisible) {
			hide();
		} else {
			show();
		}
	}

	private void hide() {
		// Hide UI first
		ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			actionBar.hide();
		}
		mControlsView.setVisibility(View.GONE);
		mVisible = false;

		// Schedule a runnable to remove the status and navigation bar after a delay
		mHideHandler.removeCallbacks(mShowPart2Runnable);
		mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY);
	}

	private void show() {
		// Stop slideshow
		stopSlideshow();

		// Show the system bar
		mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
				| View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
		mDetailsView.setVisibility(View.GONE);
		mVisible = true;

		// Schedule a runnable to display UI elements after a delay
		mHideHandler.removeCallbacks(mHidePart2Runnable);
		mHideHandler.postDelayed(mShowPart2Runnable, UI_ANIMATION_DELAY);
	}

	/**
	 * Schedules a call to hide() in [delayMillis] milliseconds, canceling any
	 * previously scheduled calls.
	 */
	private void delayedHide(int delayMillis) {
		mHideHandler.removeCallbacks(mHideRunnable);
		mHideHandler.postDelayed(mHideRunnable, delayMillis);
	}

	/**
	 * Starts or restarts the slideshow if the view is in fullscreen mode.
	 */
	private void startSlideshowIfFullscreen(){
		if (!mVisible){
			startSlideshow();
		}
	}

	/**
	 * Starts or restarts the slideshow
	 */
	private void startSlideshow(){
		mSlideshowHandler.removeCallbacks(mSlideshowRunnable);
		queueSlide();
	}

	/**
	 * Queue the next slide in the slideshow
	 */
	private void queueSlide(){
		mSlideshowHandler.postDelayed(mSlideshowRunnable, SLIDESHOW_DELAY);
	}

	/**
	 * Stops the slideshow
	 */
	private void stopSlideshow(){
		mSlideshowHandler.removeCallbacks(mSlideshowRunnable);
	}

	/**
	 * Permissions checker
	 */
	private boolean isStoragePermissionGranted() {
		if (Build.VERSION.SDK_INT >= 23) {
			if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
				Log.v(TAG,"Permission is granted");
				return true;
			} else {
				Log.v(TAG,"Permission is revoked");
				requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
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
			deleteImage();
		}
	}
}
