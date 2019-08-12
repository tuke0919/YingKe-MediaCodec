package com.yingke.mediacodec.videorecorder.glsurface;


import android.graphics.SurfaceTexture;
import android.opengl.EGL14;
import android.opengl.EGLExt;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGL11;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;
import javax.microedition.khronos.opengles.GL10;

import static javax.microedition.khronos.egl.EGL10.EGL_BAD_ACCESS;
import static javax.microedition.khronos.egl.EGL10.EGL_BAD_ALLOC;
import static javax.microedition.khronos.egl.EGL10.EGL_BAD_ATTRIBUTE;
import static javax.microedition.khronos.egl.EGL10.EGL_BAD_CONFIG;
import static javax.microedition.khronos.egl.EGL10.EGL_BAD_CONTEXT;
import static javax.microedition.khronos.egl.EGL10.EGL_BAD_CURRENT_SURFACE;
import static javax.microedition.khronos.egl.EGL10.EGL_BAD_DISPLAY;
import static javax.microedition.khronos.egl.EGL10.EGL_BAD_MATCH;
import static javax.microedition.khronos.egl.EGL10.EGL_BAD_NATIVE_PIXMAP;
import static javax.microedition.khronos.egl.EGL10.EGL_BAD_NATIVE_WINDOW;
import static javax.microedition.khronos.egl.EGL10.EGL_BAD_PARAMETER;
import static javax.microedition.khronos.egl.EGL10.EGL_BAD_SURFACE;
import static javax.microedition.khronos.egl.EGL10.EGL_NOT_INITIALIZED;
import static javax.microedition.khronos.egl.EGL10.EGL_SUCCESS;

/**
 * 功能：
 * EGL执行流程：
 * a, 选择Display
 * b, 选择Config
 * c, 创建Surface
 * d, 创建Context
 * e, 指定当前的环境为绘制环境
 * </p>
 * <p>Copyright corp.netease.com 2018 All right reserved </p>
 *
 * @author tuke 时间 2019/7/15
 * @email tuke@corp.netease.com
 * <p>
 * 最后修改人：无
 * <p>
 */
public class EGLHelper {

    public EGL10 mEgl10;
    public EGLDisplay mEglDisplay;
    public EGLConfig mEglConfig ;
    public EGLSurface mEglSurface;
    public EGLContext mEglContext;

    public GL10 mGL10;

    public int mSurfaceWidth;
    public int mSurfaceHeight;

    // Open GL ES 2.0
    public int mEGLContextClientVersion = 2;
    // 创建Surface 的承载对象
    private Object mSurfaceNativeObj;


    public EGLHelper(int surfaceWidth, int surfaceHeight, Object surfaceNativeObj) {
        this.mSurfaceWidth = surfaceWidth;
        this.mSurfaceHeight = surfaceHeight;
        this.mSurfaceNativeObj = surfaceNativeObj;

        createEglContext();
    }

    /**
     * 创建 EGL环境
     */
    public void createEglContext() {
        // 创建EGL实例
        mEgl10 = (EGL10) EGLContext.getEGL();
        // 创建EglDisplay
        mEglDisplay = mEgl10.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);

        if (mEglDisplay == EGL10.EGL_NO_DISPLAY) {
            throw new RuntimeException("eglGetDisplay failed");
        }

        // 初始化主版本号和副版本号
        int[] version = new int[2];
        if (!mEgl10.eglInitialize(mEglDisplay, version)) {
            throw new RuntimeException("eglInitialize failed");
        }
        // 设置 egl配置
        EGLConfigChooser eglConfigChooser = getEGLConfigChooser();
        mEglConfig = eglConfigChooser.chooseConfig(mEgl10, mEglDisplay);

        // 创建 EglSurface
        EGLSurfaceFactory eglSurfaceFactory = getEGLSurfaceFactory();
        // surface 属性
        int[] attrList = new int[]{
                EGL10.EGL_WIDTH, mSurfaceWidth,
                EGL10.EGL_HEIGHT, mSurfaceHeight,
                EGL10.EGL_NONE
        };

