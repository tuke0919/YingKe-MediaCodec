package com.yingke.mediacodec.transcode.opengl;

import android.graphics.SurfaceTexture;
import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLExt;
import android.opengl.EGLSurface;
import android.view.Surface;

import com.yingke.mediacodec.player.PlayerLog;
import com.yingke.mediacodec.transcode.MediaCodecTransCoder;

import java.nio.ByteBuffer;

public class CodecInputSurface implements SurfaceTexture.OnFrameAvailableListener {
    private static final int EGL_RECORDABLE_ANDROID = 0x3142;

    private EGLDisplay mEGLDisplay = EGL14.EGL_NO_DISPLAY;
    private EGLContext mEGLContext = EGL14.EGL_NO_CONTEXT;
    private EGLSurface mEGLSurface = EGL14.EGL_NO_SURFACE;

    // 编码器输入Surface
    private Surface mEncoderInputSurface;

    // 解码器输出图像流
    private SurfaceTexture mSurfaceTexture;
    // 解码器输出Surface
    private Surface mDecoderOutputSurface;

    // 图像帧同步对象
    private final Object mFrameSyncObject = new Object();
    private boolean mFrameAvailable;

    private ByteBuffer mPixelBuf;
    // Open GL 渲染器
    private TextureRenderer mTextureRender;

    /**
     * Creates a CodecInputSurface from a Surface.
     */
    public CodecInputSurface(Surface surface) {
        PlayerLog.e(MediaCodecTransCoder.TAG, "----------CodecInputSurface constructor--------");

        if (surface == null) {
            throw new NullPointerException();
        }

        mEncoderInputSurface = surface;
        eglSetup();

    }

    /**
     * 创建opengl 渲染器
     */
    public void createRender() {
        PlayerLog.e(MediaCodecTransCoder.TAG, "----------createRender--------");

        mTextureRender = new TextureRenderer();
        mTextureRender.surfaceCreated();
        mSurfaceTexture = new SurfaceTexture(mTextureRender.getTextureId());
        mSurfaceTexture.setOnFrameAvailableListener(this);
        // 承载图像流的Surface，作为解码器的输出，解码器输出绘制在此Surface
        mDecoderOutputSurface = new Surface(mSurfaceTexture);
    }

    /**
     * Prepares EGL.  We want a GLES 2.0 context and a surface that supports recording.
     */
    private void eglSetup() {
        PlayerLog.e(MediaCodecTransCoder.TAG, "----------eglSetup--------");


        mEGLDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
        if (mEGLDisplay == EGL14.EGL_NO_DISPLAY) {
            throw new RuntimeException("unable to get EGL14 display");
        }
        int[] version = new int[2];
        if (!EGL14.eglInitialize(mEGLDisplay, version, 0, version, 1)) {
            throw new RuntimeException("unable to initialize EGL14");
        }

        // Configure EGL for recording and OpenGL ES 2.0.
        int[] attribList = {
                EGL14.EGL_RED_SIZE, 8,
                EGL14.EGL_GREEN_SIZE, 8,
                EGL14.EGL_BLUE_SIZE, 8,
                EGL14.EGL_ALPHA_SIZE, 8,
                EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                EGL_RECORDABLE_ANDROID, 1,
                EGL14.EGL_NONE
        };
        EGLConfig[] configs = new EGLConfig[1];
        int[] numConfigs = new int[1];
        EGL14.eglChooseConfig(mEGLDisplay, attribList, 0, configs, 0, configs.length,
                numConfigs, 0);
        checkEglError("eglCreateContext RGB888+recordable ES2");

        // Configure context for OpenGL ES 2.0.
        int[] attrib_list = {
                EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
                EGL14.EGL_NONE
        };
        mEGLContext = EGL14.eglCreateContext(mEGLDisplay, configs[0], EGL14.EGL_NO_CONTEXT,
                attrib_list, 0);
        checkEglError("eglCreateContext");

        // Create a window surface, and attach it to the Surface we received.
        int[] surfaceAttribs = {
                EGL14.EGL_NONE
        };

        // 通过Surface 创建在在EGL环境下的EGLSurface，以后使用的 Open Gl绘制纹理图，就是在这个EGLSurface上绘制，相当于绘制到 参数Surface上
        mEGLSurface = EGL14.eglCreateWindowSurface(mEGLDisplay, configs[0], mEncoderInputSurface, surfaceAttribs, 0);
        checkEglError("eglCreateWindowSurface");
    }

