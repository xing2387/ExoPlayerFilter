package com.daasuu.epf.player;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.Nullable;

import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.Renderer;
import com.google.android.exoplayer2.drm.DrmSessionManager;
import com.google.android.exoplayer2.drm.FrameworkMediaCrypto;
import com.google.android.exoplayer2.mediacodec.MediaCodecSelector;
import com.google.android.exoplayer2.text.TextOutput;
import com.google.android.exoplayer2.video.VideoRendererEventListener;

import java.util.ArrayList;

public class CustomRenderersFactory extends DefaultRenderersFactory {

//    private final int texId;


    @Override
    protected void buildVideoRenderers(
            Context context,
            @ExtensionRendererMode int extensionRendererMode,
            MediaCodecSelector mediaCodecSelector,
            @Nullable DrmSessionManager<FrameworkMediaCrypto> drmSessionManager,
            boolean playClearSamplesWithoutKeys,
            boolean enableDecoderFallback,
            Handler eventHandler,
            VideoRendererEventListener eventListener,
            long allowedVideoJoiningTimeMs,
            ArrayList<Renderer> out) {
        super.buildVideoRenderers(context,
                extensionRendererMode,
                mediaCodecSelector,
                drmSessionManager,
                playClearSamplesWithoutKeys,
                enableDecoderFallback,
                eventHandler,
                eventListener,
                allowedVideoJoiningTimeMs,
                out);
        out.add(new ImageRenderer(eventHandler, eventListener));
    }

    public CustomRenderersFactory(Context context) {
        super(context);
    }
}
