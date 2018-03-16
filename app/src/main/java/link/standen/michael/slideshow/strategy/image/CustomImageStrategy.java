package link.standen.michael.slideshow.strategy.image;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.widget.ImageView;

import javax.microedition.khronos.opengles.GL11;

import link.standen.michael.slideshow.model.FileItem;

/**
 * A strategy for loading images that was taken from Google's Camera2 app.
 * This was the original implementation before Glide was added.
 */
public class CustomImageStrategy implements ImageStrategy {

	private static final String TAG = CustomImageStrategy.class.getName();

	private ImageStrategyCallback callback;

	@Override
	public void setContext(Context context) {
		// Context not needed
	}

	@Override
	public void setCallback(ImageStrategyCallback callback) {
		this.callback = callback;
	}

	@Override
	public void preload(FileItem item) {
		// This implementation does not support preloading
	}

	@Override
	public void load(FileItem item, ImageView view) {
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

		if( image == null ) {
			Log.e(TAG, "Error loading image");
		}
		else {
			/*
			 * Step 2: scale maximum edge down to maximum texture size.
			 * If Bitmap maximum edge > MAXIMUM_TEXTURE_SIZE, which can happen for panoramas,
			 * scale to fit in MAXIMUM_TEXTURE_SIZE.
			 */
			if (image.getWidth() > GL11.GL_MAX_TEXTURE_SIZE || image.getHeight() > GL11.GL_MAX_TEXTURE_SIZE) {
				// Scale down
				int maxEdge = Math.max(width, height);
				image = Bitmap.createScaledBitmap(image, width * GL11.GL_MAX_TEXTURE_SIZE / maxEdge,
						height * GL11.GL_MAX_TEXTURE_SIZE / maxEdge, false);

			}
		}
		view.setImageBitmap(image);

		// Callback
		callback.clearLoadingSnackbar();
		callback.queueSlide();
		callback.updateImageDetails(item, width, height);
	}

	@Override
	public void loadPreferences(SharedPreferences preferences) {
		// No preferences to load
	}
}
