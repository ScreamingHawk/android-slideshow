package link.standen.michael.slideshow.listener;

import android.content.Context;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

/**
 * Monitors for swipe events.
 * http://stackoverflow.com/a/12938787
 */
public abstract class OnSwipeTouchListener implements View.OnTouchListener {

	private final GestureDetector gestureDetector;

	protected OnSwipeTouchListener (Context ctx){
		gestureDetector = new GestureDetector(ctx, new GestureListener());
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		return gestureDetector.onTouchEvent(event);
	}

	private final class GestureListener extends GestureDetector.SimpleOnGestureListener {

		private static final int SWIPE_THRESHOLD = 100;
		private static final int SWIPE_VELOCITY_THRESHOLD = 100;

		@Override
		public boolean onDown(MotionEvent e) {
			return true;
		}

		@Override
		public boolean onSingleTapConfirmed(MotionEvent e) {
			onClick();
			return super.onSingleTapConfirmed(e);
		}

		@Override
		public boolean onDoubleTap(MotionEvent e){
			onDoubleClick();
			return super.onDoubleTap(e);
		}

		@Override
		public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
			boolean result = false;
			try {
				float diffY = e2.getY() - e1.getY();
				float diffX = e2.getX() - e1.getX();
				if (Math.abs(diffX) > Math.abs(diffY)) {
					if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
						if (diffX > 0) {
							onSwipeRight();
						} else {
							onSwipeLeft();
						}
					}
					result = true;
				}
				else if (Math.abs(diffY) > SWIPE_THRESHOLD && Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
					if (diffY > 0) {
						onSwipeDown();
					} else {
						onSwipeUp();
					}
				}
				result = true;

			} catch (Exception exception) {
				exception.printStackTrace();
			}
			return result;
		}
	}

	/**
	 * Implement this method to handle swipe right events
	 */
	protected abstract void onSwipeRight();

	/**
	 * Implement this method to handle swipe left events
	 */
	protected abstract void onSwipeLeft();

	/**
	 * Implement this method to handle swipe up events
	 */
	protected abstract void onSwipeUp();

	/**
	 * Implement this method to handle swipe down events
	 */
	protected abstract void onSwipeDown();

	/**
	 * Implement this method to handle click events
	 */
	protected abstract void onClick();

	/**
	 * Implement this method to handle double click events
	 */
	protected abstract void onDoubleClick();
}
