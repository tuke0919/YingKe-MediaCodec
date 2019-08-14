package com.yingke.mediacodec.recorder.shader;

import android.graphics.PointF;
import android.opengl.GLES20;



import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.LinkedList;

/**
 * 功能：图片渲染着色器 的基类
 * </p>
 * <p>Copyright corp.netease.com 2018 All right reserved </p>
 *
 * @author tuke 时间 2019/7/16
 * @email tuke@corp.netease.com
 * <p>
 * 最后修改人：无
 * <p>
 */
public class GPUImageFilter {

    public static final String NO_FILTER_VERTEX_SHADER = "" +
            "attribute vec4 position;\n" +
            "attribute vec4 inputTextureCoordinate;\n" +
            " \n" +
            "varying vec2 textureCoordinate;\n" +
            " \n" +
            "void main()\n" +
            "{\n" +
            "    gl_Position = position;\n" +
            "    textureCoordinate = inputTextureCoordinate.xy;\n" +
            "}";
    public static final String NO_FILTER_FRAGMENT_SHADER = "" +
            "varying highp vec2 textureCoordinate;\n" +
            " \n" +
            "uniform sampler2D inputImageTexture;\n" +
            " \n" +
            "void main()\n" +
            "{\n" +
            "     gl_FragColor = texture2D(inputImageTexture, textureCoordinate);\n" +
            "}";


    // 其他绘制任务
    private final LinkedList<Runnable> mRunOnDraw;

    // 顶点着色器代码
    private final String mVertexShader;
    // 片元着色器代码
    private final String mFragmentShader;

    // 主程序id
    protected int mGLProgId;
    // 顶点坐标 引用
    protected int mGLAttribPosition;
    // 纹理坐标 引用
    protected int mGLAttribTextureCoordinate;
    // 纹理采样器 引用
    protected int mGLUniformTexture;

    // 输入宽
    protected int mIntputWidth;
    // 输入高
    protected int mIntputHeight;
    // 是否已经初始化
    protected boolean mIsInitialized;

    // 顶点坐标数据
    protected FloatBuffer mGLCubeBuffer;
    // 纹理坐标数据
    protected FloatBuffer mGLTextureBuffer;

    // 输出宽
    protected int mOutputWidth;
    // 输出高
    protected int mOutputHeight;

    public GPUImageFilter() {
        this(NO_FILTER_VERTEX_SHADER, NO_FILTER_FRAGMENT_SHADER);
    }

