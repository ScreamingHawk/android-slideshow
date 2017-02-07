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

	private static boolean SHOW_THUMBNAILS;

	public FileItemHelper(Context context){
		this.context = context;
		SHOW_THUMBNAILS = PreferenceManager.getDefaultSharedPreferences(context).getBoolean("show_thumbnails", true);
	}

    /**
     * Creates a list of fileitem for the given path.
     * @param currentPath The directory path.
     */
    public List<FileItem> getFileList(@NonNull String currentPath){
        Log.d(TAG, "updateFileList currentPath: "+currentPath);

        // Create file list
        List<FileItem> fileList = new ArrayList<>();
        File dir = new File(currentPath);

        File[] files = dir.listFiles();
        if (files != null){
			// Check hidden file preference
			boolean showHiddenFiles = PreferenceManager.getDefaultSharedPreferences(context).getBoolean("show_hidden_files", false);
            for (File file : files){
				if (showHiddenFiles || !file.getName().startsWith(".")) {
					final FileItem item = new FileItem();
					item.setName(file.getName());
					item.setPath(file.getAbsolutePath());
					item.setIsDirectory(file.isDirectory());
					fileList.add(item);
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
					(int) context.getResources().getDimension(R.dimen.file_image_width),
					(int) context.getResources().getDimension(R.dimen.file_image_height));
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
