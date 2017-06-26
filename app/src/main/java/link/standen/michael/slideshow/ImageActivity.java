package link.standen.michael.slideshow;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.content.FileProvider;
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

import javax.microedition.khronos.opengles.GL11;

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

	private static final int LOCATION_DETAIL_MAX_LENGTH = 35;

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
		mDetailsView = findViewById(R.id.image_details1); // Visible during slideshow play

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
		fileList = new FileItemHelper(this).getFileList(currentPath, false, imagePath == null);
		if (RANDOM_ORDER){
			Collections.shuffle(fileList);
		}

		// Find the selected image position
		if (imagePath == null) {
			imagePosition = 0;
			nextImage();
		} else {
			for (int i = 0; i < fileList.size(); i++) {
				if (imagePath.equals(fileList.get(i).getPath())) {
					imagePosition = i;
					break;
				}
			}
		}
		firstImagePosition = imagePosition;

		Log.v(TAG, "First item is at index: "+imagePosition);
		Log.v(TAG, "File list has size of: "+fileList.size());

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

		// Show/Hide the image details that are show during pause
		if (!IMAGE_DETAILS){
			findViewById(R.id.image_details2).setVisibility(View.GONE);
		} else {
			findViewById(R.id.image_details2).setVisibility(View.VISIBLE);
		}
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

		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(item.getPath(), options);

		int sampleSize = 1;
		int width = options.outWidth;
		int height = options.outHeight;

		/*
		 * Downscale strategy taken from:
		 * https://android.googlesource.com/platform/packages/apps/Camera2/src/com/android/camera/data/FilmstripItemUtils.java
		 *
		 * For large (> MAXIMUM_TEXTURE_SIZE) high aspect ratio (panorama)
		 * Bitmap requests:
		 *   Step 1: ask for double size.
		 *   Step 2: scale maximum edge down to MAXIMUM_TEXTURE_SIZE.
		 *
		 * Here's the step 1: double size.
		 */
		if (width > GL11.GL_MAX_TEXTURE_SIZE || height > GL11.GL_MAX_TEXTURE_SIZE) {
			sampleSize = 2;
		}

		options = new BitmapFactory.Options();
		options.inSampleSize = sampleSize;
		/* 32K buffer. */
		options.inTempStorage = new byte[32 * 1024];

		// Load image
		Bitmap image = BitmapFactory.decodeFile(item.getPath(), options);

		/*
		 * Step 2: scale maximum edge down to maximum texture size.
		 * If Bitmap maximum edge > MAXIMUM_TEXTURE_SIZE, which can happen for panoramas,
		 * scale to fit in MAXIMUM_TEXTURE_SIZE.
		 */
		if (image.getWidth() > GL11.GL_MAX_TEXTURE_SIZE || image.getHeight() > GL11.GL_MAX_TEXTURE_SIZE){
			// Scale down
			int maxEdge = Math.max(width, height);
			image = Bitmap.createScaledBitmap(image, width * GL11.GL_MAX_TEXTURE_SIZE / maxEdge,
					height * GL11.GL_MAX_TEXTURE_SIZE / maxEdge, false);
		}

		mContentView.setImageBitmap(image);

		updateImageDetails(item, width, height);
	}

	/**
	 * Update the image details
	 */
	private void updateImageDetails(FileItem item, int width, int height){
		// Update image details
		File file = new File(item.getPath());
		// Location
		String location = item.getPath().replace(getRootLocation(), "");
		if (location.length() > LOCATION_DETAIL_MAX_LENGTH){
			location = "..." + location.substring(location.length() - (LOCATION_DETAIL_MAX_LENGTH - 3));
		}
		((TextView)findViewById(R.id.image_detail_location1)).setText(location);
		((TextView)findViewById(R.id.image_detail_location2)).setText(location);
		// Dimensions
		String dimensions = getResources().getString(R.string.image_detail_dimensions, width, height);
		((TextView)findViewById(R.id.image_detail_dimensions1)).setText(dimensions);
		((TextView)findViewById(R.id.image_detail_dimensions2)).setText(dimensions);
		// Size
		String size = getResources().getString(R.string.image_detail_size,
				Formatter.formatShortFileSize(this, file.length()));
		((TextView)findViewById(R.id.image_detail_size1)).setText(size);
		((TextView)findViewById(R.id.image_detail_size2)).setText(size);
		// Modified
		String modified = getResources().getString(R.string.image_detail_modified,
				DateFormat.getDateFormat(this).format(file.lastModified()));
		((TextView)findViewById(R.id.image_detail_modified1)).setText(modified);
		((TextView)findViewById(R.id.image_detail_modified2)).setText(modified);
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
		intent.putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(this,
				getApplicationContext().getPackageName() + ".provider",
				new File(item.getPath())));
		intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
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
