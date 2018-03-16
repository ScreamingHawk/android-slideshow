package link.standen.michael.slideshow.strategy.image;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.util.Log;
import android.widget.ImageView;

import com.bumptech.glide.DrawableTypeRequest;
import com.bumptech.glide.Glide;
import com.bumptech.glide.gifdecoder.GifDecoder;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.load.resource.gif.GifDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

import link.standen.michael.slideshow.R;
import link.standen.michael.slideshow.model.FileItem;

public class GlideImageStrategy implements ImageStrategy {

    private static final String TAG = GlideImageStrategy.class.getName();

	private Context context;
	private ImageStrategyCallback callback;

	private static boolean PLAY_GIF;

	@Override
	public void setContext(Context context) {
		this.context = context;
	}

	@Override
	public void setCallback(ImageStrategyCallback callback) {
		this.callback = callback;
	}

	@Override
	public void preload(final FileItem item) {
		final DrawableTypeRequest<String> glideLoad = Glide
				.with(context)
				.load(item.getPath());
		if (PLAY_GIF) {
			// Play GIFs
			glideLoad.preload();
		} else {
			// Force bitmap so GIFs don't play
			glideLoad.asBitmap().preload();
		}
	}

	@Override
	public void load(final FileItem item, final ImageView view) {
		final DrawableTypeRequest<String> glideLoad = Glide
				.with(context)
				.load(item.getPath());
		if (PLAY_GIF) {
			// Play GIFs
			glideLoad
					.diskCacheStrategy(DiskCacheStrategy.SOURCE)
					.placeholder(view.getDrawable())
					.fitCenter()
					.dontAnimate()
					.listener(new RequestListener<String, GlideDrawable>() {
						@Override
						public boolean onException(Exception e, String s, Target<GlideDrawable> target, boolean b) {
							Log.e(TAG, "Error loading image", e);
							callback.clearLoadingSnackbar();
							return false;
						}

						@Override
						public boolean onResourceReady(GlideDrawable glideDrawable, String s, Target<GlideDrawable> target, boolean b, boolean b1) {
							callback.clearLoadingSnackbar();
							if (glideDrawable instanceof GifDrawable) {
								// Queue the next slide after the animation completes
								GifDrawable gifDrawable = (GifDrawable) glideDrawable;

								int duration = 250; // Start with a little extra time
								GifDecoder decoder = gifDrawable.getDecoder();
								for (int i = 0; i < gifDrawable.getFrameCount(); i++) {
									duration += decoder.getDelay(i);
								}

								callback.queueSlide(duration);
							} else {
								callback.queueSlide();
							}

							// Update image details
							callback.updateImageDetails(item);
							return false;
						}
					})
					.into(view);
		} else {
			// Force bitmap so GIFs don't play
			glideLoad
					.asBitmap()
					.placeholder(view.getDrawable())
					.fitCenter()
					.dontAnimate()
					.error(R.color.image_background)
					.listener(new RequestListener<String, Bitmap>() {
						@Override
						public boolean onException(Exception e, String s, Target<Bitmap> target, boolean b) {
							Log.e(TAG, "Error loading image", e);
							callback.clearLoadingSnackbar();
							callback.queueSlide();
							callback.updateImageDetails(item);
							return false;
						}

						@Override
						public boolean onResourceReady(Bitmap bitmap, String s, Target<Bitmap> target, boolean b, boolean b1) {
							callback.clearLoadingSnackbar();
							callback.queueSlide();
							callback.updateImageDetails(item);

							return false;
						}
					})
					.into(view);
		}
	}

	@Override
	public void loadPreferences(SharedPreferences preferences) {
		PLAY_GIF = preferences.getBoolean("enable_gif_support", true);
	}
}
