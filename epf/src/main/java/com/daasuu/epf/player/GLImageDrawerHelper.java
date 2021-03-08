package com.daasuu.epf.player;

import android.graphics.Bitmap;
import android.opengl.EGL14;
import android.opengl.GLES20;
import android.opengl.GLException;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.util.Log;
import android.view.Surface;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;

import static android.opengl.GLES20.GL_FRAGMENT_SHADER;
import static android.opengl.GLES20.GL_LINK_STATUS;
import static android.opengl.GLES20.GL_TRUE;
import static android.opengl.GLES20.GL_VERTEX_SHADER;
import static android.opengl.GLES20.glCreateProgram;

/**
 * 创建opengl环境 并渲染bitmap纹理
 */
public class GLImageDrawerHelper {
    private static final String TAG = "GLImageDrawerHelper";

    private String mVertex = "precision highp float;\n" +
            "precision highp int;\n" +
            "attribute vec4 aVertexCo;\n" +
            "attribute vec2 aTextureCo;\n" +
            "\n" +
            "uniform mat4 uVertexMatrix;\n" +
            "uniform mat4 uTextureMatrix;\n" +
            "\n" +
            "varying vec2 vTextureCo;\n" +
            "\n" +
            "void main(){\n" +
            "    gl_Position = uVertexMatrix*aVertexCo;\n" +
            "    vTextureCo = (uTextureMatrix*vec4(aTextureCo,0,1)).xy;\n" +
            "}";

    private String mFragment = "precision mediump float;\n" +
            "varying vec2 vTextureCo;\n" +
            "uniform sampler2D uTexture;\n" +
            "void main() {\n" +
            "    gl_FragColor = texture2D(uTexture, vTextureCo);\n" +
            "}";

    private int vertexShader;
    private int fragmentShader;

    protected int mGLVertexCo;
    protected int mGLTextureCo;
    protected int mGLVertexMatrix;
    protected int mGLTextureMatrix;
    protected int mGLTexture;
    private float[] mVertexMatrix = new float[]{
            1, 0, 0, 0,
            0, 1, 0, 0,
            0, 0, 1, 0,
            0, 0, 0, 1
    };
    private float[] mTextureMatrix = new float[]{
            1, 0, 0, 0,
            0, 1, 0, 0,
            0, 0, 1, 0,
            0, 0, 0, 1
    };

    protected FloatBuffer mVertexBuffer;
    protected FloatBuffer mTextureBuffer;

    public static float[] textureCo = new float[]{
            0.0f, 0.0f,
            1.0f, 0.0f,
            0.0f, 1.0f,
            1.0f, 1.0f
    };

    public static float[] vertexCo = new float[]{
            -1.0f, 1.0f, 0.0f,
            1.0f, 1.0f, 0.0f,
            -1.0f, -1.0f, 0.0f,
            1.0f, -1.0f, 0.0f
    };

    private static final int FLOAT_SIZE_BYTES = 4;

    /**
     * 初始化顶点数据
     */
    protected void initBuffer() {
        ByteBuffer vertex = ByteBuffer.allocateDirect(vertexCo.length * FLOAT_SIZE_BYTES);
        vertex.order(ByteOrder.nativeOrder());
        mVertexBuffer = vertex.asFloatBuffer();
        mVertexBuffer.put(vertexCo);
        mVertexBuffer.position(0);
        ByteBuffer texture = ByteBuffer.allocateDirect(textureCo.length * FLOAT_SIZE_BYTES);
        texture.order(ByteOrder.nativeOrder());
        mTextureBuffer = texture.asFloatBuffer();
        mTextureBuffer.put(textureCo);
        mTextureBuffer.position(0);
    }


    public static int loadShader(final String strSource, final int iType) {
        int[] compiled = new int[1];
        int iShader = GLES20.glCreateShader(iType);
        GLES20.glShaderSource(iShader, strSource);
        GLES20.glCompileShader(iShader);
        GLES20.glGetShaderiv(iShader, GLES20.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == 0) {
            Log.d("Load Shader Failed", "Compilation\n" + GLES20.glGetShaderInfoLog(iShader));
            return 0;
        }
        return iShader;
    }

    public static int createProgram(final int vertexShader, final int pixelShader) throws GLException {
        final int program = glCreateProgram();
//        checkGlError("createProgram");
        if (program == 0) {
            throw new RuntimeException("Could not create program. GLES20 error: " + GLES20.glGetError());
        }

        GLES20.glAttachShader(program, vertexShader);
        GLES20.glAttachShader(program, pixelShader);

        GLES20.glLinkProgram(program);
        final int[] linkStatus = new int[1];
        GLES20.glGetProgramiv(program, GL_LINK_STATUS, linkStatus, 0);
        if (linkStatus[0] != GL_TRUE) {
            GLES20.glDeleteProgram(program);
            throw new RuntimeException("Could not link program");
        }
        return program;
    }

