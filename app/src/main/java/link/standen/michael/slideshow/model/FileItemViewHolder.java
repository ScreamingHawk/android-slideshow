package link.standen.michael.slideshow.model;

import android.widget.ImageView;
import android.widget.TextView;

public class FileItemViewHolder {
	private FileItem fileItem;

	private TextView textView;
	private ImageView imageView;

	public FileItem getFileItem() {
		return fileItem;
	}

	public void setFileItem(FileItem fileItem) {
		this.fileItem = fileItem;
	}

	public TextView getTextView() {
		return textView;
	}

	public void setTextView(TextView textView) {
		this.textView = textView;
	}

	public ImageView getImageView() {
		return imageView;
	}

	public void setImageView(ImageView imageView) {
		this.imageView = imageView;
	}
}
