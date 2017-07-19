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

import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener;

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
	private final ImageLoader imageLoader = ImageLoader.getInstance();

	private transient Boolean thumbnailPreference;

	public FileItemArrayAdapter(Context context, int resourceId, List<FileItem> items) {
		super(context, resourceId, items);

		this.context = context;
		this.resourceId = resourceId;
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
			item.setHolder(holder);
			holder.getTextView().setText(item.getName());
			// Set thumbnail image
			if (thumbnailPreferenceOn() && item.couldHaveThumbnail() && item.getThumbnail() == null){
				imageLoader.loadImage(item.getPathUri(),
						new ImageLoadingListener(){
							@Override
							public void onLoadingStarted(String s, View view) {
								item.setHolderImageView();
							}

							@Override
							public void onLoadingFailed(String s, View view, FailReason failReason) {
								item.setThumbnailAttempted(true);
								item.setHolderImageView();
							}

							@Override
							public void onLoadingComplete(String s, View view, Bitmap bitmap) {
								item.setThumbnail(bitmap);
								item.setHolderImageView();
							}

							@Override
							public void onLoadingCancelled(String s, View view) {
								item.setHolderImageView();
							}
						});
			} else {
				item.setHolderImageView();
			}
		}
		return view;
	}
}
