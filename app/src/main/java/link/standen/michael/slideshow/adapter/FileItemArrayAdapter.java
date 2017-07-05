package link.standen.michael.slideshow.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;
import java.util.concurrent.RejectedExecutionException;

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
				try {
					new ThumbnailTask(item).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
				} catch (RejectedExecutionException ex){
					// Not important, log and continue
					Log.e(TAG, "Rejected thumbnail job", ex);
				}
			}
			item.setHolderImageView();
		}
		return view;
	}

	/**
	 * Background task for loading thumbnails
	 */
	private class ThumbnailTask extends AsyncTask<Object, Void, Bitmap> {
		private final FileItem item;

		private ThumbnailTask(FileItem item) {
			this.item = item;
		}

		@Override
		protected void onPostExecute(Bitmap result) {
			if (item.getIsDirectory()){
				return;
			}
			item.setThumbnail(result);
		}

		@Override
		protected Bitmap doInBackground(Object[] params) {
			if (item.getThumbnail() != null){
				return item.getThumbnail();
			}
			if (item.getIsDirectory() || item.getThumbnailAttempted()){
				return null;
			}
			return new FileItemHelper(context).createThumbnail(item);
		}
	}
}
