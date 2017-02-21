package link.standen.michael.slideshow.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import link.standen.michael.slideshow.R;
import link.standen.michael.slideshow.model.FileItem;

public class FileItemHelper {

    private static final String TAG = FileItemHelper.class.getName();

	private final Context context;

	// Preferences
	private static boolean SHOW_THUMBNAILS;
	private static Integer THUMBNAIL_IMAGE_WIDTH;
	private static Integer THUMBNAIL_IMAGE_HEIGHT;

	public FileItemHelper(Context context){
		this.context = context;
		SHOW_THUMBNAILS = PreferenceManager.getDefaultSharedPreferences(context).getBoolean("show_thumbnails", true);
		THUMBNAIL_IMAGE_WIDTH = (int) context.getResources().getDimension(R.dimen.file_image_width);
		THUMBNAIL_IMAGE_HEIGHT = (int) context.getResources().getDimension(R.dimen.file_image_height);
	}

	/**
	 * Creates a list of fileitem for the given path. Includes all directories.
	 * @param currentPath The directory path.
	 */
	public List<FileItem> getFileList(@NonNull String currentPath){
		return getFileList(currentPath, true);
	}

    /**
     * Creates a list of FileItem for the given path.
     * @param currentPath The directory path.
	 * @param includeDirectories Whether or not to include directories.
     */
    public List<FileItem> getFileList(@NonNull String currentPath, boolean includeDirectories){
        Log.d(TAG, "updateFileList currentPath: " + currentPath);

        // Create file list
        List<FileItem> fileList = new ArrayList<>();
        File dir = new File(currentPath);

        File[] files = dir.listFiles();
        if (files != null){
			// Check hidden file preference
			boolean showHiddenFiles = PreferenceManager.getDefaultSharedPreferences(context).getBoolean("show_hidden_files", false);
			for (File file : files){
				// Test hidden files
				if (showHiddenFiles || !file.getName().startsWith(".")) {
					// Test directories
					if (includeDirectories || !file.isDirectory()) {
						fileList.add(createFileItem(file));
					}
				}
			}
        }
        Collections.sort(fileList);
        return fileList;
    }

	/**
	 * Create a list of available storage options.
	 * http://stackoverflow.com/a/19982451
	 */
	public List<FileItem> getStorageList() {

		List<FileItem> list = new ArrayList<>();
		String def_path = Environment.getExternalStorageDirectory().getPath();
		boolean def_path_internal = !Environment.isExternalStorageRemovable();
		String def_path_state = Environment.getExternalStorageState();
		boolean def_path_available = def_path_state.equals(Environment.MEDIA_MOUNTED)
				|| def_path_state.equals(Environment.MEDIA_MOUNTED_READ_ONLY);
		boolean def_path_readonly = Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED_READ_ONLY);
		BufferedReader buf_reader = null;
		try {
			Set<String> paths = new HashSet<>();
			buf_reader = new BufferedReader(new FileReader("/proc/mounts"));
			String line;
			int cur_display_number = 1;
			Log.d(TAG, "/proc/mounts");
			while ((line = buf_reader.readLine()) != null) {
				Log.d(TAG, line);
				if (line.contains("vfat") || line.contains("/mnt")) {
					StringTokenizer tokens = new StringTokenizer(line, " ");
					tokens.nextToken(); // ignore device
					String mount_point = tokens.nextToken(); //mount point
					if (paths.contains(mount_point)) {
						continue;
					}
					tokens.nextToken(); // ignore file system
					List<String> flags = Arrays.asList(tokens.nextToken().split(","));
					boolean readonly = flags.contains("ro");

					if (mount_point.equals(def_path)) {
						paths.add(def_path);
						list.add(0, createFileItem(def_path, def_path_internal, readonly, -1));
					} else if (line.contains("/dev/block/vold")) {
						if (!line.contains("/mnt/secure")
								&& !line.contains("/mnt/asec")
								&& !line.contains("/mnt/obb")
								&& !line.contains("/dev/mapper")
								&& !line.contains("tmpfs")) {
							paths.add(mount_point);
							list.add(createFileItem(mount_point, false, readonly, cur_display_number++));
						}
					}
				}
			}

			if (!paths.contains(def_path) && def_path_available) {
				list.add(0, createFileItem(def_path, def_path_internal, def_path_readonly, -1));
			}

		} catch (IOException ex) {
			ex.printStackTrace();
		} finally {
			if (buf_reader != null) {
				try {
					buf_reader.close();
				} catch (IOException ex) {
					Log.e(TAG, ex.toString());
				}
			}
		}
		return list;
	}

	/**
	 * Create a FileItem from the given information.
	 * @param path The path
	 * @param internal Whether the location is considered internal
	 * @param readonly Whether the location is read only.
	 * @param display_number An integer for name display purposes.
	 */
	private FileItem createFileItem(String path, boolean internal, boolean readonly, int display_number) {
		FileItem item = new FileItem();
		item.setPath(path);
		item.setIsDirectory(true);
		// Set the name using the details provided
		StringBuilder bob = new StringBuilder();
		if (internal) {
			bob.append("Internal Storage");
		} else {
			bob.append("SD card");
			if (display_number > 1) {
				bob.append(display_number);
			}
		}
		if (readonly) {
			bob.append(" (Read only)");
		}
		item.setName(bob.toString());
		return item;
	}

	/**
	 * Create a FileItem from the given file.
	 */
	public FileItem createFileItem(File file){
		FileItem item = new FileItem();
		item.setName(file.getName());
		item.setPath(file.getAbsolutePath());
		item.setIsDirectory(file.isDirectory());
		return item;
	}

	/**
	 * Creates the thumbnail of the FileItem.
	 */
	public Bitmap createThumbnail(FileItem item){
		if (item.getIsDirectory()){
			return null;
		}
		if (!SHOW_THUMBNAILS){
			// Thumbnail should not be loaded.
			return null;
		}
		if (!item.getThumbnailAttempted() || item.getHasThumbnail()) {
			return ThumbnailUtils.extractThumbnail(
					BitmapFactory.decodeFile(item.getPath()),
					THUMBNAIL_IMAGE_WIDTH,
					THUMBNAIL_IMAGE_HEIGHT);
		}
		return null;
	}

	/**
	 * Checks the mime-type of the file to see if it is an image.
	 */
	public boolean isImage(FileItem item){
		if (item.getIsDirectory()){
			return false;
		}
		if (item.getIsImage() != null){
			return item.getIsImage();
		}
		String mimeType = getImageMimeType(item);
		item.setIsImage(mimeType != null && mimeType.startsWith("image"));
		return item.getIsImage();
	}

	/**
	 * Returns the mime type of the given item.
	 */
	public String getImageMimeType(FileItem item){
		return URLConnection.guessContentTypeFromName(item.getPath());
	}
}
