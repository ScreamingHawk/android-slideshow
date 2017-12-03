package link.standen.michael.slideshow.strategy.image;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.resource.gif.GifDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;

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
		RequestOptions options = new RequestOptions()
				.fitCenter();
		if (PLAY_GIF){
			Glide.with(context)
					.load(item.getPath()).apply(options)
					.preload();
		} else {
			// Force bitmap
			Glide.with(context)
					.asBitmap()
					.load(item.getPath()).apply(options)
					.preload();
		}
	}

	@Override
	public void load(final FileItem item, final ImageView view) {
		RequestOptions options = new RequestOptions()
				.fitCenter()
				.placeholder(view.getDrawable());
		if (PLAY_GIF) {
			options = options.diskCacheStrategy(DiskCacheStrategy.NONE);
			// Play GIFs
			Glide.with(context)
					.load(item.getPath())
					.apply(options)
					.listener(new RequestListener<Drawable>() {
						@Override
						public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
							Log.e(TAG, "Error loading image", e);
							callback.clearLoadingSnackbar();
							return false;
						}

						@Override
						public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
							callback.clearLoadingSnackbar();
							if (resource instanceof GifDrawable) {
								// Queue the next slide after the animation completes
								GifDrawable gifDrawable = (GifDrawable) resource;

								int duration = 250; // Start with a little extra time
								//FIXME getDecoder is not available in Glide v4
								/*
								GifDecoder decoder = gifDrawable.getDecoder();
								for (int i = 0; i < gifDrawable.getFrameCount(); i++) {
									duration += decoder.getDelay(i);
								}
								*/

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
			options = options.diskCacheStrategy(DiskCacheStrategy.RESOURCE)
					.dontAnimate();
			// Force bitmap so GIFs don't play
			Glide.with(context)
					.asBitmap()
					.load(item.getPath())
					.apply(options)
					.listener(new RequestListener<Bitmap>() {
						@Override
						public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Bitmap> target, boolean isFirstResource) {
							Log.e(TAG, "Error loading image", e);
							callback.clearLoadingSnackbar();
							return false;
						}

						@Override
						public boolean onResourceReady(Bitmap resource, Object model, Target<Bitmap> target, DataSource dataSource, boolean isFirstResource) {
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
