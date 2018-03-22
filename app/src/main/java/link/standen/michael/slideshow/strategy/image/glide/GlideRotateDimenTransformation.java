package link.standen.michael.slideshow.strategy.image.glide;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation;
import com.bumptech.glide.load.resource.bitmap.TransformationUtils;

/**
 * Rotates an image if the image is wider than it is high.
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
			return toTransform;
		}
		return TransformationUtils.rotateImage(toTransform, 90);
	}

	@Override
	public String getId() {
		return TAG;
	}
}
