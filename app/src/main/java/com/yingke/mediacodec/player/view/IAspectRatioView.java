package com.yingke.mediacodec.player.view;

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
public interface IAspectRatioView {

    /**
     * @param aspectRatio
     */
    void setAspectRatio(double aspectRatio);

    /**
     *
     */
    void onPause();

    /**
     *
     */
    void onResume();
}
