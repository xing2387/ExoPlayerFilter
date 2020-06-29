package com.daasuu.epf.player;


import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.Nullable;

import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.source.BaseMediaSource;
import com.google.android.exoplayer2.source.MediaPeriod;
import com.google.android.exoplayer2.source.SinglePeriodTimeline;
import com.google.android.exoplayer2.upstream.Allocator;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.TransferListener;

import java.io.IOException;

public class ImageMediaSource extends BaseMediaSource {
    private static final String TAG = "ImageMediaSource";

    private long mDuration;
    private Uri mUri;
    private final Timeline timeline;
    @Nullable
    private TransferListener transferListener;
    private final DataSource.Factory dataSourceFactory;


    public ImageMediaSource(DataSource.Factory dataSourceFactory, Uri uri, long duration) {
        this.dataSourceFactory = dataSourceFactory;
        this.mUri = uri;
        this.mDuration = duration;
        timeline = new SinglePeriodTimeline(duration,
                /* isSeekable= */ true, /* isDynamic= */ false, null);
    }

    @Override
    protected void prepareSourceInternal(@Nullable TransferListener mediaTransferListener) {
        transferListener = mediaTransferListener;
        Log.d(TAG, "prepareSourceInternal: " + Thread.currentThread().getName());
        refreshSourceInfo(timeline, /* manifest= */ null);
    }

    @Override
    protected void releaseSourceInternal() {
        Log.d(TAG, "releaseSourceInternal: " + Thread.currentThread().getName());
    }

    @Nullable
    @Override
    public Object getTag() {
        return super.getTag();
    }

    @Override
    public void maybeThrowSourceInfoRefreshError() throws IOException {

    }

    @Override
    public MediaPeriod createPeriod(MediaPeriodId id, Allocator allocator, long startPositionUs) {
        Log.d(TAG, "createPeriod: " + Thread.currentThread().getName());
        return new PicMediaPeriod(createEventDispatcher(id), mUri, mDuration, dataSourceFactory, transferListener);
    }

    @Override
    public void releasePeriod(MediaPeriod mediaPeriod) {
        ((PicMediaPeriod) mediaPeriod).release();
        Log.d(TAG, "releasePeriod: " + Thread.currentThread().getName());
    }
}
