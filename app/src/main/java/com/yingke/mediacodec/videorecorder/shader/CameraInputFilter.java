package com.yingke.mediacodec.videorecorder.shader;

import android.opengl.GLES11Ext;
import android.opengl.GLES20;


import com.yingke.mediacodec.R;

import java.nio.FloatBuffer;

/**
 * 相机输入着色器：实现将Camera硬件传回的图像 渲染到 GLSurfaceView
 */
public class CameraInputFilter extends GPUImageFilter {

    // 纹理坐标 变换矩阵
    private float[] mTextureTransformMatrix;
    // 纹理坐标 变换矩阵 的引用
    private int mTextureTransformMatrixLocation;



    // 帧缓冲对象FBO
    private int[] mFrameBuffers = null;
    // 附着帧缓冲 的 纹理id
    private int[] mFrameBufferTextures = null;
    // 帧缓冲宽
    private int mFrameWidth = -1;
    // 帧缓冲高
    private int mFrameHeight = -1;


    public CameraInputFilter(){
        // 顶点着色器 和 片元着色器
        super(OpenGlUtils.readShaderFromRawResource( R.raw.camera_input_vertex),
                OpenGlUtils.readShaderFromRawResource( R.raw.camera_input_fragment));
    }

    @Override
    protected void onInit() {
        super.onInit();
        // 获取 引用
        mTextureTransformMatrixLocation = GLES20.glGetUniformLocation(mGLProgId, "textureTransform");

    }

    /**
     * 设置 纹理坐标 变换矩阵
     * @param mtx
     */
    public void setTextureTransformMatrix(float[] mtx){
        mTextureTransformMatrix = mtx;
    }

    @Override
    public void onInputSizeChanged(final int width, final int height) {
        super.onInputSizeChanged(width, height);
    }


