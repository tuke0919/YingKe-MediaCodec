package com.yingke.mediacodec;

import android.app.Application;

import com.yingke.mediacodec.videorecorder.shader.OpenGlCameraSdk;


/**
 * 功能：
 * </p>
 * <p>Copyright corp.netease.com 2018 All right reserved </p>
 *
 * @author tuke 时间 2019/7/17
 * @email tuke@corp.netease.com
 * <p>
 * 最后修改人：无
 * <p>
 */
public class MediaCodecApplication extends Application {

    private static MediaCodecApplication app;
    @Override
    public void onCreate() {
        super.onCreate();
        app = this;
        OpenGlCameraSdk.getInstance().init(this);
    }

    public static MediaCodecApplication getInstance() {
        return app;
    }


}
