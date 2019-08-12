package com.yingke.mediacodec.videorecorder.glsurface;

import android.opengl.EGL14;
import android.opengl.EGLExt;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLDisplay;

/**
 * 功能：
 * </p>
 * <p>Copyright corp.netease.com 2018 All right reserved </p>
 *
 * @author tuke 时间 2019/8/12
 * @email tuke@corp.netease.com
 * <p>
 * 最后修改人：无
 * <p>
 */
public class RecordEGLHelper extends EGLHelper {


    public RecordEGLHelper(int surfaceWidth, int surfaceHeight, Object surfaceObj) {
        super(surfaceWidth, surfaceHeight, surfaceObj);
    }

    @Override
    protected BaseConfigChooser getEGLConfigChooser() {
        return new RecordConfigChooser(8, 8, 8, 8, 0,0);
    }

    public class RecordConfigChooser extends SimpleEGLConfigChooser {

        private static final int EGL_RECORDABLE_ANDROID = 0x3142;

        public RecordConfigChooser(int redSize, int greenSize, int blueSize, int alphaSize, int depthSize, int stencilSize) {
            super(redSize, greenSize, blueSize, alphaSize, depthSize, stencilSize);
        }

        public RecordConfigChooser(int[] configSpec) {
            super(configSpec);
        }

        @Override
        protected int[] filterConfigSpec2(int[] configSpec) {
            // 不是2.0 也不是3.0
            if (mEGLContextClientVersion != 2 && mEGLContextClientVersion != 3) {
                return configSpec;
            }
            // 长度
            int len = configSpec.length;
            // 加长2个
            int[] newConfigSpec = new int[len + 2];
            // 拷贝
            System.arraycopy(configSpec, 0, newConfigSpec, 0, len-1);

            // 再加一对属性值

            // len - 1
            newConfigSpec[len - 1] = EGL_RECORDABLE_ANDROID;
            newConfigSpec[len] = 1;
            // 总是以EGL10.EGL_NONE结尾
            newConfigSpec[len + 1] = EGL10.EGL_NONE;
            return newConfigSpec;
        }
    }
}