    @Override
    public int onDrawFrame(int textureId) {
        GLES20.glUseProgram(mGLProgId);
        runPendingOnDrawTasks();
        if(!isInitialized()) {
            return OpenGlUtils.NOT_INIT;
        }
        mGLCubeBuffer.position(0);
        GLES20.glVertexAttribPointer(mGLAttribPosition, 2, GLES20.GL_FLOAT, false, 0, mGLCubeBuffer);
        GLES20.glEnableVertexAttribArray(mGLAttribPosition);
        mGLTextureBuffer.position(0);
        GLES20.glVertexAttribPointer(mGLAttribTextureCoordinate, 2, GLES20.GL_FLOAT, false, 0, mGLTextureBuffer);
        GLES20.glEnableVertexAttribArray(mGLAttribTextureCoordinate);
        GLES20.glUniformMatrix4fv(mTextureTransformMatrixLocation, 1, false, mTextureTransformMatrix, 0);

        if(textureId != OpenGlUtils.NO_TEXTURE){
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            // 需要绑定 扩展OES 纹理
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId);
            GLES20.glUniform1i(mGLUniformTexture, 0);
        }

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        GLES20.glDisableVertexAttribArray(mGLAttribPosition);
        GLES20.glDisableVertexAttribArray(mGLAttribTextureCoordinate);
        // 解除 绑定 扩展OES 纹理
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0);
        return OpenGlUtils.ON_DRAWN;
    }

    @Override
    public int onDrawFrame(int textureId, FloatBuffer vertexBuffer, FloatBuffer textureBuffer) {
        GLES20.glUseProgram(mGLProgId);
        runPendingOnDrawTasks();
        if(!isInitialized()) {
            return OpenGlUtils.NOT_INIT;
        }
        vertexBuffer.position(0);
        GLES20.glVertexAttribPointer(mGLAttribPosition, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer);
        GLES20.glEnableVertexAttribArray(mGLAttribPosition);
        textureBuffer.position(0);
        GLES20.glVertexAttribPointer(mGLAttribTextureCoordinate, 2, GLES20.GL_FLOAT, false, 0, textureBuffer);
        GLES20.glEnableVertexAttribArray(mGLAttribTextureCoordinate);
        GLES20.glUniformMatrix4fv(mTextureTransformMatrixLocation, 1, false, mTextureTransformMatrix, 0);

        if(textureId != OpenGlUtils.NO_TEXTURE){
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            // 需要绑定 扩展OES 纹理
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId);
            GLES20.glUniform1i(mGLUniformTexture, 0);
        }

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        GLES20.glDisableVertexAttribArray(mGLAttribPosition);
        GLES20.glDisableVertexAttribArray(mGLAttribTextureCoordinate);
        // 解除 绑定 扩展OES 纹理
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0);
        return OpenGlUtils.ON_DRAWN;
    }


    /**
     * 创建帧缓冲FBO
     * @param width
     * @param height
     */
    public void initCameraFrameBuffer(int width, int height) {

        if(mFrameBuffers != null && (mFrameWidth != width || mFrameHeight != height))
            // 销毁帧缓冲对象
            destroyFrameBuffers();

        if (mFrameBuffers == null) {
            mFrameWidth = width;
            mFrameHeight = height;
            // 帧缓冲对象FBO
            mFrameBuffers = new int[1];
            // 纹理id
            mFrameBufferTextures = new int[1];

            // 创建FBO
            GLES20.glGenFramebuffers(1, mFrameBuffers, 0);

            // 这里可否绑定 FBO？??

            // 创建 纹理
            GLES20.glGenTextures(1, mFrameBufferTextures, 0);
            // 绑定纹理
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mFrameBufferTextures[0]);
            // 设置FBO分配内存大小，只是分配大小，此时纹理id 并没有图像数据
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width, height, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
            // 设置纹理参数
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

            // 绑定FBO，可否提到前面
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBuffers[0]);

            // 把纹理附着到FBO 的颜色附着点，并没有图像数据
            GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER,
                    GLES20.GL_COLOR_ATTACHMENT0,
                    GLES20.GL_TEXTURE_2D,
                    mFrameBufferTextures[0],
                    0);
            // 解绑纹理
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);

            // 解绑FBO
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        }
    }

    /**
     * 渲染到帧缓冲FBO
     * @param textureId 纹理id
     * @return 附着帧缓冲 的 纹理id
     */
    public int onDrawToTexture(final int textureId) {
        if(mFrameBuffers == null) {
            return OpenGlUtils.NO_TEXTURE;
        }

        runPendingOnDrawTasks();
        //
        GLES20.glViewport(0, 0, mFrameWidth, mFrameHeight);
        // 绑定 帧缓冲，下面的渲染操作 都在帧缓冲中
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBuffers[0]);

        // ######帧缓冲使用开始：下面的绘图方式不变 ###########

        // 使用主程序
        GLES20.glUseProgram(mGLProgId);
        if(!isInitialized()) {
            return OpenGlUtils.NOT_INIT;
        }
        // 赋值 顶点缓冲 和 纹理缓冲
        mGLCubeBuffer.position(0);
        GLES20.glVertexAttribPointer(mGLAttribPosition, 2, GLES20.GL_FLOAT, false, 0, mGLCubeBuffer);
        GLES20.glEnableVertexAttribArray(mGLAttribPosition);
        mGLTextureBuffer.position(0);
        GLES20.glVertexAttribPointer(mGLAttribTextureCoordinate, 2, GLES20.GL_FLOAT, false, 0, mGLTextureBuffer);
        GLES20.glEnableVertexAttribArray(mGLAttribTextureCoordinate);

        // 赋值 纹理坐标 变换矩阵
        GLES20.glUniformMatrix4fv(mTextureTransformMatrixLocation, 1, false, mTextureTransformMatrix, 0);

        if(textureId != OpenGlUtils.NO_TEXTURE){
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            // 需要绑定 扩展OES 纹理
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId);
            GLES20.glUniform1i(mGLUniformTexture, 0);
        }
        // 开始绘图
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        GLES20.glDisableVertexAttribArray(mGLAttribPosition);
        GLES20.glDisableVertexAttribArray(mGLAttribTextureCoordinate);

        // 解绑 扩展oes 纹理
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0);

        // ######帧缓冲使用结束 ###########

        // 解绑 帧缓冲
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);

        GLES20.glViewport(0, 0, mOutputWidth, mOutputHeight);

        // 返回 附着帧缓冲 的 纹理id，此时有图像数据了
        return mFrameBufferTextures[0];
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        destroyFrameBuffers();
    }

    /**
     * 销毁纹理和FBO
     */
    public void destroyFrameBuffers() {
        if (mFrameBufferTextures != null) {
            GLES20.glDeleteTextures(1, mFrameBufferTextures, 0);
            mFrameBufferTextures = null;
        }
        if (mFrameBuffers != null) {
            GLES20.glDeleteFramebuffers(1, mFrameBuffers, 0);
            mFrameBuffers = null;
        }
        mFrameWidth = -1;
        mFrameHeight = -1;
    }

}