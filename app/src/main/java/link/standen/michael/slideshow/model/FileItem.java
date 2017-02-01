package link.standen.michael.slideshow.model;

import android.graphics.Bitmap;
import android.support.annotation.NonNull;

import link.standen.michael.slideshow.R;

/**
 * Model object for a file item.
 */
public class FileItem implements Comparable<FileItem> {

	private String name;
	private String path;
	private Boolean isDirectory;
	private Bitmap thumbnail;
	private Boolean thumbnailAttempted = Boolean.FALSE;

	private FileItemViewHolder holder;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public Boolean getIsDirectory() {
		return isDirectory;
	}

	public void setIsDirectory(Boolean directory) {
		isDirectory = directory;
	}

	@Override
	public int compareTo(@NonNull FileItem other) {
		return this.getName().compareTo(other.getName());
	}

	public Bitmap getThumbnail() {
		return thumbnail;
	}

	public void setThumbnail(Bitmap thumbnail) {
		this.thumbnail = thumbnail;
		this.thumbnailAttempted = true;
		setHolderImageView();
	}

	/**
	 * Update the image view of the holder
	 */
	public void setHolderImageView(){
		if (holder != null && holder.getFileItem() == this){
			if (thumbnail != null){
				// Image thumb
				holder.getImageView().setImageBitmap(thumbnail);
			} else if (isDirectory) {
				// Directory
				holder.getImageView().setImageResource(R.mipmap.folder);
			} else if (thumbnailAttempted){
				// Something unknown
				holder.getImageView().setImageResource(R.mipmap.unknown);
			} else {
				// Loading
				holder.getImageView().setImageResource(R.mipmap.loading);
			}
		}
	}

	public FileItemViewHolder getHolder() {
		return holder;
	}

	public void setHolder(FileItemViewHolder holder) {
		this.holder = holder;
	}

	public Boolean getThumbnailAttempted() {
		return thumbnailAttempted;
	}

	public void setThumbnailAttempted(Boolean thumbnailAttempted) {
		this.thumbnailAttempted = thumbnailAttempted;
	}
}
