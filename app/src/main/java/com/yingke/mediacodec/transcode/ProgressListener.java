package com.yingke.mediacodec.transcode;

/**
 * 功能：
 * </p>
 * <p>Copyright corp.netease.com 2018 All right reserved </p>
 *
 * @author tuke 时间 2019/8/16
 * @email tuke@corp.netease.com
 * <p>
 * 最后修改人：无
 * <p>
 */
public interface ProgressListener {

    void onStart();
    void onFinish(boolean result);
    void onProgress(float progress);
}
