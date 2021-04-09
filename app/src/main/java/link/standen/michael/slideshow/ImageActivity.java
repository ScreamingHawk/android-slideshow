package link.standen.michael.slideshow;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.content.FileProvider;
import android.support.v7.app.ActionBar;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.text.format.DateFormat;
import android.text.format.Formatter;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;

import link.standen.michael.slideshow.listener.OnSwipeTouchListener;
import link.standen.michael.slideshow.model.FileItem;
import link.standen.michael.slideshow.strategy.image.CustomImageStrategy;
import link.standen.michael.slideshow.strategy.image.GlideImageStrategy;
import link.standen.michael.slideshow.strategy.image.ImageStrategy;
import link.standen.michael.slideshow.util.FileItemHelper;

/**
 * A full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class ImageActivity extends BaseActivity implements ImageStrategy.ImageStrategyCallback {

	private static final String TAG = ImageActivity.class.getName();

	private boolean blockPreferenceReload = false;

	private SharedPreferences preferences;
	private ImageStrategy imageStrategy;

	private int imagePosition;
	private int firstImagePosition;

	private boolean isRunning = false;
	private boolean inPipMode = false;

	private static boolean STOP_ON_COMPLETE;
	private static boolean PAUSE_ON_COMPLETE;
	private static boolean REVERSE_ORDER;
	private static boolean RANDOM_ORDER;
	private static boolean REFRESH_FOLDER;
	private static int SLIDESHOW_DELAY;
	private static boolean IMAGE_DETAILS;
	private static boolean IMAGE_DETAILS_DURING;
	private static boolean SKIP_LONG_LOAD;
	private static boolean PRELOAD_IMAGES;
	private static boolean DELETE_WARNING;

	private static final int LOCATION_DETAIL_MAX_LENGTH = 35;

	private static final boolean SUPPORTS_PIP = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O;

	// Loading warnings
	private static final int LONG_LOAD_WARNING_DELAY = 5000;
	private View snackbarPlayingView;
	private View snackbarStoppedView;
	private boolean isLoading = false;
	private Snackbar loadingSnackbar = null;
	private final Handler loadingHandler = new Handler();
	private final Runnable loadingRunnable = new Runnable() {
		@Override
		public void run() {
			if (isLoading) {
				// Show snack bar with filename and option to skip
				String path = fileList.get(imagePosition).getPath();
				if (path.length() > LOCATION_DETAIL_MAX_LENGTH){
					path = "..." + path.substring(path.length() - (LOCATION_DETAIL_MAX_LENGTH - 3));
				}
				if (SKIP_LONG_LOAD && isRunning){
					// Notify and skip it
					Snackbar.make(snackbarPlayingView,
							getResources().getString(R.string.long_loading_skipping, path),
							Snackbar.LENGTH_LONG).show();
					followingImage(false);
				} else {
					// Show snackbar with option to skip
					loadingSnackbar = Snackbar.make(isRunning ? snackbarPlayingView : snackbarStoppedView,
							getResources().getString(R.string.long_loading_warning, path),
							Snackbar.LENGTH_INDEFINITE);
					loadingSnackbar.setAction(R.string.long_loading_skip_action, new View.OnClickListener() {
						@Override
						public void onClick(View view) {
							followingImage(false);
						}
					});
					loadingSnackbar.show();
				}
			}
		}
	};

	private final Handler mSlideshowHandler = new Handler();
	private final Runnable mSlideshowRunnable = new Runnable() {
		@Override
		public void run() {
			int nextPos = followingImagePosition();
			if (nextPos == firstImagePosition) {
				if (STOP_ON_COMPLETE && !inPipMode) {
					show();
				} else if (PAUSE_ON_COMPLETE) {
					stopSlideshow();
					Toast.makeText(ImageActivity.this, R.string.toast_completed, Toast.LENGTH_SHORT).show();
					return;
				}
			}
			nextImage(nextPos, false);
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
			if (IMAGE_DETAILS_DURING && !inPipMode) {
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

	private boolean userInputAllowed = true;

	@SuppressLint("ClickableViewAccessibility")
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
		mContentView = findViewById(R.id.fullscreen_content);
		mDetailsView = findViewById(R.id.image_details1); // Visible during slideshow play
		snackbarPlayingView = mContentView;
		snackbarStoppedView = findViewById(R.id.snackbar_here);

		loadPreferences();
		// Stop resume from reloading the same settings
		blockPreferenceReload = true;

		// Gesture / click detection
		mContentView.setOnTouchListener(new OnSwipeTouchListener(this) {

			@Override
			public void onClick() {
				if (checkUserInputAllowed()){
					toggle();
				}
			}

			@Override
			public void onDoubleClick() {
				if (!mVisible && checkUserInputAllowed()) {
					toggleSlideshow();
					if (isRunning) {
						Toast.makeText(ImageActivity.this, R.string.toast_resumed, Toast.LENGTH_SHORT).show();
					} else {
						Toast.makeText(ImageActivity.this, R.string.toast_paused, Toast.LENGTH_SHORT).show();
					}
				}
			}

			@Override
			public void onLongClick() {
				// Prevents grandma stopping the slideshow when dusting
				userInputAllowed = !userInputAllowed;
				if (checkUserInputAllowed()){
					Toast.makeText(ImageActivity.this, R.string.toast_input_allowed, Toast.LENGTH_SHORT).show();
				} else {
					Toast.makeText(ImageActivity.this, R.string.toast_input_blocked, Toast.LENGTH_SHORT).show();
				}
			}

			@Override
			public void onSwipeLeft() {
				if (checkUserInputAllowed()) {
					nextImage(true, false);
					startSlideshowIfFullscreen();
				}
			}

			@Override
			public void onSwipeRight() {
				if (checkUserInputAllowed()) {
					nextImage(false, false);
					startSlideshowIfFullscreen();
				}
			}

			@Override
			protected void onSwipeUp() {
				if (checkUserInputAllowed()) {
					// Swipe up starts and stops the slideshow
					toggle();
				}
			}

			@Override
			protected void onSwipeDown() {
				if (checkUserInputAllowed()) {
					// Swipe down starts and stops the slideshow
					toggle();
				}
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
		boolean autoStart = getIntent().getBooleanExtra("autoStart", false);
		Log.i(TAG, String.format("Starting slideshow at %s %s", currentPath, imagePath));
		// Save the starting values
		SharedPreferences.Editor editor = preferences.edit();
		editor.putString("remembered_location", currentPath);
		editor.putString("remembered_image", imagePath);
		editor.apply();

		// Set up image list
		fileList = new FileItemHelper(this).getFileList(currentPath, false, imagePath == null);
		if (fileList.size() == 0){
			// No files to view. Exit
			Log.i(TAG, "No files in list.");
			Toast.makeText(this, R.string.toast_no_files, Toast.LENGTH_SHORT).show();
			onBackPressed();
			return;
		}

		if (RANDOM_ORDER){
			Collections.shuffle(fileList);
		}

		if (autoStart){
			// Auto start from last image
			imagePath = preferences.getString("remembered_image_current", null);
			Log.d(TAG, String.format("Remembered start location: %s", imagePath));
		}
		// Find the selected image position
		if (imagePath == null) {
			imagePosition = 0;
			nextImage(true, true);
		} else {
			for (int i = 0; i < fileList.size(); i++) {
				if (imagePath.equals(fileList.get(i).getPath())) {
					imagePosition = i;
					break;
				}
			}
		}
		firstImagePosition = imagePosition;

		Log.v(TAG, String.format("First item is at index: %s", imagePosition));
		Log.v(TAG, String.format("File list has size of: %s", fileList.size()));

		// Show the first image
		loadImage(imagePosition, false);
	}

	/**
	 * Checks if user input is allowed and toasts if not.
	 * @return True if allowed, false otherwise
	 */
	private boolean checkUserInputAllowed(){
		if (!userInputAllowed) {
			Toast.makeText(ImageActivity.this, R.string.toast_input_blocked, Toast.LENGTH_SHORT).show();
		}
		return userInputAllowed;
	}

	@Override
	protected void onStart() {
		super.onStart();

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

		if (!SUPPORTS_PIP || !inPipMode) {
			// Stop slideshow
			show();
		}
	}

	/**
	 * Swapping between picture in picture mode starts and stops the slideshow.
	 */
	@Override
	public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode) {
		super.onPictureInPictureModeChanged(isInPictureInPictureMode);
		inPipMode = isInPictureInPictureMode;
		if (inPipMode) {
			hide();
		} else {
			show();
			// Force reloading image at full dimensions
			loadImage(imagePosition, false);
		}
	}

	/**
	 * Starts picture in picture mode
	 * @param toast If picture in picture is not supported, toast determines if a toast is displayed
	 */
	private void startPictureInPictureMode(boolean toast){
		if (SUPPORTS_PIP) {
			//noinspection deprecation
			this.enterPictureInPictureMode();
			mDetailsView.setVisibility(View.GONE);
		} else if (toast) {
			Toast.makeText(this, R.string.no_picture_in_picture, Toast.LENGTH_LONG).show();
		}
	}

	/**
	 * Set the image activity specific options menu
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.image_main, menu);
		if (!SUPPORTS_PIP){
			menu.findItem(R.id.action_picture_in_picture).setVisible(false);
		}
		return true;
	}

	/**
	 * Handle image activity specific options menu
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();

		if (id == R.id.action_picture_in_picture) {
			this.startPictureInPictureMode(true);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * Load the relevant preferences.
	 */
	private void loadPreferences(){
		preferences = PreferenceManager.getDefaultSharedPreferences(this);
		// Load preferences
		Log.d(TAG, "Loaded preferences:");
		SLIDESHOW_DELAY = (int) (Float.parseFloat(preferences.getString("slide_delay", "3")) * 1000);
		Log.d(TAG, String.format("SLIDESHOW_DELAY: %d", SLIDESHOW_DELAY));
		REVERSE_ORDER = preferences.getBoolean("reverse_order", false);
		Log.d(TAG, String.format("REVERSE_ORDER: %b", REVERSE_ORDER));
		RANDOM_ORDER = preferences.getBoolean("random_order", false);
		Log.d(TAG, String.format("RANDOM_ORDER: %b", RANDOM_ORDER));
		REFRESH_FOLDER = preferences.getBoolean("refresh_folder", false);
		Log.d(TAG, String.format("REFRESH_FOLDER: %b", REFRESH_FOLDER));
		IMAGE_DETAILS = preferences.getBoolean("image_details", false);
		Log.d(TAG, String.format("IMAGE_DETAILS: %b", IMAGE_DETAILS));
		IMAGE_DETAILS_DURING = preferences.getBoolean("image_details_during", false);
		Log.d(TAG, String.format("IMAGE_DETAILS_DURING: %b", IMAGE_DETAILS_DURING));
		SKIP_LONG_LOAD = preferences.getBoolean("skip_long_load", false);
		Log.d(TAG, String.format("SKIP_LONG_LOAD: %b", SKIP_LONG_LOAD));
		PRELOAD_IMAGES = preferences.getBoolean("preload_images", true);
		Log.d(TAG, String.format("PRELOAD_IMAGES: %b", PRELOAD_IMAGES));
		DELETE_WARNING = preferences.getBoolean("delete_warning", true);
		Log.d(TAG, String.format("DELETE_WARNING: %b", DELETE_WARNING));
		// List prefs
		int action_on_complete = Arrays.asList(getResources().getStringArray(R.array.pref_list_values_action_on_complete)).indexOf(
				preferences.getString("action_on_complete", getResources().getString(R.string.pref_default_value_action_on_complete)));
		STOP_ON_COMPLETE = action_on_complete == 1;
		Log.d(TAG, String.format("STOP_ON_COMPLETE: %b", STOP_ON_COMPLETE));
		PAUSE_ON_COMPLETE = action_on_complete == 2;
		Log.d(TAG, String.format("STOP_ON_COMPLETE: %b", PAUSE_ON_COMPLETE));

		// Show/Hide the image details that are shown during pause
		if (!IMAGE_DETAILS){
			findViewById(R.id.image_details2).setVisibility(View.GONE);
		} else {
			findViewById(R.id.image_details2).setVisibility(View.VISIBLE);
		}

		// Set the image strategy.
		if (preferences.getBoolean("glide_image_strategy", true)){
			imageStrategy = new GlideImageStrategy();
			Log.d(TAG, "Glide image strategy");
		} else {
			imageStrategy = new CustomImageStrategy();
			Log.d(TAG, "Custom image strategy");
		}
		imageStrategy.setContext(this);
		imageStrategy.setCallback(this);
		imageStrategy.loadPreferences(preferences);

	}

	/**
	 * Show the next image.
	 */
	private void nextImage(boolean forwards, boolean preload){
		nextImage(nextImagePosition(forwards), preload);
	}

	/**
	 * Show the next image.
	 */
	private void nextImage(int newPosition, boolean preload){
		if (preload && !PRELOAD_IMAGES){
			// Stop
			return;
		}

		int current = imagePosition;
		if (REFRESH_FOLDER && newPosition == 0) { // Time to reload, easy base case
			fileList = new FileItemHelper(this).getFileList(currentPath, false, getIntent().getStringExtra("imagePath") == null);
			if (RANDOM_ORDER){
				Collections.shuffle(fileList);
			}
		}

		if (newPosition == current){
			// Looped. Exit
			onBackPressed();
			return;
		}
		if (!preload){
			imagePosition = newPosition;
		}
		loadImage(newPosition, preload);
	}

	/**
	 * Show the following image.
	 * This method handles whether or not the slideshow is in reverse order.
	 */
	private void followingImage(boolean preload){
		nextImage(!REVERSE_ORDER, preload);
	}

	/**
	 * Gets the position of the next image.
	 */
	private int nextImagePosition(boolean forwards){
		int newPosition = imagePosition;

		do {
			newPosition += forwards ? 1 : -1;
			if (newPosition < 0){
				newPosition = fileList.size() - 1;
			}
			if (newPosition >= fileList.size()){
				newPosition = 0;
			}
		} while (!testPositionIsImage(newPosition) || !testPositionExists(newPosition));
		return newPosition;
	}

	/**
	 * Gets the position of the following image.
	 * This method handles whether or not the slideshow is in reverse order.
	 */
	private int followingImagePosition(){
		return nextImagePosition(!REVERSE_ORDER);
	}

	/**
	 * Tests if the current file item is an image.
	 * @return True if image, false otherwise.
     */
	private boolean testPositionIsImage(int position){
		return new FileItemHelper(this).isImage(fileList.get(position));
	}

	/**
	 * Tests if the current file item still exists.
	 * @return True if it's there, false otherwise.
     */
	private boolean testPositionExists(int position){
		return new File(fileList.get(position).getPath()).exists();
	}

	@Override
	public void clearLoadingSnackbar(){
		isLoading = false;
		if (loadingSnackbar != null){
			loadingSnackbar.dismiss();
			loadingSnackbar = null;
		}
		loadingHandler.removeCallbacks(loadingRunnable);
	}

	@Override
	public void beginLoadingSnackbar(){
		clearLoadingSnackbar();
		isLoading = true;
		loadingHandler.removeCallbacks(loadingRunnable);
		loadingHandler.postDelayed(loadingRunnable, LONG_LOAD_WARNING_DELAY);
	}

	/**
	 * Load the image to the screen.
	 */
	private void loadImage(int position, boolean preload){
		if (preload && !PRELOAD_IMAGES){
			// Stop
			return;
		}

		try {
		final FileItem item = fileList.get(position);

			if (preload) {
				imageStrategy.preload(item);
			} else {
				setTitle(item.getName());
				// Begin timer for long loading warning
				beginLoadingSnackbar();
				imageStrategy.load(item, mContentView);
			}
		} catch (NullPointerException npe) {
			Toast.makeText(this,R.string.toast_error_loading_image, Toast.LENGTH_SHORT).show();
			finish();
		}
	}

	/**
	 * Save the current image path for instant restore features.
	 */
	private void saveCurrentImagePath(){
		// Save the current image path
		if (preferences == null){
			loadPreferences();
		}
		SharedPreferences.Editor editor = preferences.edit();
		editor.putString("remembered_image_current", fileList.get(imagePosition).getPath());
		editor.apply();
	}

	@Override
	public void updateImageDetails(FileItem item) {
		// Decode dimensions
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(item.getPath(), options);

		updateImageDetails(item, options.outWidth, options.outHeight);
	}

	@Override
	public void updateImageDetails(FileItem item, int width, int height){
		File file = new File(item.getPath());

		// Location
		String location = file.getParent().replace(getRootLocation(), "");
		if (location.length() > LOCATION_DETAIL_MAX_LENGTH){
			location = "..." + location.substring(location.length() - (LOCATION_DETAIL_MAX_LENGTH - 3));
		}
		((TextView)findViewById(R.id.image_detail_location1)).setText(location);
		((TextView)findViewById(R.id.image_detail_location2)).setText(location);
		Log.d(TAG, String.format("Current image location: %s", location));
		// Dimensions
		String dimensions = getResources().getString(R.string.image_detail_dimensions, width, height);
		((TextView)findViewById(R.id.image_detail_dimensions1)).setText(dimensions);
		((TextView)findViewById(R.id.image_detail_dimensions2)).setText(dimensions);
		Log.d(TAG, String.format("Current image dimensions: %s", dimensions));
		// Size
		String size = getResources().getString(R.string.image_detail_size,
				Formatter.formatShortFileSize(this, file.length()));
		((TextView)findViewById(R.id.image_detail_size1)).setText(size);
		((TextView)findViewById(R.id.image_detail_size2)).setText(size);
		Log.d(TAG, String.format("Current image size: %s", size));
		// Modified
		String modified = getResources().getString(R.string.image_detail_modified,
				DateFormat.getMediumDateFormat(this).format(file.lastModified()));
		((TextView)findViewById(R.id.image_detail_modified1)).setText(modified);
		((TextView)findViewById(R.id.image_detail_modified2)).setText(modified);
		Log.d(TAG, String.format("Current image modified: %s", modified));

		// Save this spot
		saveCurrentImagePath();
	}

	/**
	 * Delete the current image with a warning pop up if required
	 */
	private void deleteImage(){
		if (DELETE_WARNING) {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(R.string.delete_dialog_title);
			builder.setMessage(R.string.delete_dialog_message);
			builder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
					doDelete();
				}
			});
			builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
				}
			});
			builder.create().show();
		} else {
			doDelete();
		}
	}

	/**
	 * Delete the current image
	 */
	private void doDelete(){
		FileItem item = fileList.get(imagePosition);
		if (new File(item.getPath()).delete()) {
			fileList.remove(item);
			Toast.makeText(ImageActivity.this, R.string.image_deleted, Toast.LENGTH_SHORT).show();
			// Show next image
			imagePosition = imagePosition + (REVERSE_ORDER ? 1 : -1);
			followingImage(false);
		} else {
			Toast.makeText(ImageActivity.this, R.string.image_not_deleted, Toast.LENGTH_SHORT).show();
		}
	}

	/**
	 * Share the current image
	 */
	private void shareImage(){
		FileItem item = fileList.get(imagePosition);
		Intent intent = new Intent(Intent.ACTION_SEND);
		String mime = new FileItemHelper(this).getImageMimeType(item);
		intent.setType(mime);
		intent.putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(this,
				getApplicationContext().getPackageName() + ".provider",
				new File(item.getPath())));
		intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
		Log.d(TAG, String.format("Sharing image at %s of type %s", item.getPath(), mime));
		startActivity(Intent.createChooser(intent, getResources().getString(R.string.share_via)));
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);

		// Trigger the initial hide() shortly after the activity has been created, to briefly hint
		// to the user that UI controls are available.
		delayedHide();
	}

	/**
	 * Pause or play the slideshow.
	 */
	private void toggleSlideshow(){
		if (isRunning) {
			stopSlideshow();
		} else {
			startSlideshowIfFullscreen();
		}
	}

	/**
	 * Stop or start the slideshow.
	 */
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
	 * Schedules a call to hide() in 100 milliseconds, canceling any previously scheduled calls.
	 */
	private void delayedHide() {
		mHideHandler.removeCallbacks(mHideRunnable);
		mHideHandler.postDelayed(mHideRunnable, 100);
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
		isRunning = true;
		mSlideshowHandler.removeCallbacks(mSlideshowRunnable);
		queueSlide();
	}

	/**
	 * Queue the next slide in the slideshow
	 */
	@Override
	public void queueSlide(){
		queueSlide(SLIDESHOW_DELAY);
	}

	/**
	 * Queue the next slide in the slideshow
	 */
	@Override
	public void queueSlide(int delayMillis){
		if (delayMillis < SLIDESHOW_DELAY){
			delayMillis = SLIDESHOW_DELAY;
		}
		if (isRunning) {
			// Ensure only one runnable is in the queue
			mSlideshowHandler.removeCallbacks(mSlideshowRunnable);
			mSlideshowHandler.postDelayed(mSlideshowRunnable, delayMillis);
			// Preload the next image
			followingImage(true);
		}
	}

	/**
	 * Stops the slideshow
	 */
	private void stopSlideshow(){
		isRunning = false;
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
