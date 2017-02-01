package link.standen.michael.slideshow.model;

import android.graphics.Bitmap;
import android.support.annotation.NonNull;

/**
 * Model object for a file item.
 */
public class FileItem implements Comparable<FileItem> {

	private String name;
	private String path;
	private Boolean isDirectory;
	private Bitmap thumbnail;

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
	}
}
