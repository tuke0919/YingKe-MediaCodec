package com.yingke.mediacodec.videoplayer.view;

/**
 * 功能：
 * </p>
 * <p>Copyright corp.netease.com 2018 All right reserved </p>
 *
 * @author tuke 时间 2019/8/8
 */
public interface IPlayerView {

    /**
     * 准备
     */
    void prepare();

    /**
     *  开始
     */
    void start();

    /**
     * 暂停
     */
    void pause();

    /**
     * 恢复
     */
    void resume();

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
     * 是否播放
     * @return
     */
    boolean isStop();

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
