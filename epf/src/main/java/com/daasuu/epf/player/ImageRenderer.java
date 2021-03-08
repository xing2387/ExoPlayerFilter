package com.daasuu.epf.player;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.opengl.EGL14;
import android.opengl.GLES20;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.util.Pair;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.gif.GifDrawable;
import com.bumptech.glide.request.FutureTarget;
import com.daasuu.epf.App;
import com.google.android.exoplayer2.BaseRenderer;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.FormatHolder;
import com.google.android.exoplayer2.decoder.DecoderInputBuffer;
import com.google.android.exoplayer2.video.VideoRendererEventListener;

import java.util.LinkedList;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;

public class ImageRenderer extends BaseRenderer {
    private static final String TAG = "ljx-ImageRenderer";
    private final FormatHolder formatHolder;
    //    private final DecoderInputBuffer inputBuffer;
    private final LinkedList<BufferHolder> buffList;
    private final BufferHolder bufferHolder;
    //    private final LinkedHashMap<DecoderInputBuffer, Pair<Long, Long>> buffList = new LinkedHashMap<>(2);
    private final VideoRendererEventListener.EventDispatcher eventDispatcher;
    private Surface surface;
    private int viewH;
    private int viewW;
    private Bitmap[] bitmaps = new Bitmap[]{null, null};
    private int currBitmapIndex = -1;
    private boolean surfaceChanged;
    private boolean clearedSurface;

    public ImageRenderer(
            @Nullable Handler eventHandler,
            @Nullable VideoRendererEventListener eventListener) {
        super(C.TRACK_TYPE_VIDEO);

        this.eventDispatcher = new VideoRendererEventListener.EventDispatcher(eventHandler, eventListener);
        this.formatHolder = new FormatHolder();
        buffList = new LinkedList<>();
        DecoderInputBuffer inputBuffer = new DecoderInputBuffer(DecoderInputBuffer.BUFFER_REPLACEMENT_MODE_NORMAL);
        buffList.add(new BufferHolder(-1, -1, inputBuffer));
        inputBuffer = new DecoderInputBuffer(DecoderInputBuffer.BUFFER_REPLACEMENT_MODE_NORMAL);
        buffList.add(new BufferHolder(-1, -1, inputBuffer));

        bufferHolder = new BufferHolder(-1, -1, inputBuffer);
    }

    @Override
    public void handleMessage(int messageType, @Nullable Object message) throws ExoPlaybackException {
        if (messageType == C.MSG_SET_SURFACE) {
            setSurface((Surface) message);
        } else if (messageType == C.MSG_CUSTOM_BASE + 10) {
            Pair<Integer, Integer> size = (Pair<Integer, Integer>) message;
            onSurfaceSizeChanged(size.first, size.second);
        } else {
            super.handleMessage(messageType, message);
        }
    }

    private void setSurface(Surface surface) {
        this.surface = surface;
        releaseEgl();
    }

    private boolean isNothingToRead = false;
    private long currentImageRendererStartElapsedRealtimeUs = -1;

    private String aaa(String uri) {
        return uri.substring(uri.length() - 10, uri.length() - 4);
    }

    @Override
    protected void onStreamChanged(Format[] formats, long offsetUs) throws ExoPlaybackException {
        super.onStreamChanged(formats, offsetUs);
        ImageMediaPeriod.ImageSampleStreamImpl stream = (ImageMediaPeriod.ImageSampleStreamImpl) getStream();
        Log.d(TAG, "onStreamChanged: " + offsetUs + ", " + aaa(stream.dataSpec.uri.toString()));
        stream.startPositionUs = offsetUs;
    }

    public boolean isGif(byte[] bytes) {
        return bytes != null && bytes.length > 2 && bytes[0] == 71 && bytes[1] == 73;
    }

    @Override
    protected void onPositionReset(long positionUs, boolean joining) throws ExoPlaybackException {
        super.onPositionReset(positionUs, joining);
        Log.d(TAG, "onPositionReset: positionUs " + positionUs + ", joining " + joining);
        for (BufferHolder bufferHolder : buffList) {
            bufferHolder.hasDraw = false;
        }
        bufferHolder.hasDraw = false;
    }

