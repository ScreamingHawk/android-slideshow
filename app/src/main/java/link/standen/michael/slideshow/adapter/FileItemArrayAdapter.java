package link.standen.michael.slideshow.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import link.standen.michael.slideshow.R;
import link.standen.michael.slideshow.model.FileItem;
import link.standen.michael.slideshow.model.FileItemViewHolder;

/**
 * Class for managing lists of file items.
 */
public class FileItemArrayAdapter extends ArrayAdapter<FileItem> {

	private Context context;
	private int resourceId;
	private List<FileItem> items;

	public FileItemArrayAdapter(Context context, int resourceId, List<FileItem> items) {
		super(context, resourceId, items);

		this.context = context;
		this.resourceId = resourceId;
		this.items = items;
	}

	public FileItem getItem(int index){
		return items.get(index);
	}

	public View getView(int position, View convertView, ViewGroup parent){
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
			holder.getTextView().setText(item.getName());
			if (item.getIsDirectory()){
				holder.getImageView().setImageResource(R.mipmap.folder);
			} else {
				holder.getImageView().setImageURI(null); //TODO Thumbnail
			}
		}
		return view;
	}
}
