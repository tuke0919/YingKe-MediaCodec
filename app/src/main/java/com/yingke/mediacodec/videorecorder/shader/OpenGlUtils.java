package com.yingke.mediacodec.videorecorder.shader;

import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import javax.microedition.khronos.opengles.GL10;

/**
 * 功能：
 * </p>
 * <p>Copyright corp.netease.com 2018 All right reserved </p>
 *
 * @author tuke 时间 2019/7/7
 * @email tuke@corp.netease.com
 * <p>
 * 最后修改人：无
 * <p>
 */
public class OpenGlUtils {

    private static final String TAG = "OpenGlUtils";

    public static final int NO_TEXTURE = -1;
    public static final int NOT_INIT = -1;
    public static final int ON_DRAWN = 1;

    /**
     * 获取上下文
     * @return
     */
    public static Context getContext() {
        checkContext();
        return OpenGlCameraSdk.getInstance().getContext();
    }

    /**
     * 根据着色器代码 文件路径 创建主程序
     * @param res 资源对象
     * @param vertexResPath   顶点着色器 文件路径
     * @param fragmentResPath 片元着色器 文件路径
     * @return
     */
    public static int createProgram(Resources res, String vertexResPath, String fragmentResPath){
        return createProgram(loadShaderSrcFromAssetFile(res, vertexResPath), loadShaderSrcFromAssetFile(res, fragmentResPath));
    }

