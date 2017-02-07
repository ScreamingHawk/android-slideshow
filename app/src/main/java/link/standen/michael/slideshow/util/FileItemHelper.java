package link.standen.michael.slideshow.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.util.Log;

import java.io.File;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import link.standen.michael.slideshow.R;
import link.standen.michael.slideshow.model.FileItem;

public class FileItemHelper {

    private static final String TAG = FileItemHelper.class.getName();

    public static final String absPath = Environment.getExternalStorageDirectory().getAbsolutePath();

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
     * Creates a list of fileitem for the given path.
     * @param currentPath The directory path.
	 * @param includeDirectories Whether or not to include directories.
     */
    public List<FileItem> getFileList(@NonNull String currentPath, boolean includeDirectories){
        Log.d(TAG, "updateFileList currentPath: "+currentPath);

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
						final FileItem item = new FileItem();
						item.setName(file.getName());
						item.setPath(file.getAbsolutePath());
						item.setIsDirectory(file.isDirectory());
						fileList.add(item);
					}
				}
            }
        }
        Collections.sort(fileList);
        return fileList;
    }

	/**
	 * Creates the thumbnail of the fileitem.
	 */
	public Bitmap createThumbnail(FileItem item, boolean force){
		if (item.getIsDirectory()){
			return null;
		}
		if (!force && !SHOW_THUMBNAILS){
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
		String mimeType = URLConnection.guessContentTypeFromName(item.getPath());
		item.setIsImage(mimeType != null && mimeType.startsWith("image"));
		return item.getIsImage();
	}
}
