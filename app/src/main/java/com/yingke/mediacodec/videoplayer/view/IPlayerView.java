package com.yingke.mediacodec.videoplayer.view;

/**
 * 功能：
 * </p>
 * <p>Copyright corp.netease.com 2018 All right reserved </p>
 *
 * @author tuke 时间 2019/8/8
 * @email tuke@corp.netease.com
 * <p>
 * 最后修改人：无
 * <p>
 */
public interface IPlayerView {


    /**
     *  开始
     */
    void start();

    /**
     * 暂停
     */
    void pause();

    /**
     * 总时长
     * @return
     */
    String getDuration();

    /**
     * 当前时间
     * @return
     */
    int getCurrentPosition();

    /**
     * 是否正在播放
     * @return
     */
    boolean isPlaying();

    /**
     * 是否暂停
     * @return
     */
    boolean isPaused();

    /**
     * 设置path
     * @param path
     */
    void setVideoPath(String path);



}
