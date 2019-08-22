package com.yingke.mediacodec.widget.localmedia;

import android.app.Activity;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;


import com.yingke.mediacodec.R;
import com.yingke.mediacodec.widget.localmedia.config.LocalMediaConfig;
import com.yingke.mediacodec.widget.localmedia.config.MediaMimeType;
import com.yingke.mediacodec.widget.localmedia.entity.LocalMediaFolder;
import com.yingke.mediacodec.widget.localmedia.entity.LocalMediaResource;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * 功能：
 * </p>
 * <p>Copyright corp.netease.com 2018 All right reserved </p>
 *
 * @author tuke 时间 2019/5/28
 * @email tuke@corp.netease.com
 * <p>
 * 最后修改人：无
 * <p>
 */
public class LocalMediaLoader {

    private static final Uri QUERY_URI = MediaStore.Files.getContentUri("external");
    private static final String ORDER_BY = MediaStore.Files.FileColumns._ID + " DESC";
    private static final String DURATION = "duration";
    private static final String NOT_GIF = "!='image/gif'";

    private static final int MIN_AUDIO_DURATION = 500;  // 过滤掉小于500毫秒的录音


    private  boolean hasInited = false;


    // 媒体文件数据库字段
    private static final String[] PROJECTION = {
            MediaStore.Files.FileColumns._ID,
            MediaStore.MediaColumns.DATA,
            MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.MediaColumns.WIDTH,
            MediaStore.MediaColumns.HEIGHT,
            DURATION};

    // 图片
    private static final String SELECTION_ALL_IMAGE = MediaStore.Files.FileColumns.MEDIA_TYPE + "=?"
            + " AND " + MediaStore.MediaColumns.SIZE + ">0";

    // 图片 除去gif
    private static final String SELECTION_IMAGE_NOT_GIF = MediaStore.Files.FileColumns.MEDIA_TYPE + "=?"
            + " AND " + MediaStore.MediaColumns.SIZE + ">0"
            + " AND " + MediaStore.MediaColumns.MIME_TYPE + NOT_GIF;

    // ALL 类型 实际是获取图片or视频
    private static final String[] SELECTION_ALL_ARGS = {
            String.valueOf(MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE),
            String.valueOf(MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO),
    };


    private Activity  activity;

    public LocalMediaLoader(Activity activity) {
        this.activity = activity;
    }

