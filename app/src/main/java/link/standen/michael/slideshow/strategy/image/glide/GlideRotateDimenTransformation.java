package link.standen.michael.slideshow.strategy.image.glide;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation;
import com.bumptech.glide.load.resource.bitmap.TransformationUtils;

/**
 * Rotates an image if the image is wider than it is high.
 * Note: This class uses {@link com.bumptech.glide.load.resource.bitmap.TransformationUtils#fitCenter(Bitmap, BitmapPool, int, int)} to reduce computation.
 */
public class GlideRotateDimenTransformation extends BitmapTransformation {

	private static final String TAG = GlideRotateDimenTransformation.class.getName();

	public GlideRotateDimenTransformation(Context context) {
		super(context);
	}

	@Override
	protected Bitmap transform(BitmapPool pool, Bitmap toTransform, int outWidth, int outHeight) {
		Log.d(TAG, String.format("Height: %d Width: %d", toTransform.getHeight(), toTransform.getWidth()));
		if (toTransform.getHeight() >= toTransform.getWidth()){
			// Perform fit center here on un-rotated image.
			toTransform = TransformationUtils.fitCenter(toTransform, pool, outWidth, outHeight);
			return toTransform;
		}
		// Fit center using largest side (width) for both to reduce computation for rotate
		//noinspection SuspiciousNameCombination
		toTransform = TransformationUtils.fitCenter(toTransform, pool, outWidth, outWidth);
		return TransformationUtils.rotateImage(toTransform, 90);
	}

	@Override
	public String getId() {
		return TAG;
	}
}
