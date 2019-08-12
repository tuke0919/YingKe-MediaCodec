package com.yingke.mediacodec.videorecorder.encoder;

import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.text.TextUtils;
import android.view.Surface;
import android.view.SurfaceHolder;

import com.yingke.mediacodec.videorecorder.glsurface.RecordEGLHelper;
import com.yingke.mediacodec.videorecorder.shader.CameraInputFilter;
import com.yingke.mediacodec.videorecorder.shader.OpenGlUtils;

import java.nio.Buffer;
import java.nio.FloatBuffer;

/**
 * 功能：绘制Surface线程，将camera图像帧绘制到 视频编码器的输入Surface上，就是在向编码器写数据
 * </p>
 * <p>Copyright corp.netease.com 2018 All right reserved </p>
 *
 * @author tuke 时间 2019/8/11
 * @email tuke@corp.netease.com
 * <p>
 * 最后修改人：无
 * <p>
 */
public class RecordSurfaceRender implements Runnable {

    private static final String TAG = RecordSurfaceRender.class.getSimpleName();

    private final Object mSync = new Object();

    private int mSurfaceWidth;
    private int mSurfaceHeight;

    // 编码器输入Surface，渲染到这里
    private Object mMediaCodecInputSurface;
    // 纹理id，是相机的SurfaceTexture的内存
    private int mTextureId = OpenGlUtils.NO_TEXTURE;
    // 顶点坐标
    private FloatBuffer mGLCubeBuffer;
    // 纹理坐标
    private FloatBuffer mGLTextureBuffer;


    // 是否请求设置Egl环境
    private boolean mRequestSetEglContext;

    // 是否需要释放资源
    private boolean mRequestRelease;

    // 需要绘制的次数
    private int mRequestDraw;

    private RecordEGLHelper mEGLHelper;
    private CameraInputFilter mInputFilter;


    /**
     * 创建线程,开启这个Runable
     * @param name
     * @return
     */
    public static final RecordSurfaceRender createSurfaceRenderTask(final String name) {

        final RecordSurfaceRender surfaceRender = new RecordSurfaceRender();
        synchronized (surfaceRender.mSync) {

            // 创建线程，并开启任务
            new Thread(surfaceRender, !TextUtils.isEmpty(name) ? name : TAG).start();
            try {
                // 当前线程等待
                surfaceRender.mSync.wait();
            } catch (final InterruptedException e) {
            }
        }
        return surfaceRender;
    }

    /**
     * 开始录制时，调用该方法,设置一些数据
     *
     * @param surfaceWidth
     * @param surfaceHeight
     * @param surface
     */
    public final void setEglContext(int surfaceWidth,
                                    int surfaceHeight,
                                    final Object surface) {

        if (!(surface instanceof Surface) && !(surface instanceof SurfaceTexture) && !(surface instanceof SurfaceHolder)) {
            throw new RuntimeException("unsupported window type:" + surface);
        }

        synchronized (mSync) {
            // 释放资源
            if (mRequestRelease) {
                return;
            }
            mSurfaceWidth = surfaceWidth;
            mSurfaceHeight = surfaceHeight;
            mMediaCodecInputSurface = surface;


            mRequestSetEglContext = true;
            mSync.notifyAll();
            try {
                // 当前线程等待
                mSync.wait();
            } catch (final InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 运行在GLThread
     *
     * @param texId
     */
    public final void draw(final int texId,  FloatBuffer gLCubeBuffer, FloatBuffer gLTextureBuffer) {
        synchronized (mSync) {
            // 释放资源
            if (mRequestRelease) {
                return;
            }
            mTextureId = texId;
            mGLCubeBuffer = gLCubeBuffer;
            mGLTextureBuffer = gLTextureBuffer;

            mRequestDraw++;
            mSync.notifyAll();
        }
    }

    /**
     * 释放资源
     */
    public final void release() {
        synchronized (mSync) {
            if (mRequestRelease) {
                return;
            }
            mRequestRelease = true;
            mSync.notifyAll();
            try {
                mSync.wait();
            } catch (final InterruptedException e) {
                e.printStackTrace();
            }
        }
    }


    @Override
    public void run() {

        synchronized (mSync) {
            mRequestSetEglContext = mRequestRelease = false;
            mRequestDraw = 0;
            mSync.notifyAll();
        }

        boolean localRequestDraw;
        // 无限循环
        for (; ; ) {

            synchronized (mSync) {
                // 是否需要释放资源
                if (mRequestRelease) {
                    break;
                }

                // 请求设置Egl环境
                if (mRequestSetEglContext) {
                    mRequestSetEglContext = false;
                    internalPrepare();
                }

                // 本地请求 绘制
                localRequestDraw = mRequestDraw > 0;
                if (localRequestDraw) {
                    mRequestDraw--;
                }
            }
            if (localRequestDraw) {
                // 需要绘制
                if ((mEGLHelper != null) && mTextureId >= 0) {
                    // 清屏颜色为黑色
                    GLES20.glClearColor(0, 0, 0, 0);
                    GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);

                    // 在EGL环境中绘制帧
                    mInputFilter.onDrawFrame(mTextureId, mGLCubeBuffer, mGLTextureBuffer);
                    // 交换buffer
                    mEGLHelper.swapMyEGLBuffers();
                }
            } else {

                //-------- 暂不需要绘制，进入等待状态-----------
                synchronized (mSync) {
                    try {
                        mSync.wait();
                    } catch (final InterruptedException e) {
                        e.printStackTrace();
                        break;
                    }
                }
            }
        }
        synchronized (mSync) {
            mRequestRelease = true;
            releaseEGL();
            mSync.notifyAll();
        }

    }

    /**
     * 内部初始化EGL
     */
    private final void internalPrepare() {
        releaseEGL();
        mEGLHelper = new RecordEGLHelper(mSurfaceWidth, mSurfaceHeight, mMediaCodecInputSurface);
        mInputFilter = new CameraInputFilter();
        mMediaCodecInputSurface = null;
        mSync.notifyAll();
    }

    /**
     * 释放之前的 EGL
     */
    private final void releaseEGL() {
        if (mEGLHelper != null) {
            mEGLHelper.destroyEglContext();
            mEGLHelper = null;
        }
    }


}