    public void loadMedia(final LocalMediaLoadCallback callback) throws Exception {
       if (activity instanceof  FragmentActivity) {
           ((FragmentActivity) activity).getSupportLoaderManager().initLoader(LocalMediaConfig.getInstance().getMediaType().ordinal(), null,
                   new LoaderManager.LoaderCallbacks<Cursor>() {

                       @Override
                       public Loader<Cursor> onCreateLoader(int i,  Bundle bundle) {
                           CursorLoader cursorLoader = null;
                           if (i == MediaMimeType.ofAll().ordinal()) {
                               // 全部,实际是图片和视频

                               String allSelection = getAllSelection(getDurationCondition(0, 0), LocalMediaConfig.getInstance().isShowGif());
                               cursorLoader = new CursorLoader(
                                       activity,
                                       QUERY_URI,
                                       PROJECTION,
                                       allSelection,
                                       SELECTION_ALL_ARGS,
                                       ORDER_BY);

                           } else if (i == MediaMimeType.ofImage().ordinal()){
                               // 图片
                               String imageSelection = getImageSelection();
                               String[] imageSelectionArgs = new String[]{String.valueOf(MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE)};
                               cursorLoader = new CursorLoader(
                                       activity,
                                       QUERY_URI,
                                       PROJECTION,
                                       imageSelection,
                                       imageSelectionArgs,
                                       ORDER_BY);

                           } else if (i == MediaMimeType.ofVideo().ordinal()){
                               // 视频
                               String videoSelection = getVideoAudioSelection(getDurationCondition(0,0));
                               String[] videoSelectionArgs = new String[]{String.valueOf(MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO)};
                               cursorLoader = new CursorLoader(
                                       activity,
                                       QUERY_URI,
                                       PROJECTION,
                                       videoSelection,
                                       videoSelectionArgs,
                                       ORDER_BY);

                           } else if (i == MediaMimeType.ofAudio().ordinal()){
                               // 音频
                               String audioSelection = getVideoAudioSelection(getDurationCondition(0,MIN_AUDIO_DURATION));
                               String[] audioSelectionArgs = new String[]{String.valueOf(MediaStore.Files.FileColumns.MEDIA_TYPE_AUDIO)};
                               cursorLoader = new CursorLoader(
                                       activity,
                                       QUERY_URI,
                                       PROJECTION,
                                       audioSelection,
                                       audioSelectionArgs,
                                       ORDER_BY);
                           }
                           return cursorLoader;
                       }

                       @Override
                       public void onLoadFinished( Loader<Cursor> loader, Cursor cursor) {
                           if (hasInited) {
                               return;
                           }

                           try {
                               // 相册文件夹列表
                               List<LocalMediaFolder> imageFoldersList = new ArrayList<>();
                               // 全部图片的文件夹
                               LocalMediaFolder allImagesFolder = new LocalMediaFolder();
                               // 全部图片列表
                               List<LocalMediaResource> allImagesList = new ArrayList<>();
                               if (cursor != null) {
                                   int count = cursor.getCount();
                                   if (count > 0) {
                                       cursor.moveToFirst();
                                       do {
                                           String mediaPath = cursor.getString(cursor.getColumnIndexOrThrow(PROJECTION[1]));
                                           String mimeType = cursor.getString(cursor.getColumnIndexOrThrow(PROJECTION[2]));
                                           int width = cursor.getInt(cursor.getColumnIndexOrThrow(PROJECTION[3]));
                                           int height = cursor.getInt(cursor.getColumnIndexOrThrow(PROJECTION[4]));
                                           int duration = cursor.getInt(cursor.getColumnIndexOrThrow(PROJECTION[5]));

                                           LocalMediaResource mediaResource = new LocalMediaResource();
                                           mediaResource.setMediaType(LocalMediaConfig.getInstance().getMediaType());
                                           mediaResource.setMediaPath(mediaPath);
                                           mediaResource.setMimeType(mimeType);
                                           mediaResource.setWidth(width);
                                           mediaResource.setHeight(height);
                                           mediaResource.setDuration(duration);

                                           // 添加到相应相册文件夹
                                           LocalMediaFolder folder = createImageFolder(mediaPath, imageFoldersList);
                                           List<LocalMediaResource> images = folder.getFolderImages();
                                           images.add(mediaResource);
                                           folder.setImageNum(folder.getImageNum() + 1);

                                           // 收集全部图片
                                           allImagesList.add(mediaResource);
                                           int imageNum = allImagesFolder.getImageNum();
                                           allImagesFolder.setImageNum(imageNum + 1);

                                       } while (cursor.moveToNext());


                                       if (allImagesList.size() > 0) {
                                           // 排序
                                           sortFolder(imageFoldersList);
                                           // 插入“全部图片文件夹”
                                           imageFoldersList.add(0, allImagesFolder);
                                           // “全部图片文件夹” 设置第一张图片
                                           allImagesFolder.setFirstImagePath(allImagesList.get(0).getMediaPath());
                                           String title = LocalMediaConfig.getInstance().getMediaType() == MediaMimeType.ofAudio() ?
                                                   activity.getString(R.string.selector_title_all_audio) : activity.getString(R.string.selector_title_camera_roll);
                                           allImagesFolder.setFolderName(title);
                                           allImagesFolder.setFolderImages(allImagesList);
                                       }
                                       if (callback != null) {
                                           callback.loadCompleted(imageFoldersList);
                                       }
                                   } else {
                                       // 如果没有相册
                                       if (callback != null) {
                                           callback.loadCompleted(imageFoldersList);
                                       }
                                   }

                                   hasInited = true;
                               }
                           } catch (Exception e) {
                               e.printStackTrace();
                           }
                       }

                       @Override
                       public void onLoaderReset(Loader<Cursor> loader) {

                       }
                   });

       } else {
           throw new Exception("activity must be subClass of FragmentActivity");
       }
    }

