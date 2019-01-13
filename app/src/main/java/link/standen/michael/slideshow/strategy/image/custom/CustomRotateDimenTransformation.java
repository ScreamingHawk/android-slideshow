package link.standen.michael.slideshow.strategy.image.custom;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.support.media.ExifInterface;
import android.util.Log;

import java.io.IOException;

public class CustomRotateDimenTransformation {

	private static final String TAG = CustomRotateDimenTransformation.class.getName();

	// see https://www.daveperrett.com/articles/2012/07/28/exif-orientation-handling-is-a-ghetto/
	private static final int[] EXIF_ORIENTATION_TO_ROTATION = new int[]{-1, 0, 0, 180, 180, 90, 90, 270, 270};

	/**
	 * Calculates the necessary degrees by which the image needs to be rotated in order to be displayed correctly according to the EXIf information.
	 *
	 * Note: image flipping is not supported, although part of the same EXIF tag
	 *
	 * @return the degrees to rotate
	 */
	public static int getRotationFromExif(String filename) {
		try {
			ExifInterface exif = new ExifInterface(filename);
			int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);
			Log.d(TAG, "File " + filename + " has EXIF orientation " + orientation);

			return EXIF_ORIENTATION_TO_ROTATION[orientation];
		} catch (IOException e) {
			Log.e(TAG, "EXIF data for file " + filename + " failed to load.");
			return -1;
		}
	}

	/**
	 * Calculates the necessary degrees by which the image needs to be rotated in order to fill the full screen in landscape mode
	 *
	 * @return the degrees to rotate
	 */
	public static int getRotationFromDimensions(Bitmap image) {
		if (image.getWidth() > image.getHeight()) {
			return 90;
		} else {
			return 0;
		}
	}

	/**
	 * Rotates the given image by the given degrees
	 *
	 * @return the rotated image
	 */
	public static Bitmap rotate(Bitmap image, int degrees) {
		// Rotate the image if it is landscape
		Matrix matrix = new Matrix();
		matrix.postRotate(degrees);
		return Bitmap.createBitmap(image, 0, 0, image.getWidth(), image.getHeight(), matrix, true);
	}

	/**
	 * Return true if the rotation for the given degeers swaps the coordinates of the image, false otherwise.
	 */
	public static boolean isCoordinatesSwapped(int degrees) {
		return (degrees == 90) || (degrees == 270);
	}
}
