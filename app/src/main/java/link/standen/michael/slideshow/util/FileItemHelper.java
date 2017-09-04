package link.standen.michael.slideshow.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.util.Log;
import android.webkit.MimeTypeMap;

import java.io.File;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
		return getFileList(currentPath, true, false);
	}

    /**
     * Creates a list of fileitem for the given path.
     * @param currentPath The directory path.
	 * @param includeDirectories Whether or not to include directories.
	 * @param includeSubDirectories Whether or not to include sub directories.
     */
    public List<FileItem> getFileList(@NonNull String currentPath, boolean includeDirectories,
			boolean includeSubDirectories){
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
						fileList.add(createFileItem(file));
					} else if (includeSubDirectories){
						fileList.addAll(getFileList(file.getAbsolutePath(), includeDirectories, includeSubDirectories));
					}
				}
            }
        }
        Collections.sort(fileList);
        return fileList;
    }

	/**
	 * Create a fileitem from the given file.
	 */
	public FileItem createFileItem(File file){
		FileItem item = new FileItem();
		item.setName(file.getName());
		item.setPath(file.getAbsolutePath());
		item.setIsDirectory(file.isDirectory());
		return item;
	}

	/**
	 * Create a special file item for a folder linking to the default External Storage location.
	 */
	public FileItem createGoHomeFileItem(){
		FileItem item = new FileItem();
		item.setName(context.getResources().getString(R.string.go_home_folder));
		item.setPath(Environment.getExternalStorageDirectory().getAbsolutePath());
		item.setIsDirectory(true);
		item.setIsSpecial(true);
		return item;
	}

	/**
	 * Create a special file item for a folder linking to the default External Storage location.
	 */
	public FileItem createPlayFileItem(){
		FileItem item = new FileItem();
		item.setName(context.getResources().getString(R.string.play_folder));
		item.setIsDirectory(false);
		item.setIsSpecial(true);
		return item;
	}

	/**
	 * Creates the thumbnail of the fileitem.
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
			try {
				return ThumbnailUtils.extractThumbnail(
						BitmapFactory.decodeFile(item.getPath()),
						THUMBNAIL_IMAGE_WIDTH,
						THUMBNAIL_IMAGE_HEIGHT);
			} catch (OutOfMemoryError e){
				// Ignore. Process will try again automatically.
				item.setThumbnailAttempted(false);
				item.setHasThumbnail(false);
			}
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
		String mime = "";
		try {
			mime = URLConnection.guessContentTypeFromName(item.getPath());
		} catch (StringIndexOutOfBoundsException e){
			// Not sure the cause of this issue but it occurred on production so handling as blank mime.
		}

		if (mime == null || mime.isEmpty()){
			// Test mime type by loading the image
			BitmapFactory.Options opt = new BitmapFactory.Options();
			opt.inJustDecodeBounds = true;
			BitmapFactory.decodeFile(item.getPath(), opt);
			mime = opt.outMimeType;
		}

		return mime;
	}
}