    /**
     * 全部模式下条件 实际是图片和视频
     * @param timeCondition
     * @param isGif
     * @return
     */
    private  String getAllSelection(String timeCondition, boolean isGif) {
        String condition = "(" + MediaStore.Files.FileColumns.MEDIA_TYPE + "=?"
                + (isGif ? "" : " AND " + MediaStore.MediaColumns.MIME_TYPE + NOT_GIF)
                + " OR "
                + (MediaStore.Files.FileColumns.MEDIA_TYPE + "=? AND " + timeCondition) + ")"
                + " AND " + MediaStore.MediaColumns.SIZE + ">0";
        return condition;
    }

    /**
     * 图片条件
     * @return
     */
    private  String getImageSelection() {
        return LocalMediaConfig.getInstance().isShowGif() ? SELECTION_ALL_IMAGE : SELECTION_IMAGE_NOT_GIF;
    }

    /**
     * 查询条件(音视频)
     * @param timeCondition
     * @return
     */
    private static String getVideoAudioSelection(String timeCondition) {
        return MediaStore.Files.FileColumns.MEDIA_TYPE + "=?"
                + " AND " + MediaStore.MediaColumns.SIZE + ">0"
                + " AND " + timeCondition;
    }

    /**
     * 创建相应文件夹
     * 同一个文件夹下，返回自己，否则创建新文件夹
     *
     * @param path
     * @param imageFoldersList
     * @return
     */
    private LocalMediaFolder createImageFolder(String path, List<LocalMediaFolder> imageFoldersList) {
        File imageFile = new File(path);
        File folderFile = imageFile.getParentFile();

        // 原来有folder了
        for (LocalMediaFolder folder : imageFoldersList) {
            if (folder.getFolderName().equals(folderFile.getName())) {
                return folder;
            }
        }

        // 新建添加folder
        LocalMediaFolder newFolder = new LocalMediaFolder();
        newFolder.setFolderName(folderFile.getName());
        newFolder.setFolderPath(folderFile.getAbsolutePath());
        newFolder.setFirstImagePath(path);
        imageFoldersList.add(newFolder);
        return newFolder;
    }


    /**
     * 文件夹数量进行排序
     *
     * @param imageFoldersList
     */
    private void sortFolder(List<LocalMediaFolder> imageFoldersList) {
        // 文件夹按图片数量排序
        Collections.sort(imageFoldersList, new Comparator<LocalMediaFolder>() {
            @Override
            public int compare(LocalMediaFolder lhs, LocalMediaFolder rhs) {
                if (lhs.getFolderImages() == null || rhs.getFolderImages() == null) {
                    return 0;
                }
                int lsize = lhs.getImageNum();
                int rsize = rhs.getImageNum();
                return lsize == rsize ? 0 : (lsize < rsize ? 1 : -1);
            }
        });
    }

    /**
     * 获取视频(最长或最小时间)
     * TODO 有误
     * @param exMaxLimit
     * @param exMinLimit
     * @return
     */
    private String getDurationCondition(long exMaxLimit, long exMinLimit) {
        long maxS = LocalMediaConfig.getInstance().getVideoMaxSecond() == 0 ? Long.MAX_VALUE : LocalMediaConfig.getInstance().getVideoMaxSecond();
        if (exMaxLimit != 0) {
            maxS = Math.min(maxS, exMaxLimit);
        }
        return String.format(Locale.CHINA, "%d <%s duration and duration <= %d",
                Math.max(exMinLimit,  LocalMediaConfig.getInstance().getVideoMinSecond()),
                Math.max(exMinLimit,  LocalMediaConfig.getInstance().getVideoMinSecond()) == 0 ? "" : "=",
                maxS);
    }

    /**
     * 加载本地文件回调
     */
    public interface LocalMediaLoadCallback {
        void loadCompleted(List<LocalMediaFolder> folders);
    }


}
