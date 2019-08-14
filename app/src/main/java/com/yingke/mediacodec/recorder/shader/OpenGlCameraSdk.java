package com.yingke.mediacodec.recorder.shader;

import android.content.Context;

/**
 * 功能：
 * </p>
 * <p>Copyright corp.netease.com 2018 All right reserved </p>
 *
 * @author tuke 时间 2019/7/16
 * @email tuke@corp.netease.com
 * <p>
 * 最后修改人：无
 * <p>
 */
public class OpenGlCameraSdk {

    private Context mContext;
    private static OpenGlCameraSdk mOpenGlCameraSdk;


    public static OpenGlCameraSdk getInstance() {
        if (mOpenGlCameraSdk == null) {
            mOpenGlCameraSdk = new OpenGlCameraSdk();
        }
        return mOpenGlCameraSdk;
    }

    public void init(Context context) {
        this.mContext = context;
    }

    public Context getContext() {
        return mContext;
    }

}
