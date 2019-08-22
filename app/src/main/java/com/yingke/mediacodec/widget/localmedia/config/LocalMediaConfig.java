package com.yingke.mediacodec.widget.localmedia.config;

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
public class LocalMediaConfig {

    // 要选择的媒体类型  图片，音频，视频
    private MediaConfig.MediaType mediaType = MediaConfig.MediaType.MEDIA_TYPE_ALL;
    // 多选 单选
    private MediaConfig.SelectionMode selectionMode = MediaConfig.SelectionMode.MULTI_SELECTION;
    // 视频最大时长
    public int videoMaxSecond;
    // 视频最小时长
    public int videoMinSecond;
    // 图片是否显示gif
    private boolean isShowGif = true;

    private static LocalMediaConfig mLocalMediaConfig;

    private LocalMediaConfig(){}

    public static LocalMediaConfig getInstance() {
        if (mLocalMediaConfig == null) {
            synchronized (LocalMediaConfig.class) {
                if (mLocalMediaConfig == null) {
                     mLocalMediaConfig = new LocalMediaConfig();
                }
            }
        }
        return mLocalMediaConfig;
    }

    public MediaConfig.MediaType getMediaType() {
        return mediaType;
    }

    public LocalMediaConfig setMediaType(MediaConfig.MediaType mediaType) {
        this.mediaType = mediaType;
        return this;
    }

    public MediaConfig.SelectionMode getSelectionMode() {
        return selectionMode;
    }

    public LocalMediaConfig setSelectionMode(MediaConfig.SelectionMode selectionMode) {
        this.selectionMode = selectionMode;
        return this;
    }

    public boolean isShowGif() {
        return isShowGif;
    }

    public LocalMediaConfig setShowGif(boolean showGif) {
        isShowGif = showGif;
        return this;
    }

    public int getVideoMaxSecond() {
        return videoMaxSecond;
    }

    public void setVideoMaxSecond(int videoMaxSecond) {
        this.videoMaxSecond = videoMaxSecond;
    }

    public int getVideoMinSecond() {
        return videoMinSecond;
    }

    public void setVideoMinSecond(int videoMinSecond) {
        this.videoMinSecond = videoMinSecond;
    }
}
