package link.standen.michael.slideshow.adapter;

import android.content.Context;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;
import java.util.Stack;
import java.util.TimerTask;

import link.standen.michael.slideshow.R;
import link.standen.michael.slideshow.model.FileItem;
import link.standen.michael.slideshow.model.FileItemViewHolder;
import link.standen.michael.slideshow.util.FileItemHelper;

/**
 * Class for managing lists of file items.
 */
public class FileItemArrayAdapter extends ArrayAdapter<FileItem> {

	private static final String TAG = FileItemArrayAdapter.class.getName();

	private final Context context;
	private final int resourceId;
	private final List<FileItem> items;

	private final Stack<FileItem> thumbnailStack = new Stack<>();
	private final Handler thumbnailHandler = new Handler();

	/**
	 * Background task for loading thumbnails
	 */
	private final Runnable thumbnailTask = new TimerTask() {
		@Override
		synchronized public void run() {
			if (thumbnailStack.isEmpty()){
				// No items
				return;
			}
			FileItem item = thumbnailStack.pop();
			if (item == null){
				// No items
				return;
			}
			if (item.getIsDirectory() || item.getThumbnailAttempted() || item.getThumbnail() != null){
				// Not valid, already attempted, or already succeeded.
				return;
			}
			item.setThumbnail(new FileItemHelper(context).createThumbnail(item));
		}
	};

	public FileItemArrayAdapter(Context context, int resourceId, List<FileItem> items) {
		super(context, resourceId, items);

		this.context = context;
		this.resourceId = resourceId;
		this.items = items;
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
			if (item.getThumbnail() == null){
				thumbnailStack.push(item);
				thumbnailHandler.post(thumbnailTask);
			}
			item.setHolderImageView();
		}
		return view;
	}
}
