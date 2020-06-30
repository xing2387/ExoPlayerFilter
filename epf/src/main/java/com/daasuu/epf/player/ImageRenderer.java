package com.daasuu.epf.player;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.media.MediaCodec;
import android.opengl.EGL14;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.os.Build;
import android.util.Log;
import android.view.Surface;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.google.android.exoplayer2.BaseRenderer;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.FormatHolder;
import com.google.android.exoplayer2.decoder.DecoderInputBuffer;
import com.google.android.exoplayer2.video.VideoFrameMetadataListener;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;


public class ImageRenderer extends BaseRenderer {
    private static final String TAG = "ImageRenderer";

    public static final int TRACK_TYPE_CUSTOM_IMAGE = C.TRACK_TYPE_CUSTOM_BASE + 1;

    private int[] textures = new int[1];
    private final FormatHolder formatHolder;
    private final DecoderInputBuffer inputBuffer;
    private Surface surface;
    private final Paint paint;
    private final Matrix matrix;

    public ImageRenderer() {
        super(C.TRACK_TYPE_VIDEO);
        this.formatHolder = new FormatHolder();
        this.inputBuffer = new DecoderInputBuffer(DecoderInputBuffer.BUFFER_REPLACEMENT_MODE_NORMAL);
        this.paint = new Paint();
        this.matrix = new Matrix();
        matrix.setScale(0.5f, 0.5f);
        this.paint.setColor(Color.BLUE);
        this.paint.setTextSize(40);
    }

    @Override
    public void handleMessage(int messageType, @Nullable Object message) throws ExoPlaybackException {
        if (messageType == C.MSG_SET_SURFACE) {
            setSurface((Surface) message);
//        } else if (messageType == C.MSG_SET_SCALING_MODE) {
//            scalingMode = (Integer) message;
//            MediaCodec codec = getCodec();
//            if (codec != null) {
//                codec.setVideoScalingMode(scalingMode);
//            }
//        } else if (messageType == C.MSG_SET_VIDEO_FRAME_METADATA_LISTENER) {
//            frameMetadataListener = (VideoFrameMetadataListener) message;
        } else {
            super.handleMessage(messageType, message);
        }
    }

    private void setSurface(Surface surface) throws ExoPlaybackException {
        if (this.surface != surface) {
            this.surface = surface;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void render(long positionUs, long elapsedRealtimeUs) throws ExoPlaybackException {
        if (surface != null) {
            readSource(formatHolder, inputBuffer, false);
            Bitmap bitmap = null;
            if (inputBuffer.data != null) {
                bitmap = BitmapFactory.decodeByteArray(inputBuffer.data.array(), 0, inputBuffer.data.array().length);
            }
            if (bitmap != null) {
                Canvas canvas = surface.lockCanvas(null);
                canvas.drawBitmap(bitmap, 0, 0, paint);
                surface.unlockCanvasAndPost(canvas);
//                drawBitmapTOSurface(surface, bitmap);
            }
        }

        Log.d(TAG, "render: " + Thread.currentThread().getName());
    }

    @Override
    public boolean isReady() {
        Log.d(TAG, "isReady: " + Thread.currentThread().getName());
        return true;
    }

    @Override
    public boolean isEnded() {
        Log.d(TAG, "isEnded: " + Thread.currentThread().getName());
        return false;
    }

    @Override
    public int supportsFormat(Format format) {
        Log.d(TAG, "supportsFormat: " + Thread.currentThread().getName());
        String mimeType = format.sampleMimeType;
        if ("image/jpeg".equals(mimeType) || "image/png".equals(mimeType)) {
            return FORMAT_HANDLED;
        } else if (mimeType != null && mimeType.startsWith("image/")) {
            return FORMAT_UNSUPPORTED_SUBTYPE;
        } else {
            return FORMAT_UNSUPPORTED_TYPE;
        }
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
