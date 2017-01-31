package link.standen.michael.slideshow.model;

/**
 * Model object for a file item.
 */
public class FileItem implements Comparable<FileItem> {

	private String name;
	private String path;
	private Boolean isDirectory;

	public FileItem(String name, String path, Boolean isDirectory){
		this.name = name;
		this.path = path;
		this.isDirectory = isDirectory;
	}

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
	public int compareTo(FileItem other) {
		return this.getName().compareTo(other.getName());
	}
}