    public SurfaceTexture getSurfaceTexture() {
        return mSurfaceTexture;
    }

    public void changeFragmentShader(String fragmentShader) {
        mTextureRender.changeFragmentShader(fragmentShader);
    }


    /**
     *
     */
    public void awaitNewImage() {
        PlayerLog.e(MediaCodecTransCoder.TAG, "----------awaitNewImage--------");

        final int TIMEOUT_MS = 5000;
        synchronized (mFrameSyncObject) {
            // 如果帧不可用
            while (!mFrameAvailable) {
                try {
                    PlayerLog.e(MediaCodecTransCoder.TAG, "----------awaitNewImage wait-------- mFrameAvailable = " + mFrameAvailable);

                    // 等待5s，当前线程挂起，释放锁
                    mFrameSyncObject.wait(TIMEOUT_MS);
                    if (!mFrameAvailable) {
                        throw new RuntimeException("Surface frame wait timed out");
                    }
                } catch (InterruptedException ie) {
                    throw new RuntimeException(ie);
                }
            }
            mFrameAvailable = false;
        }
        PlayerLog.e(MediaCodecTransCoder.TAG, "----------awaitNewImage updateTexImage-------- mFrameAvailable = " + mFrameAvailable);

        mTextureRender.checkGlError("before updateTexImage");
        // 从图像流将纹理图像更新为最新帧
        mSurfaceTexture.updateTexImage();
    }

    /**
     * opengl 画纹理图像Id到Surface
     */
    public void drawImage() {
        PlayerLog.e(MediaCodecTransCoder.TAG, "----------drawImage--------");
        mTextureRender.drawFrame(mSurfaceTexture);
    }

    @Override
    public void onFrameAvailable(SurfaceTexture st) {
        synchronized (mFrameSyncObject) {
            if (mFrameAvailable) {
                throw new RuntimeException("mFrameAvailable already set, frame could be dropped");
            }
            // 帧可用
            mFrameAvailable = true;

            PlayerLog.e(MediaCodecTransCoder.TAG, "----------onFrameAvailable notifyAll -------- mFrameAvailable = " + mFrameAvailable);
            mFrameSyncObject.notifyAll();
        }
    }

    public Surface getSurface() {
        return mDecoderOutputSurface;
    }

    /**
     * Discards all resources held by this class, notably the EGL context.  Also releases the
     * Surface that was passed to our constructor.
     */
    public void release() {
        if (mEGLDisplay != EGL14.EGL_NO_DISPLAY) {
            EGL14.eglMakeCurrent(mEGLDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE,
                    EGL14.EGL_NO_CONTEXT);
            EGL14.eglDestroySurface(mEGLDisplay, mEGLSurface);
            EGL14.eglDestroyContext(mEGLDisplay, mEGLContext);
            EGL14.eglReleaseThread();
            EGL14.eglTerminate(mEGLDisplay);
        }

        mEncoderInputSurface.release();

        mEGLDisplay = EGL14.EGL_NO_DISPLAY;
        mEGLContext = EGL14.EGL_NO_CONTEXT;
        mEGLSurface = EGL14.EGL_NO_SURFACE;

        mEncoderInputSurface = null;
    }

    /**
     * Makes our EGL context and surface current.
     */
    public void makeCurrent() {
        EGL14.eglMakeCurrent(mEGLDisplay, mEGLSurface, mEGLSurface, mEGLContext);
        checkEglError("eglMakeCurrent");
    }

    /**
     * Calls eglSwapBuffers.  Use this to "publish" the current frame.
     */
    public boolean swapBuffers() {
        boolean result = EGL14.eglSwapBuffers(mEGLDisplay, mEGLSurface);
        checkEglError("eglSwapBuffers");
        return result;
    }

    /**
     * Sends the presentation time stamp to EGL.  Time is expressed in nanoseconds.
     */
    public void setPresentationTime(long nsecs) {
        PlayerLog.e(MediaCodecTransCoder.TAG, "----------setPresentationTime--------");

        EGLExt.eglPresentationTimeANDROID(mEGLDisplay, mEGLSurface, nsecs);
        checkEglError("eglPresentationTimeANDROID");
    }

    /**
     * Checks for EGL errors.  Throws an exception if one is found.
     */
    private void checkEglError(String msg) {
        int error;
        if ((error = EGL14.eglGetError()) != EGL14.EGL_SUCCESS) {
            throw new RuntimeException(msg + ": EGL error: 0x" + Integer.toHexString(error));
        }
    }
}