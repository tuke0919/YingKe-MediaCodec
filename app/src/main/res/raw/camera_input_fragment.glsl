#extension GL_OES_EGL_image_external : require

precision mediump float;
// 顶点着色器 传递来的纹理坐标
varying mediump vec2 textureCoordinate;

// 扩展纹理采样器
uniform samplerExternalOES inputImageTexture;


void main(){
    gl_FragColor = texture2D(inputImageTexture, textureCoordinate);
}