    public GPUImageFilter(final String vertexShader, final String fragmentShader) {

        mRunOnDraw = new LinkedList<>();
        mVertexShader = vertexShader;
        mFragmentShader = fragmentShader;

        // 设置顶点坐标数据
        mGLCubeBuffer = ByteBuffer.allocateDirect(TextureRotationUtil.CUBE.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        mGLCubeBuffer.put(TextureRotationUtil.CUBE).position(0);
        // 设置纹理坐标数据
        mGLTextureBuffer = ByteBuffer.allocateDirect(TextureRotationUtil.TEXTURE_NO_ROTATION.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        // 垂直翻转了一下
        mGLTextureBuffer.put(TextureRotationUtil.getRotation(Rotation.NORMAL, false, true)).position(0);
    }

    /**
     * 第一步：创建 主程序program，获取着色器变量引用
     */
    public void init() {
        onInit();
        mIsInitialized = true;
        onInitialized();
    }

    /**
     * 创建 主程序program，获取着色器变量引用
     */
    protected void onInit() {
        mGLProgId = OpenGlUtils.createProgram(mVertexShader, mFragmentShader);
        mGLAttribPosition = GLES20.glGetAttribLocation(mGLProgId, "position");
        mGLUniformTexture = GLES20.glGetUniformLocation(mGLProgId, "inputImageTexture");
        mGLAttribTextureCoordinate = GLES20.glGetAttribLocation(mGLProgId, "inputTextureCoordinate");
        mIsInitialized = true;
    }

    /**
     * 可供子类 获取其他着色器变量引用
     */
    protected void onInitialized() {

    }

    /**
     * 第二步：输入尺寸改变，图像的尺寸等
     * @param width
     * @param height
     */
    public void onInputSizeChanged(final int width, final int height) {
        mIntputWidth = width;
        mIntputHeight = height;
    }

    /**
     * 设置显示宽高，比如surface宽高
     * @param width
     * @param height
     */
    public void onDisplaySizeChanged(final int width, final int height) {
        mOutputWidth = width;
        mOutputHeight = height;
    }

    /**
     * 第三步：绘制图形，用外部提供的数 坐标数据
     * @param textureId      纹理id
     * @param cubeBuffer     顶点坐标数据
     * @param textureBuffer  片元坐标数据
     * @return
     */
    public int onDrawFrame(final int textureId,
                           final FloatBuffer cubeBuffer,
                           final FloatBuffer textureBuffer) {
        // 使用程序
        GLES20.glUseProgram(mGLProgId);
        // 执行其他任务
        runPendingOnDrawTasks();
        // 没初始化
        if (!mIsInitialized) {
            return OpenGlUtils.NOT_INIT;
        }
        // 向着色器中 顶点坐标赋值
        cubeBuffer.position(0);
        GLES20.glVertexAttribPointer(mGLAttribPosition, 2, GLES20.GL_FLOAT, false, 0, cubeBuffer);
        GLES20.glEnableVertexAttribArray(mGLAttribPosition);

        // 向着色器中 纹理坐标赋值
        textureBuffer.position(0);
        GLES20.glVertexAttribPointer(mGLAttribTextureCoordinate, 2, GLES20.GL_FLOAT, false, 0, textureBuffer);
        GLES20.glEnableVertexAttribArray(mGLAttribTextureCoordinate);

        if (textureId != OpenGlUtils.NO_TEXTURE) {
            // 启动纹理单元 0号单元
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            // 绑定纹理id
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
            // 向着色器中 纹理采样器 赋值0号纹理单元
            GLES20.glUniform1i(mGLUniformTexture, 0);
        }
        // 绘图前
        onDrawArraysPre();

        // 开始绘图
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        // 禁用 顶点坐标 和纹理坐标引用
        GLES20.glDisableVertexAttribArray(mGLAttribPosition);
        GLES20.glDisableVertexAttribArray(mGLAttribTextureCoordinate);

        // 绘图后
        onDrawArraysAfter();
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        return OpenGlUtils.ON_DRAWN;
    }

    /**
     * 第三步：绘制图形，用内部提供的数 坐标数据
     * @param textureId
     * @return
     */
    public int onDrawFrame(final int textureId) {

        GLES20.glUseProgram(mGLProgId);
        runPendingOnDrawTasks();
        if (!mIsInitialized)
            return OpenGlUtils.NOT_INIT;

        // 赋值
        mGLCubeBuffer.position(0);
        GLES20.glVertexAttribPointer(mGLAttribPosition, 2, GLES20.GL_FLOAT, false, 0, mGLCubeBuffer);
        GLES20.glEnableVertexAttribArray(mGLAttribPosition);
        mGLTextureBuffer.position(0);
        GLES20.glVertexAttribPointer(mGLAttribTextureCoordinate, 2, GLES20.GL_FLOAT, false, 0, mGLTextureBuffer);
        GLES20.glEnableVertexAttribArray(mGLAttribTextureCoordinate);

        if (textureId != OpenGlUtils.NO_TEXTURE) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
            GLES20.glUniform1i(mGLUniformTexture, 0);
        }
        onDrawArraysPre();
        // 开始绘图
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        GLES20.glDisableVertexAttribArray(mGLAttribPosition);
        GLES20.glDisableVertexAttribArray(mGLAttribTextureCoordinate);
        onDrawArraysAfter();
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        return OpenGlUtils.ON_DRAWN;
    }

    /**
     * 绘图前 供子类 赋值其他着色器变量
     */
    protected void onDrawArraysPre() {}

    /**
     * 绘图前 供子类 做销毁操作
     */
    protected void onDrawArraysAfter() {}

    /**
     * 执行其他任务,可用于设置 子类的fragment中的新 属性
     */
    protected void runPendingOnDrawTasks() {
        while (!mRunOnDraw.isEmpty()) {
            mRunOnDraw.removeFirst().run();
        }
    }

    /**
     * 销毁
     */
    public final void destroy() {
        mIsInitialized = false;
        GLES20.glDeleteProgram(mGLProgId);
        onDestroy();
    }

    /**
     * 子类销毁
     */
    protected void onDestroy() {}


    public boolean isInitialized() {
        return mIsInitialized;
    }

    public int getIntputWidth() {
        return mIntputWidth;
    }

    public int getIntputHeight() {
        return mIntputHeight;
    }

    public int getProgram() {
        return mGLProgId;
    }

    public int getAttribPosition() {
        return mGLAttribPosition;
    }

    public int getAttribTextureCoordinate() {
        return mGLAttribTextureCoordinate;
    }

    public int getUniformTexture() {
        return mGLUniformTexture;
    }

    /**
     * 赋值
     * @param location
     * @param intValue
     */
    protected void setInteger(final int location, final int intValue) {
        runOnDraw(new Runnable() {
            @Override
            public void run() {
                GLES20.glUniform1i(location, intValue);
            }
        });
    }

    /**
     * 赋值
     * @param location
     * @param floatValue
     */
    protected void setFloat(final int location, final float floatValue) {
        runOnDraw(new Runnable() {
            @Override
            public void run() {
                GLES20.glUniform1f(location, floatValue);
            }
        });
    }

    /**
     * 赋值
     * @param location
     * @param arrayValue
     */
    protected void setFloatVec2(final int location, final float[] arrayValue) {
        runOnDraw(new Runnable() {
            @Override
            public void run() {
                GLES20.glUniform2fv(location, 1, FloatBuffer.wrap(arrayValue));
            }
        });
    }

    /**
     * 赋值
     * @param location
     * @param arrayValue
     */
    protected void setFloatVec3(final int location, final float[] arrayValue) {
        runOnDraw(new Runnable() {
            @Override
            public void run() {
                GLES20.glUniform3fv(location, 1, FloatBuffer.wrap(arrayValue));
            }
        });
    }

    /**
     * 赋值
     * @param location
     * @param arrayValue
     */
    protected void setFloatVec4(final int location, final float[] arrayValue) {
        runOnDraw(new Runnable() {
            @Override
            public void run() {
                GLES20.glUniform4fv(location, 1, FloatBuffer.wrap(arrayValue));
            }
        });
    }

    /**
     * 赋值
     * @param location
     * @param arrayValue
     */
    protected void setFloatArray(final int location, final float[] arrayValue) {
        runOnDraw(new Runnable() {
            @Override
            public void run() {
                GLES20.glUniform1fv(location, arrayValue.length, FloatBuffer.wrap(arrayValue));
            }
        });
    }

    /**
     * 赋值
     * @param location
     * @param point
     */
    protected void setPoint(final int location, final PointF point) {
        runOnDraw(new Runnable() {

            @Override
            public void run() {
                float[] vec2 = new float[2];
                vec2[0] = point.x;
                vec2[1] = point.y;
                GLES20.glUniform2fv(location, 1, vec2, 0);
            }
        });
    }

    /**
     * 赋值
     * @param location
     * @param matrix
     */
    protected void setUniformMatrix3f(final int location, final float[] matrix) {
        runOnDraw(new Runnable() {

            @Override
            public void run() {
                GLES20.glUniformMatrix3fv(location, 1, false, matrix, 0);
            }
        });
    }

    /**
     * 赋值 矩阵
     * @param location
     * @param matrix
     */
    protected void setUniformMatrix4f(final int location, final float[] matrix) {
        runOnDraw(new Runnable() {

            @Override
            public void run() {
                GLES20.glUniformMatrix4fv(location, 1, false, matrix, 0);
            }
        });
    }

    /**
     * 添加任务
     * @param runnable
     */
    protected void runOnDraw(final Runnable runnable) {
        synchronized (mRunOnDraw) {
            mRunOnDraw.addLast(runnable);
        }
    }











}
