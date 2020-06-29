package com.daasuu.epf.player;

import android.content.Context;
import android.os.Looper;

import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.Renderer;
import com.google.android.exoplayer2.text.TextOutput;

import java.util.ArrayList;

public class CustomRenderersFactory extends DefaultRenderersFactory {

//    private final int texId;

    @Override
    protected void buildTextRenderers(Context context,
                                      TextOutput output,
                                      Looper outputLooper,
                                      int extensionRendererMode,
                                      ArrayList<Renderer> out) {
        super.buildTextRenderers(context, output, outputLooper, extensionRendererMode, out);
        out.add(new ImageRenderer());
    }

    public CustomRenderersFactory(Context context) {
        super(context);
    }
}
