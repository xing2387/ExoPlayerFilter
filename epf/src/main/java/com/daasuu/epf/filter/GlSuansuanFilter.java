package com.daasuu.epf.filter;

import android.opengl.GLES20;

public class GlSuansuanFilter extends GlFilter {

    private static final String SUANSUAN_FRAGMENT_SHADER = "" +
            "        precision mediump float;\n" +
            "        varying highp vec2 vTextureCoord;\n" +
            "        uniform float progress;\n" +
            "        uniform lowp sampler2D sTexture;\n" +
            "void main() {\n" +
            "        highp vec2 p = vTextureCoord;\n" +
            "        float duration = 0.7;\n" +
            "        float maxAlpha = 0.4;\n" +
            "        float maxScale = 1.8;\n" +
            "        float progressa = mod(progress, duration) / duration;\n" +
            "        float alphaA = maxAlpha * (1.0 - progressa);\n" +
            "        float scale = 1.0 + (maxScale - 1.0) * progressa;\n" +
            "        highp float weakX = 0.5 + (p.x - 0.5) / scale;\n" +
            "        highp float weakY = 0.5 + (p.y - 0.5) / scale;\n" +
            "        vec2 weakTextureCoords = vec2(weakX, weakY);\n" +
            "        vec4 weakMask = texture2D(sTexture, weakTextureCoords);\n" +
            "        vec4 mask = texture2D(sTexture, p);\n" +
            "        gl_FragColor = mask * (1.0 - alphaA) + weakMask * alphaA;\n" +
            "}";

    public GlSuansuanFilter() {
        super(DEFAULT_VERTEX_SHADER, SUANSUAN_FRAGMENT_SHADER);
    }

    private long ms = 0;

    @Override
    public void onDraw() {
        GLES20.glUniform1f(getHandle("progress"), ms++);
    }
}