    private BufferHolder getCurrentBuff(long position) {
        for (BufferHolder buffer : buffList) {
            if (position >= buffer.start && position < buffer.end) {
                return buffer;
            }
        }
        return null;
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void render(long positionUs, long elapsedRealtimeUs) throws ExoPlaybackException {
        if (surface == null || !(getStream() instanceof ImageMediaPeriod.ImageSampleStreamImpl)) {
            return;
        }
        ImageMediaPeriod.ImageSampleStreamImpl stream = (ImageMediaPeriod.ImageSampleStreamImpl) getStream();
//        BufferHolder bufferHolder = getCurrentBuff(positionUs);
//        if (bufferHolder == null) {
//            Log.d(TAG, "render: bufferHolder == null, position " + positionUs + ", stream " + stream.startPositionUs + "," + stream.durationUs);
//            bufferHolder = buffList.removeFirst();
//            readSource(formatHolder, bufferHolder.buffer, false);
//            bufferHolder.setData(stream.startPositionUs, stream.startPositionUs + stream.durationUs, bufferHolder.buffer);
//            buffList.addLast(bufferHolder);
//        }
//        if (bufferHolder.start > positionUs || bufferHolder.end <= positionUs) {
//            Log.d(TAG, "render: " + positionUs + ", " + bufferHolder.start);
        if (stream.isReady()) {
            readSource(formatHolder, bufferHolder.buffer, false);
            bufferHolder.setData(stream.startPositionUs, stream.startPositionUs + stream.durationUs, bufferHolder.buffer);
        }
//        }

        DecoderInputBuffer inputBuffer = bufferHolder.buffer;
        if (inputBuffer.data != null && !bufferHolder.hasDraw) {
            Bitmap bitmap = null;
            if (isGif(inputBuffer.data.array()) || (formatHolder.format != null && "image/gif".equals(formatHolder.format.sampleMimeType))) {
                FutureTarget<GifDrawable> glideTarget = Glide.with(App.appContext).asGif().load(inputBuffer.data.array()).submit();
                try {
                    bitmap = glideTarget.get().getFirstFrame();
                    if (bitmap != null) {
                        eventDispatcher.videoSizeChanged(bitmap.getWidth(), bitmap.getHeight(), Format.NO_VALUE, Format.NO_VALUE);
                    }
                } catch (Exception e) {
                    //ignore
                }
            } else {
                BitmapFactory.Options options = new BitmapFactory.Options();
                Log.d(TAG, "render: " + positionUs);
                if (viewH != 0 && viewW != 0) {
                    options.inJustDecodeBounds = true;
                    BitmapFactory.decodeByteArray(inputBuffer.data.array(), 0, inputBuffer.data.array().length, options);
                    float bitmapW = options.outWidth;
                    float bitmapH = options.outHeight;
                    float scale = 1;
                    if (bitmapW * viewH > viewW * bitmapH) {    //图片宽高比比view大
                        if (bitmapW > viewW) {
                            scale = bitmapW / viewW;
                        }
                    } else {
                        if (bitmapH > viewH) {
                            scale = bitmapH / viewH;
                        }
                    }
                    options.inSampleSize = (int) scale;
                    options.inJustDecodeBounds = false;
                    bitmap = BitmapFactory.decodeByteArray(inputBuffer.data.array(), 0, inputBuffer.data.array().length, options);
                    eventDispatcher.videoSizeChanged((int) bitmap.getWidth(), (int) bitmap.getHeight(), Format.NO_VALUE, Format.NO_VALUE);
                }
            }
            bufferHolder.hasDraw = true;
            if (bitmap != null) {
                updateCurrBitmap(bitmap);
                drawBitmapToSurface(bitmap);
            }
            surfaceChanged = false;
            clearedSurface = false;
        } else if (surfaceChanged) {
            if (!clearedSurface) {
                if (getCurrBitmap() != null && egl != null) {       //好像无效。。
                    GLES20.glClearColor(0, 0, 0, 1);
                    GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
                }
                clearedSurface = true;
                releaseEgl();
                tryInitEGL();
            } else {
                Log.d(TAG, "render: surfaceChanged w/h = " + viewW + "/" + viewH);
                final Bitmap bitmap = getCurrBitmap();
                if (bitmap != null && !bitmap.isRecycled()) {
                    drawBitmapToSurface(bitmap);
                }
                surfaceChanged = false;
            }
        }

    }


    private void updateCurrBitmap(@NonNull Bitmap bitmap) {
        int index = currBitmapIndex == 0 ? 1 : 0;
        if (bitmaps[index] != null && !bitmaps[index].isRecycled()) {
            bitmaps[index].recycle();
        }
        bitmaps[index] = bitmap;
        currBitmapIndex = index;
    }

    @Nullable
    private Bitmap getCurrBitmap() {
        return currBitmapIndex < 0 ? null : bitmaps[currBitmapIndex];
    }

    @Override
    protected void onDisabled() {
        super.onDisabled();
        Log.d(TAG, "onDisabled: ");
        releaseEgl();

        for (int i = 0; i < bitmaps.length; i++) {
            if (bitmaps[i] != null && !bitmaps[i].isRecycled()) {
                bitmaps[i].recycle();
            }
            bitmaps[i] = null;
        }
    }

    private void releaseEgl() {
        if (egl == null) {
            return;
        }
        egl.eglDestroySurface(eglDisplay, eglSurface);
        egl.eglMakeCurrent(eglDisplay, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_CONTEXT);
        egl.eglDestroyContext(eglDisplay, eglContext);
        egl.eglTerminate(eglDisplay);

        egl = null;
        eglDisplay = null;
        eglContext = null;
        eglSurface = null;
    }


    GLImageDrawerHelper glImageDrawerHelper;
    EGLSurface eglSurface;
    EGLContext eglContext;
    EGLDisplay eglDisplay;
    EGL10 egl;

    @Override
    protected void onEnabled(boolean joining) throws ExoPlaybackException {
        super.onEnabled(joining);
        Log.d(TAG, "onEnabled: ");
        releaseEgl();
        tryInitEGL();
    }

    public void onSurfaceSizeChanged(int width, int height) {
        if (glImageDrawerHelper == null) {
            glImageDrawerHelper = new GLImageDrawerHelper();
        }
        surfaceChanged = viewW != width || viewH != height;
        clearedSurface = !surfaceChanged;
        viewW = width;
        viewH = height;
        glImageDrawerHelper.setViewSize(width, height);
    }

    private void tryInitEGL() {
        if (egl != null || surface == null) {
            return;
        }
        //1. 取得EGL实例
        egl = (EGL10) EGLContext.getEGL();
        //2. 选择Display
        eglDisplay = egl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);
        egl.eglInitialize(eglDisplay, null);

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
        egl.eglChooseConfig(eglDisplay, attribList, configs, configs.length, numConfigs);
        EGLConfig config = configs[0];

        //4. 创建Surface
        eglSurface = egl.eglCreateWindowSurface(eglDisplay, config, surface, new int[]{EGL14.EGL_NONE});
        //5. 创建Context
        eglContext = egl.eglCreateContext(eglDisplay, config, EGL10.EGL_NO_CONTEXT, new int[]{
                EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
                EGL10.EGL_NONE
        });
        if (glImageDrawerHelper == null) {
            glImageDrawerHelper = new GLImageDrawerHelper();
        }

        egl.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext);
    }

    @Override
    public boolean isReady() {
        return true;
    }

    @Override
    public boolean isEnded() {
        return isCurrentStreamFinal();
    }

    @Override
    protected void onStarted() throws ExoPlaybackException {
        super.onStarted();
        Log.d(TAG, "onStarted: ");
    }

    @Override
    protected void onStopped() throws ExoPlaybackException {
        super.onStopped();
        Log.d(TAG, "onStopped: ");
    }

    @Override
    public int supportsFormat(Format format) {
        String mimeType = format.sampleMimeType;
        if (mimeType != null && mimeType.startsWith("image/")) {
//            if ("image/gif".equals(mimeType)) {
//                return FORMAT_UNSUPPORTED_SUBTYPE;
//            } else {
            return FORMAT_HANDLED;
//            }
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
    private void drawBitmapToSurface(Bitmap bitmap) {
        if (egl == null) {
            return;
        }

        try {
            doDraw(bitmap);
        } catch (Exception e) {
            releaseEgl();
            tryInitEGL();
            doDraw(bitmap);
        }
    }

    private void doDraw(Bitmap bitmap) {
        if (bitmap == null || bitmap.isRecycled()) {
            return;
        }
        glImageDrawerHelper.drawToBitmap(bitmap);
        //6. 指定当前的环境为绘制环境
        egl.eglSwapBuffers(eglDisplay, eglSurface);
    }

    private static class BufferHolder {
        public long start;  //include
        public long end;   //exclude
        public boolean hasDraw;
        public DecoderInputBuffer buffer;

        public BufferHolder(long start, long end, DecoderInputBuffer buffer) {
            this.start = start;
            this.end = end;
            this.buffer = buffer;
            this.hasDraw = false;
        }

        public void setData(long start, long end, DecoderInputBuffer buffer) {
            this.start = start;
            this.end = end;
            this.buffer = buffer;
            this.hasDraw = false;
        }
    }
}