    private float viewWidth;
    private float viewHeight;

    public void setViewSize(int w, int h) {
        viewWidth = w;
        viewHeight = h;
    }

    public void setBitmapSize(float bitmapW, float bitmapH) {
        float viewPortW;
        float viewPortH;
        float startX;
        float startY;
        float scale;
        if (bitmapW * viewHeight > viewWidth * bitmapH) {   //图片比view宽
            viewPortW = viewWidth;
            viewPortH = bitmapH * viewWidth / bitmapW;
            startX = 0;
            startY = (viewHeight - viewPortH) / 2;
            scale = bitmapW / viewPortW;
        } else {
            viewPortW = bitmapW * viewHeight / bitmapH;
            viewPortH = viewHeight;
            startX = (viewWidth - viewPortW) / 2;
            startY = 0;
            scale = bitmapH / viewHeight;
        }
        Log.d(TAG, "setBitmapSize: glViewport x,y,w,h = " + startX + ", " + startY + ", " + viewPortW + ", "
                + viewPortH + ", bitmap size " + bitmapW + " x " + bitmapH + ", view size " + viewWidth + ", " + viewHeight);
//        GLES20.glViewport((int) startX, (int) startY, (int) (viewPortW), (int) viewPortH);
        Matrix.setIdentityM(mTextureMatrix, 0);
        Matrix.setIdentityM(mVertexMatrix, 0);
//        Matrix.scaleM(mTextureMatrix, 0, scale, scale, 1);
    }

    /**
     * 创建OpenGL环境 绘制bitmap纹理
     *
     * @param bitmap
     */
    public void drawToBitmap(Bitmap bitmap) {
        //清空屏幕
        GLES20.glClearColor(0, 0, 0, 1);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        if (bitmap == null || bitmap.isRecycled()) {
            return;
        }
        setBitmapSize(bitmap.getWidth(), bitmap.getHeight());

        initBuffer();

        vertexShader = loadShader(mVertex, GL_VERTEX_SHADER);
        fragmentShader = loadShader(mFragment, GL_FRAGMENT_SHADER);
        int mGLProgram = createProgram(vertexShader, fragmentShader);
        mGLVertexCo = GLES20.glGetAttribLocation(mGLProgram, "aVertexCo");
        mGLTextureCo = GLES20.glGetAttribLocation(mGLProgram, "aTextureCo");
        mGLVertexMatrix = GLES20.glGetUniformLocation(mGLProgram, "uVertexMatrix");
        mGLTextureMatrix = GLES20.glGetUniformLocation(mGLProgram, "uTextureMatrix");
        mGLTexture = GLES20.glGetUniformLocation(mGLProgram, "uTexture");

        //绘制bitmap纹理texture
        int[] texture = new int[1];
        GLES20.glGenTextures(1, texture, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture[0]);
        //设置缩小过滤为使用纹理中坐标最接近的一个像素的颜色作为需要绘制的像素颜色
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        //设置放大过滤为使用纹理中坐标最接近的若干个颜色，通过加权平均算法得到需要绘制的像素颜色
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        //设置环绕方向S，截取纹理坐标到[1/2n,1-1/2n]。将导致永远不会与border融合
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        //设置环绕方向T，截取纹理坐标到[1/2n,1-1/2n]。将导致永远不会与border融合
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);

        GLES20.glUseProgram(mGLProgram);
        GLES20.glUniformMatrix4fv(mGLVertexMatrix, 1, false, mVertexMatrix, 0);
        GLES20.glUniformMatrix4fv(mGLTextureMatrix, 1, false, mTextureMatrix, 0);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture[0]);
        GLES20.glUniform1i(mGLTexture, 0);

        GLES20.glEnableVertexAttribArray(mGLVertexCo);
        GLES20.glVertexAttribPointer(mGLVertexCo, 3, GLES20.GL_FLOAT, false, 0, mVertexBuffer);
        GLES20.glEnableVertexAttribArray(mGLTextureCo);
        GLES20.glVertexAttribPointer(mGLTextureCo, 2, GLES20.GL_FLOAT, false, 0, mTextureBuffer);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        GLES20.glDisableVertexAttribArray(mGLVertexCo);
        GLES20.glDisableVertexAttribArray(mGLTextureCo);
    }

}