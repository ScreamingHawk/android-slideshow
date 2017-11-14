package link.standen.michael.slideshow.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

import java.util.List;

import link.standen.michael.slideshow.R;
import link.standen.michael.slideshow.model.FileItem;
import link.standen.michael.slideshow.model.FileItemViewHolder;

/**
 * Class for managing lists of file items.
 */
public class FileItemArrayAdapter extends ArrayAdapter<FileItem> {

	private static final String TAG = FileItemArrayAdapter.class.getName();

	private final Context context;
	private final int resourceId;
	private final List<FileItem> items;

	private transient Boolean thumbnailPreference;

	public FileItemArrayAdapter(Context context, List<FileItem> items) {
		super(context, R.layout.file_item, items);

		this.context = context;
		this.resourceId = R.layout.file_item;
		this.items = items;
	}

	/**
	 * Determine if the thumbnail preference is set.
	 */
	private boolean thumbnailPreferenceOn(){
		if (thumbnailPreference == null) {
			thumbnailPreference = PreferenceManager.getDefaultSharedPreferences(context).getBoolean("show_thumbnails", true);
		}
		return thumbnailPreference;
	}

	public FileItem getItem(int index){
		return items.get(index);
	}

	public @NonNull View getView(int position, View convertView, @NonNull ViewGroup parent){
		View view = convertView;
		FileItemViewHolder holder;
		if (view == null){
			LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			assert inflater != null;
			view = inflater.inflate(resourceId, null);
			holder = new FileItemViewHolder();
			holder.setTextView((TextView) view.findViewById(R.id.file_name));
			holder.setImageView((ImageView) view.findViewById(R.id.file_image));
			view.setTag(holder);
		} else {
			holder = (FileItemViewHolder) view.getTag();
		}

		final FileItem item = getItem(position);
		if (item != null){
			holder.setFileItem(item);
			holder.getTextView().setText(item.getName());
			// Set thumbnail image
			final ImageView imageView = holder.getImageView();
			if (thumbnailPreferenceOn() && item.couldHaveThumbnail()){
				Glide.with(context)
						.load(item.getPathUri())
						.asBitmap()
						.placeholder(R.mipmap.loading)
						.listener(new RequestListener<String, Bitmap>() {
							@Override
							public boolean onException(Exception e, String s, Target<Bitmap> target, boolean b) {
								item.setHasNoThumbnail();
								return false;
							}

							@Override
							public boolean onResourceReady(Bitmap bitmap, String s, Target<Bitmap> target, boolean b, boolean b1) {
								return false;
							}
						}).error(item.getImageResource())
						.into(imageView);
			} else {
				Glide.with(context)
						.load(item.getImageResource())
						.into(imageView);
			}
		}
		return view;
	}
}
