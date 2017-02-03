package link.standen.michael.slideshow.util;

import android.app.Activity;
import android.content.Context;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.os.Environment;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import link.standen.michael.slideshow.R;
import link.standen.michael.slideshow.model.FileItem;

public class FileItemHelper {

    private static final String TAG = "FolderHelper";

    public static final String absPath = Environment.getExternalStorageDirectory().getAbsolutePath();

    /**
     * Creates a list of fileitem for the given path.
     * @param currentPath The directory path.
     * @param activity The calling activity. If supplied, the title will be updated, and thumbnail
     *                 will be generated.
     */
    public List<FileItem> getFileList(@NonNull String currentPath, final Activity activity){
        Log.d(TAG, "updateFileList currentPath: "+currentPath);

        // Create file list
        List<FileItem> fileList = new ArrayList<>();
        File dir = new File(currentPath);

        // Set title
        if (activity != null) {
            activity.setTitle(currentPath.replace(absPath, "") + File.separatorChar);
            if (!dir.canRead()){
                activity.setTitle(activity.getTitle() + activity.getResources().getString(R.string.inaccessible));
            }
        }

        File[] files = dir.listFiles();
        if (files != null){
            for (File file : files){
                final FileItem item = new FileItem();
                item.setName(file.getName());
                item.setPath(file.getAbsolutePath());
                item.setIsDirectory(file.isDirectory());
                fileList.add(item);
            }
        }
        Collections.sort(fileList);
        return fileList;
    }

    /**
     * Loads the thumbnail of the fileitem.
     */
    public void loadThumbnail(FileItem item, Context context){
        if (item.getIsDirectory()){
            item.setThumbnail(null);
        } else {
            item.setThumbnail(ThumbnailUtils.extractThumbnail(
                    BitmapFactory.decodeFile(item.getPath()),
                    (int) context.getResources().getDimension(R.dimen.file_image_width),
                    (int) context.getResources().getDimension(R.dimen.file_image_height)));
        }
    }
}