    /**
     * 根据 着色器代码 创建 主程序
     * @param vertexSrcCode 顶点着色器 代码
     * @param fragSrcCode   片元着色器 代码
     * @return
     */
    public static int createProgram(String vertexSrcCode, String fragSrcCode){
        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexSrcCode);
        int fragShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragSrcCode);
        if (vertexShader == 0 || fragShader == 0) {
            return 0;
        }
        return createProgram(vertexShader, fragShader);

    }


    /**
     * 绑定着色器，链接主程序
     * @param vertexShader
     * @param fragShader
     * @return
     */
    public static int createProgram(int vertexShader, int fragShader) {
        if ( vertexShader == 0 || fragShader == 0) {
            return 0;
        }

        int program = GLES20.glCreateProgram();
        // 绑定顶点着色器
        GLES20.glAttachShader(program, vertexShader);
        // 绑定片元着色器
        GLES20.glAttachShader(program, fragShader);
        // 链接主程序
        GLES20.glLinkProgram(program);

        int[] linkStatus = new int[1];
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0);
        if (linkStatus[0] == 0) {
            Log.e(TAG,"Could not compile program:" + program);
            Log.e(TAG,"GLES20 Error:"+ GLES20.glGetProgramInfoLog(program));
            GLES20.glDeleteProgram(program);
            program = 0;
        }
        return program;
    }

    /**
     * 加载源代码，编译shader
     * @param type    {@link GLES20#GL_VERTEX_SHADER, GLES20#GL_FRAGMENT_SHADER}
     * @param srcCode
     * @return
     */
    public static int loadShader(int type, String srcCode) {
        // 创建shader
        int shader = GLES20.glCreateShader(type);
        // 加载源代码
        GLES20.glShaderSource(shader, srcCode);
        // 编译shader
        GLES20.glCompileShader(shader);
        int[] compiled = new int[1];
        // 查看编译状态
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == 0) {

            Log.e(TAG,"Could not compile shader:" + shader
                    + " type = " + (type == GLES20.GL_VERTEX_SHADER ? "GL_VERTEX_SHADER" : "GL_FRAGMENT_SHADER") );
            Log.e(TAG,"GLES20 Error:"+ GLES20.glGetShaderInfoLog(shader));
            GLES20.glDeleteShader(shader);
            shader = 0;
        }
        return shader;
    }

    /**
     * 从文件中 shader 源码
     * @param resources
     * @param shaderNamePath
     * @return
     */
    public static String loadShaderSrcFromAssetFile(Resources resources, String shaderNamePath) {
        StringBuilder result=new StringBuilder();
        try{
            InputStream is=resources.getAssets().open(shaderNamePath);
            int ch;
            byte[] buffer=new byte[1024];
            while (-1!=(ch=is.read(buffer))){
                result.append(new String(buffer,0,ch));
            }
        }catch (Exception e){
            return null;
        }
//        return result.toString().replaceAll("\\r\\n","\n");
        return result.toString().replaceAll("\\r\\n","");

    }


    /**
     * 从资源文件 读 shader 源码
     * @param resourceId
     * @return
     */
    public static String readShaderFromRawResource(final int resourceId){
        checkContext();

        final InputStream inputStream = getContext().getResources().openRawResource(resourceId);
        final InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
        final BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
        String nextLine;
        final StringBuilder body = new StringBuilder();
        try{
            while ((nextLine = bufferedReader.readLine()) != null){
                body.append(nextLine);
                body.append('\n');
            }
        }
        catch (IOException e){
            return null;
        }
        return body.toString();
    }


    /**
     * 加载纹理
     * @param fileName
     * @return
     */
    public static int loadTexture(final String fileName){
        checkContext();

        final int[] textureIds = new int[1];
        GLES20.glGenTextures(1, textureIds, 0);

        if (textureIds[0] != 0){
            // Read in the resource
            final Bitmap bitmap = getImageFromAssetsFile(getContext(),fileName);
            // Bind to the texture in OpenGL
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureIds[0]);
            // Set filtering
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
            // Load the bitmap into the bound texture.
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
            // Recycle the bitmap, since its data has been loaded into OpenGL.
            bitmap.recycle();
        }

        if (textureIds[0] == 0){
            throw new RuntimeException("Error loading texture.");
        }

        return textureIds[0];
    }

    /**
     * 创建 位图纹理id
     * @param mBitmap
     * @param recycle
     * @return
     */
    public static int createBitmapTexture(Bitmap mBitmap, boolean recycle) {
        int[] texture = new int[1];
        if (mBitmap != null && !mBitmap.isRecycled()) {
            // 生成纹理，得到纹理id
            GLES20.glGenTextures(1, texture, 0);
            // 绑定纹理id
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture[0]);
            // 设置纹理参数

            // 设置最小过滤器 为 最近采样： 使用纹理坐标最接近的颜色作为需要绘制的颜色
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
            // 设置最大功过滤器 为 线性采样器：使用纹理坐标 附近的若干个颜色，加权平均 得到需要绘制的颜色
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);

            // 设置环绕方向S，截取纹理坐标到[1/2n,1-1/2n]。将导致永远不会与border融合
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
            // 设置环绕方向T，截取纹理坐标到[1/2n,1-1/2n]。将导致永远不会与border融合
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
            // 根据以上指定的参数，生成一个2D纹理
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, mBitmap, 0);

            if (recycle) {
                mBitmap.recycle();
            }
        }

        return texture[0];
    }


    /**
     * @param context
     * @param fileName
     * @return
     */
    private static Bitmap getImageFromAssetsFile(Context context, String fileName){
        Bitmap image = null;
        AssetManager am = context.getResources().getAssets();
        try{
            InputStream is = am.open(fileName);
            image = BitmapFactory.decodeStream(is);
            is.close();
        }catch (IOException e){
            e.printStackTrace();
        }
        return image;
    }

    /**
     *
     * @return 获取扩展EOS纹理id 承载{@link SurfaceTexture} 图像流 每帧数据的纹理id
     */
    public static int getExternalOESTextureID(){
        int[] texture = new int[1];
        GLES20.glGenTextures(1, texture, 0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, texture[0]);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_LINEAR);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE);
        return texture[0];
    }


    /**
     * 检查上下文
     */
    public static void checkContext() {
        if (OpenGlCameraSdk.getInstance().getContext() == null) {
            throw new RuntimeException("OpenGlCameraSdk's Context is null!" +
                    " use OpenGlCameraSdk.getInstance().init(this) in application");
        }
    }





}