        mEglSurface = eglSurfaceFactory.createSurface(mEgl10, mEglDisplay, mEglConfig, getSurfaceNativeObj(), attrList);

        // 创建 EglContext
        EGLContextFactory eglContextFactory = getEGLContextFactory();
        mEglContext = eglContextFactory.createContext(mEgl10, mEglDisplay, mEglConfig);

        // 指定当前的环境为绘制环境
        mEgl10.eglMakeCurrent(mEglDisplay,mEglSurface,mEglSurface,mEglContext);
        mGL10 = (GL10)mEglContext.getGL();

    }

    /**
     * @return
     */
    protected int getSurfaceType() {
        return EGLSurfaceFactory.SURFACE_PBUFFER;
    }

    /**
     * @return
     */
    protected Object getSurfaceNativeObj() {
        return mSurfaceNativeObj;
    }

    /**
     * @return
     */
    protected BaseConfigChooser getEGLConfigChooser() {
        return  new SimpleEGLConfigChooser(8, 8, 8, 8, 0,0);
    }

    /**
     * @return
     */
    protected EGLSurfaceFactory getEGLSurfaceFactory() {
        return new SimpleEglSurfaceFactory(getSurfaceType());
    }

    /**
     * @return
     */
    protected EGLContextFactory getEGLContextFactory(){
        return new SimpleEGLContextFactory();
    }



    /**
     * 销毁 EGL环境
     */
    public void destroyEglContext() {

        mEgl10.eglMakeCurrent(mEglDisplay, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_CONTEXT);

        mEgl10.eglDestroySurface(mEglDisplay, mEglSurface);
        mEgl10.eglDestroyContext(mEglDisplay, mEglContext);
        mEgl10.eglTerminate(mEglDisplay);
    }

    /**
     * 交换buffer数据
     *
     * @return
     */
    public int swapMyEGLBuffers() {
        if (mEgl10 == null) {
            throwEglException("swapMyEGLBuffers mEgl10 is null ", 0);
        }
        boolean result = mEgl10.eglSwapBuffers(mEglDisplay, mEglSurface);
        if (!result) {
            throwEglException("swapMyEGLBuffers", mEgl10.eglGetError());
        }
        return EGL10.EGL_SUCCESS;
    }



    /**
     * 配置选择器
     */
    public interface EGLConfigChooser {
        /**
         * @param egl10
         * @param eglDisplay
         * @return
         */
        EGLConfig chooseConfig(EGL10 egl10, EGLDisplay eglDisplay);
    }

    /**
     *
     */
    public abstract class BaseConfigChooser implements EGLConfigChooser {

        // 指定的配置
        protected int[] mConfigSpec;

        public BaseConfigChooser(int[] mConfigSpec) {
            this.mConfigSpec = filterConfigSpec(mConfigSpec);
        }

        private int[] filterConfigSpec(int[] configSpec) {
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

            // len - 1
            newConfigSpec[len-1] = EGL10.EGL_RENDERABLE_TYPE;
            // len 指定渲染api版本, EGL14.EGL_OPENGL_ES2_BIT
            if (mEGLContextClientVersion == 2) {
                newConfigSpec[len] = EGL14.EGL_OPENGL_ES2_BIT;
            } else {
                newConfigSpec[len] = EGLExt.EGL_OPENGL_ES3_BIT_KHR;
            }
            // 总是以EGL10.EGL_NONE结尾
            newConfigSpec[len + 1] = EGL10.EGL_NONE;

            return newConfigSpec;
        }

        @Override
        public EGLConfig chooseConfig(EGL10 egl10, EGLDisplay eglDisplay) {
            int[] numConfig = new int[1];
            if (!egl10.eglChooseConfig(eglDisplay, mConfigSpec, null, 0, numConfig)) {
                throw new IllegalArgumentException("eglChooseConfig failed");
            }
            int numConfigs = numConfig[0];
            if (numConfigs < 0) {
                throw new IllegalArgumentException("No configs match configSpec");
            }

            EGLConfig[] eglConfigs = new EGLConfig[numConfigs];
            if (!egl10.eglChooseConfig(eglDisplay, mConfigSpec, eglConfigs, eglConfigs.length, numConfig)) {
                throw new IllegalArgumentException("eglChooseConfig#2 failed");
            }

            EGLConfig targetConfig = chooseConfig(egl10, eglDisplay, eglConfigs);
            if (targetConfig == null) {
                throw new IllegalArgumentException("No config chosen");
            }

            return targetConfig;
        }

        /**
         * 选择合适的配置
         * @param egl10
         * @param eglDisplay
         * @param eglConfigs
         * @return
         */
        public abstract EGLConfig chooseConfig(EGL10 egl10, EGLDisplay eglDisplay, EGLConfig[] eglConfigs);
    }


    /**
     * 配置选择器
     */
    public class SimpleEGLConfigChooser extends BaseConfigChooser{

        private int[] mValue;
        protected int mRedSize;     // 指定RGB中的R大小（bits）
        protected int mGreenSize;   // 指定G大小
        protected int mBlueSize;    // 指定B大小
        protected int mAlphaSize;   // 指定Alpha大小，以上四项实际上指定了像素格式
        protected int mDepthSize;   // 指定深度缓存(Z Buffer)大小 0或者16
        protected int mStencilSize; // 指定模板大小

        public SimpleEGLConfigChooser(int redSize,
                                    int greenSize,
                                    int blueSize,
                                    int alphaSize,
                                    int depthSize,
                                    int stencilSize){
            super(new int[]{
                    EGL10.EGL_RED_SIZE, redSize,
                    EGL10.EGL_GREEN_SIZE, greenSize,
                    EGL10.EGL_BLUE_SIZE, blueSize,
                    EGL10.EGL_ALPHA_SIZE, alphaSize,
                    EGL10.EGL_DEPTH_SIZE, depthSize,
                    EGL10.EGL_STENCIL_SIZE, stencilSize,
                    EGL10.EGL_NONE  // 总是以EGL10.EGL_NONE结尾
            });
            mConfigSpec = filterConfigSpec2(mConfigSpec);

            mValue = new int[1];
            mRedSize = redSize;
            mGreenSize = greenSize;
            mBlueSize = blueSize;
            mAlphaSize = alphaSize;
            mDepthSize = depthSize;
            mStencilSize = stencilSize;
        }

        public SimpleEGLConfigChooser(int[] configSpec) {
            super(configSpec);
            mConfigSpec = filterConfigSpec2(mConfigSpec);
        }

        @Override
        public EGLConfig chooseConfig(EGL10 egl10, EGLDisplay eglDisplay, EGLConfig[] eglConfigs) {
            // 遍历可能的 配置数组
            for (EGLConfig config : eglConfigs) {
                int d = findConfigAttrib(egl10, eglDisplay, config, EGL10.EGL_DEPTH_SIZE, 0);
                int s = findConfigAttrib(egl10, eglDisplay, config, EGL10.EGL_STENCIL_SIZE, 0);
                // 深度 和模板值大
                if ((d >= mDepthSize) && (s >= mStencilSize)) {
                    int r = findConfigAttrib(egl10, eglDisplay, config,
                            EGL10.EGL_RED_SIZE, 0);
                    int g = findConfigAttrib(egl10, eglDisplay, config,
                            EGL10.EGL_GREEN_SIZE, 0);
                    int b = findConfigAttrib(egl10, eglDisplay, config,
                            EGL10.EGL_BLUE_SIZE, 0);
                    int a = findConfigAttrib(egl10, eglDisplay, config,
                            EGL10.EGL_ALPHA_SIZE, 0);
                    // rgba 值相等的 配置 ,其实不用这么准确，选直接选eglConfigs[0] 就可以
                    if ((r == mRedSize)
                            && (g == mGreenSize)
                            && (b == mBlueSize)
                            && (a == mAlphaSize)) {

                        return config;
                    }
                }
            }
            return null;
        }

        /**
         * 从配置中 获取属性值
         * @param egl
         * @param display
         * @param config
         * @param attribute
         * @param defaultValue
         * @return
         */
        private int findConfigAttrib(EGL10 egl,
                                     EGLDisplay display,
                                     EGLConfig config,
                                     int attribute,
                                     int defaultValue) {

            if (egl.eglGetConfigAttrib(display, config, attribute, mValue)) {
                return mValue[0];
            }
            return defaultValue;
        }

        /**
         * 可以对属性列表  添加其他属性
         * @param configSpec
         * @return
         */
        protected int[] filterConfigSpec2(int[] configSpec) {
            return mConfigSpec;
        }
    }

    /**
     * Surface 工厂
     */
    public interface EGLSurfaceFactory {

        int SURFACE_PBUFFER = 1;
        int SURFACE_PIM = 2;
        int SURFACE_WINDOW = 3;

        /**
         * 创建 surface
         * @param egl10
         * @param eglDisplay
         * @param eglConfig
         * @param nativeWindow  创建Surface 的承载对象，{@link #SURFACE_WINDOW} 为SurfaceHolder，其他为null
         * @param attrList
         * @return null if the surface cannot be constructed.
         */
        EGLSurface createSurface(EGL10 egl10,
                                 EGLDisplay eglDisplay,
                                 EGLConfig eglConfig,
                                 Object nativeWindow,
                                 int[] attrList);

        /**
         * 销毁 surface
         * @param egl10
         * @param eglDisplay
         * @param eglSurface
         */
        void destroySurface(EGL10 egl10, EGLDisplay eglDisplay, EGLSurface eglSurface);
    }

    /**
     * Egl Surface 工厂
     */
    public class SimpleEglSurfaceFactory implements EGLSurfaceFactory {

        public int mSurfaceType;

        public SimpleEglSurfaceFactory(int surfaceType) {
            this.mSurfaceType = surfaceType;
        }

        @Override
        public EGLSurface createSurface(EGL10 egl10,
                                        EGLDisplay eglDisplay,
                                        EGLConfig eglConfig,
                                        Object surface_native_obj,
                                        int[] attrList) {

            switch (mSurfaceType){
                case SURFACE_WINDOW:

                    if (!(surface_native_obj instanceof SurfaceView)
                            && !(surface_native_obj instanceof Surface)
                            && !(surface_native_obj instanceof SurfaceHolder)
                            && !(surface_native_obj instanceof SurfaceTexture)) {
                        throw new IllegalArgumentException(mSurfaceType + " = SURFACE_WINDOW " + " unsupported " + "surface");
                    }

                    return egl10.eglCreateWindowSurface(eglDisplay,eglConfig,surface_native_obj,attrList);
                case SURFACE_PIM:

                    if (!(surface_native_obj instanceof SurfaceView)
                            && !(surface_native_obj instanceof Surface)
                            && !(surface_native_obj instanceof SurfaceHolder)
                            && !(surface_native_obj instanceof SurfaceTexture)) {
                        throw new IllegalArgumentException(mSurfaceType + " = SURFACE_WINDOW " + "unsupported surface");
                    }

                    return egl10.eglCreatePixmapSurface(eglDisplay,eglConfig,surface_native_obj,attrList);
                default:
                    return egl10.eglCreatePbufferSurface(eglDisplay,eglConfig,attrList);
            }
        }

        @Override
        public void destroySurface(EGL10 egl10, EGLDisplay eglDisplay, EGLSurface eglSurface) {
            egl10.eglDestroySurface(eglDisplay, eglSurface);
        }
    }


    public interface EGLContextFactory {
        /**
         * 创建 EGLContext
         * @param egl10
         * @param eglDisplay
         * @param eglConfig
         * @return
         */
        EGLContext createContext(EGL10 egl10, EGLDisplay eglDisplay, EGLConfig eglConfig);

        /**
         * 销毁 EGLContext
         * @param egl10
         * @param eglDisplay
         * @param eglContext
         */
        void destroyContext(EGL10 egl10, EGLDisplay eglDisplay, EGLContext eglContext);
    }

    /**
     * 默认 EGLContext工厂
     */
    public class SimpleEGLContextFactory implements  EGLContextFactory {
        private int EGL_CONTEXT_CLIENT_VERSION = 0x3098;
        @Override
        public EGLContext createContext(EGL10 egl10, EGLDisplay eglDisplay, EGLConfig eglConfig) {
            int[] attrib_list = {
                    EGL_CONTEXT_CLIENT_VERSION, mEGLContextClientVersion,
                    EGL10.EGL_NONE };

            return egl10.eglCreateContext(eglDisplay, eglConfig, EGL10.EGL_NO_CONTEXT, attrib_list);
        }

        @Override
        public void destroyContext(EGL10 egl10, EGLDisplay eglDisplay, EGLContext eglContext) {
            // 销毁
            if (!egl10.eglDestroyContext(eglDisplay, eglContext)) {
                Log.e("DefaultContextFactory", "display:" + eglDisplay + " context: " + eglContext);
                throwEglException("eglDestroyContex", egl10.eglGetError());
            }
        }
    }

    /**
     * 抛出 EGL异常
     * @param function
     * @param error
     */
    public void throwEglException(String function, int error) {
        String message = formatEglError(function, error);
        Log.e("EglHelper", "throwEglException tid=" + Thread.currentThread().getId() + " " + message);
        throw new RuntimeException(message);
    }

    /**
     * 格式化 egl错误
     * @param function
     * @param error
     * @return
     */
    public String formatEglError(String function, int error) {
        return function + " failed: " + getErrorString(error);
    }

    /**
     * @param error
     * @return
     */
    public static String getErrorString(int error) {
        switch (error) {
            case EGL_SUCCESS:
                return "EGL_SUCCESS";
            case EGL_NOT_INITIALIZED:
                return "EGL_NOT_INITIALIZED";
            case EGL_BAD_ACCESS:
                return "EGL_BAD_ACCESS";
            case EGL_BAD_ALLOC:
                return "EGL_BAD_ALLOC";
            case EGL_BAD_ATTRIBUTE:
                return "EGL_BAD_ATTRIBUTE";
            case EGL_BAD_CONFIG:
                return "EGL_BAD_CONFIG";
            case EGL_BAD_CONTEXT:
                return "EGL_BAD_CONTEXT";
            case EGL_BAD_CURRENT_SURFACE:
                return "EGL_BAD_CURRENT_SURFACE";
            case EGL_BAD_DISPLAY:
                return "EGL_BAD_DISPLAY";
            case EGL_BAD_MATCH:
                return "EGL_BAD_MATCH";
            case EGL_BAD_NATIVE_PIXMAP:
                return "EGL_BAD_NATIVE_PIXMAP";
            case EGL_BAD_NATIVE_WINDOW:
                return "EGL_BAD_NATIVE_WINDOW";
            case EGL_BAD_PARAMETER:
                return "EGL_BAD_PARAMETER";
            case EGL_BAD_SURFACE:
                return "EGL_BAD_SURFACE";
            case EGL11.EGL_CONTEXT_LOST:
                return "EGL_CONTEXT_LOST";
            default:
                return getHex(error);
        }
    }

    private static String getHex(int value) {
        return "0x" + Integer.toHexString(value);
    }












}
