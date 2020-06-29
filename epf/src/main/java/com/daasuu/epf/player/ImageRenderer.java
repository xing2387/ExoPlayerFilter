package com.daasuu.epf.player;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.media.MediaCodec;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.util.Log;
import android.view.Surface;

import androidx.annotation.Nullable;

import com.google.android.exoplayer2.BaseRenderer;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.FormatHolder;
import com.google.android.exoplayer2.decoder.DecoderInputBuffer;
import com.google.android.exoplayer2.video.VideoFrameMetadataListener;

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
//                canvas.drawBitmap(bitmap, matrix, paint);
//                canvas.drawPicture();
                canvas.drawBitmap(bitmap, 0, 0, paint);
                surface.unlockCanvasAndPost(canvas);
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
}
