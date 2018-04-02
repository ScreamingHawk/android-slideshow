package link.standen.michael.slideshow.strategy.image.custom;

import android.graphics.Bitmap;
import android.graphics.Matrix;

public class CustomRotateDimenTransformation {

	private boolean rotated = false;

	/**
	 * Rotate the image 90 degrees.
	 */
	public Bitmap rotate(Bitmap image) {
		rotated = image.getWidth() > image.getHeight();
		if (rotated) {
			// Rotate the image if it is landscape
			Matrix matrix = new Matrix();
			matrix.postRotate(90);
			return Bitmap.createBitmap(image, 0, 0, image.getWidth(), image.getHeight(), matrix, true);
		}
		return image;
	}

	/**
	 * Return true if the last call rotated the image, false otherwise.
	 */
	public boolean wasRotated(){
		return rotated;
	}
}
