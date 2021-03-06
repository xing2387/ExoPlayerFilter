package com.daasuu.epf.player;

import android.graphics.Bitmap;
import android.opengl.EGL14;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.view.Surface;

import com.daasuu.epf.EglUtil;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;

import static android.opengl.GLES20.GL_FRAGMENT_SHADER;
import static android.opengl.GLES20.GL_VERTEX_SHADER;

/**
 * 创建opengl环境并渲染bitmap纹理
 */
public class GLHelper {

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
            "    gl_FragColor = texture2D( uTexture, vTextureCo);\n" +
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
            0.0f, 1.0f,
            0.0f, 0.0f,
            1.0f, 1.0f,
            1.0f, 0.0f
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


    /**
     * 创建OpenGL环境 绘制bitmap纹理
     *
     * @param bitmap
     */
    public void drawToBitmap(Bitmap bitmap) {
        //清空屏幕
        GLES20.glClearColor(1, 1, 1, 1);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        initBuffer();

        vertexShader = EglUtil.loadShader(mVertex, GL_VERTEX_SHADER);
        fragmentShader = EglUtil.loadShader(mFragment, GL_FRAGMENT_SHADER);
        int mGLProgram = EglUtil.createProgram(vertexShader, fragmentShader);
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


    /**
     * 向surfaceview中绘制bitmap
     * <p>
     * EGL是介于诸如OpenGL 或OpenVG的Khronos渲染API与底层本地平台窗口系统的接口。
     * 它被用于处理图形管理、表面/缓冲捆绑、渲染同步及支援使用其他Khronos API进行的高效、加速、混合模式2D和3D渲染。
     */
    public static void drawBitmapTOSurface(Surface surface, Bitmap bitmap) {

        //1. 取得EGL实例
        EGL10 egl = (EGL10) EGLContext.getEGL();
        //2. 选择Display
        EGLDisplay display = egl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);
        egl.eglInitialize(display, null);

        int[] attribList = {
                EGL10.EGL_RED_SIZE, 8,
                EGL10.EGL_GREEN_SIZE, 8,
                EGL10.EGL_BLUE_SIZE, 8,
                EGL10.EGL_ALPHA_SIZE, 8,
                EGL10.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                EGL10.EGL_NONE, 0,      // placeholder for recordable [@-3]
                EGL10.EGL_NONE
        };
        //3. 选择Config
        EGLConfig[] configs = new EGLConfig[1];
        int[] numConfigs = new int[1];
        egl.eglChooseConfig(display, attribList, configs, configs.length, numConfigs);
        EGLConfig config = configs[0];

        //4. 创建Surface
        EGLSurface eglSurface = egl.eglCreateWindowSurface(display, config, surface,
                new int[]{
                        EGL14.EGL_NONE
                });
        //5. 创建Context
        EGLContext context = egl.eglCreateContext(display, config, EGL10.EGL_NO_CONTEXT, new int[]{
                EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
                EGL10.EGL_NONE
        });
        egl.eglMakeCurrent(display, eglSurface, eglSurface, context);

        //创建OpenGL环境并绘制bitmap纹理
        new GLHelper().drawToBitmap(bitmap);


        //6. 指定当前的环境为绘制环境
        egl.eglSwapBuffers(display, eglSurface);
        egl.eglDestroySurface(display, eglSurface);
        egl.eglMakeCurrent(display, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_SURFACE,
                EGL10.EGL_NO_CONTEXT);
        egl.eglDestroyContext(display, context);
        egl.eglTerminate(display);
    }
}