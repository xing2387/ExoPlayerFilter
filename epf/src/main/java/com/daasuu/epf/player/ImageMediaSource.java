package com.daasuu.epf.player;


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

    private long mDurationUs;
    private Uri mUri;
    private final Timeline timeline;
    @Nullable
    private TransferListener transferListener;
    private final DataSource.Factory dataSourceFactory;


    public ImageMediaSource(DataSource.Factory dataSourceFactory, Uri uri, long durationUs) {
        this.dataSourceFactory = dataSourceFactory;
        this.mUri = uri;
        this.mDurationUs = durationUs;
        timeline = new SinglePeriodTimeline(durationUs,
                /* isSeekable= */ true, /* isDynamic= */ false, null);
    }

    @Nullable
    @Override
    public Object getTag() {
        return super.getTag();
    }

    @Override
    protected void prepareSourceInternal(@Nullable TransferListener mediaTransferListener) {
        Log.d(TAG, "prepareSourceInternal: " + Thread.currentThread().getName());
        transferListener = mediaTransferListener;
        refreshSourceInfo(timeline, /* manifest= */ null);
    }

    @Override
    public void maybeThrowSourceInfoRefreshError() throws IOException {

    }

    @Override
    public MediaPeriod createPeriod(MediaPeriodId id, Allocator allocator, long startPositionUs) {
        Log.d(TAG, "createPeriod: " + Thread.currentThread().getName());
        return new ImageMediaPeriod(createEventDispatcher(id), mUri, mDurationUs, startPositionUs, dataSourceFactory, transferListener);
    }

    @Override
    public void releasePeriod(MediaPeriod mediaPeriod) {
        ((ImageMediaPeriod) mediaPeriod).release();
        Log.d(TAG, "releasePeriod: " + Thread.currentThread().getName());
    }

    @Override
    protected void releaseSourceInternal() {
        Log.d(TAG, "releaseSourceInternal: " + Thread.currentThread().getName());
    }


}
