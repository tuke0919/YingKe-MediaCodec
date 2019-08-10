attribute vec4 position;
attribute vec4 inputTextureCoordinate;

// 纹理变换矩阵
uniform mat4 textureTransform;
// 传递给 片元着色器 纹理坐标
varying vec2 textureCoordinate;

void main()
{
	textureCoordinate = (textureTransform * inputTextureCoordinate).xy;
	gl_Position = position;
}
