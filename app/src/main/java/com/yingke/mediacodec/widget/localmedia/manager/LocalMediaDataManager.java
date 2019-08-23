package com.yingke.mediacodec.widget.localmedia.manager;

import android.content.Context;
import android.text.TextUtils;

import com.yingke.mediacodec.R;
import com.yingke.mediacodec.utils.ToastUtil;
import com.yingke.mediacodec.widget.localmedia.config.MediaConfig;
import com.yingke.mediacodec.widget.localmedia.config.MediaMimeType;
import com.yingke.mediacodec.widget.localmedia.entity.LocalMediaResource;

import java.util.ArrayList;
import java.util.List;

/**
 * 功能：
 * </p>
 * <p>Copyright corp.netease.com 2019 All right reserved </p>
 *
 * @author tuke 时间 2019/8/22
 * @email tuke@corp.netease.com
 * <p>
 * 最后修改人：无
 * <p>
 */
public class LocalMediaDataManager {

    private static final int MAX_IMAGE = 9;
    private static final int MAX_VIDEO = 5;
    private static final int MAX_AUDIO = 2;


    // 已选择的媒体
    private List<LocalMediaResource> selectedMedias = new ArrayList<>();
    // 当前相册媒体结合
    private List<LocalMediaResource> currentFolderMedias = new ArrayList<>();

    public static LocalMediaDataManager getInstance(){
        return DataManagerHolder.INSTANCE;
    }

    private static class DataManagerHolder{
        private static final LocalMediaDataManager INSTANCE  = new LocalMediaDataManager();
    }

    /**
     * 添加 媒体文件
     * @param selectedMedia
     */
    public boolean addSelectedMedia(Context context, LocalMediaResource selectedMedia) {
        if (processCountLimits(context, selectedMedia)) {
            selectedMedias.add(selectedMedia);
            return true;
        }
        return false;
    }

    /**
     * 移除
     * @param unSelectedMedia
     */
    public boolean removeSelectedMedia(LocalMediaResource unSelectedMedia) {
        return selectedMedias.remove(unSelectedMedia);
    }

    /**
     * 返回
     * @return
     */
    public List<LocalMediaResource> getSelectedMedias() {
        return selectedMedias;
    }

    /**
     * @return 大小
     */
    public int selectedSize(){
        return  selectedMedias.size();
    }

    /**
     * 清空
     */
    public void clearSelectedMedias(){
        for(LocalMediaResource resource: selectedMedias) {
            resource.setChecked(false);
        }
        selectedMedias.clear();
    }

    /**
     * 设置当前 显示的相册文件
     * @param currentFolderMedias
     */
    public void setCurrentFolderMedias(List<LocalMediaResource> currentFolderMedias) {
        if (currentFolderMedias != null) {
            this.currentFolderMedias.addAll(currentFolderMedias);
        }
    }

    /**
     * 清空当前文件夹
     */
    public void clearCurrentFolderMedia(){
        this.currentFolderMedias.clear();
    }

    public List<LocalMediaResource> getCurrentFolderMedias() {
        return currentFolderMedias;
    }

    /**
     * 处理 数量,类型限制,去重限制
     * @param context
     * @param data
     * @return
     */
    public boolean processCountLimits(Context context, LocalMediaResource data) {

        // 类型限制 视频和图片不能同时选
        String mimeType = selectedMedias.size() > 0 ? selectedMedias.get(0).getMimeType() : "";
        if (!TextUtils.isEmpty(mimeType)) {
            boolean toEqual = MediaMimeType.mimeToEqual(mimeType, data.getMimeType());
            if (!toEqual) {
                ToastUtil.showToastShort(context.getString(R.string.selector_picture_rule));
                return false;
            }
        }

        // 数量限制
        boolean isImage = mimeType.startsWith(MediaConfig.MimeType.IMAGE_PREFIX);
        if (isImage) {
            if (selectedMedias.size() >= MAX_IMAGE) {
                String error =String.format(context.getString(R.string.selector_picture_max_num), String.valueOf(MAX_IMAGE));
                ToastUtil.showToastShort(error);
                return false;
            }
        }

        boolean isVideo = mimeType.startsWith(MediaConfig.MimeType.VIDEO_PREFIX);
        if (isVideo) {
            if (selectedMedias.size() >= MAX_VIDEO) {
                String error =String.format(context.getString(R.string.selector_picture_video_max_num), String.valueOf(MAX_VIDEO));
                ToastUtil.showToastShort(error);
                return false;
            }
        }

        boolean isAudio = mimeType.startsWith(MediaConfig.MimeType.AUDIO_PREFIX);
        if (isAudio) {
            if (selectedMedias.size() >= MAX_AUDIO) {
                String error =String.format(context.getString(R.string.selector_picture_video_max_num), String.valueOf(MAX_AUDIO));
                ToastUtil.showToastShort(error);
                return false;
            }
        }

        // 去重
        for (LocalMediaResource media : selectedMedias) {
            if (media.getMediaPath().equals(data.getMediaPath())) {
                return false;
            }
        }
        return true;
    }



}
