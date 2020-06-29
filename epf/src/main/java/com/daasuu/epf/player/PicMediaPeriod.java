package com.daasuu.epf.player;

import android.net.Uri;
import android.util.Log;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.FormatHolder;
import com.google.android.exoplayer2.SeekParameters;
import com.google.android.exoplayer2.decoder.DecoderInputBuffer;
import com.google.android.exoplayer2.source.MediaPeriod;
import com.google.android.exoplayer2.source.MediaSourceEventListener.EventDispatcher;
import com.google.android.exoplayer2.source.SampleStream;
import com.google.android.exoplayer2.source.TrackGroup;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.upstream.Loader;
import com.google.android.exoplayer2.upstream.StatsDataSource;
import com.google.android.exoplayer2.upstream.TransferListener;
import com.google.android.exoplayer2.util.Util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

public class PicMediaPeriod implements MediaPeriod, Loader.Callback<PicMediaPeriod.SourceLoadable> {
    private static final String TAG = "PicMediaPeriod";

    private static final Format format = Format.createImageSampleFormat(
            null,
            "image/jpeg",
            null,
            Format.NO_VALUE,
            C.SELECTION_FLAG_FORCED,
            null,
            null, null);


    private final ArrayList<SampleStreamImpl> sampleStreams;
    private final TrackGroupArray tracks;
    private EventDispatcher eventDispatcher;
    private DataSource.Factory dataSourceFactory;
    /* package */ boolean loadingFinished;
    /* package */ boolean loadingSucceeded;
    /* package */ byte[] sampleData;
    /* package */ int sampleSize;
    /* package */ final Loader loader;
    private final DataSpec dataSpec;
    private final TransferListener transferListener;
    private final long durationUs;

    public PicMediaPeriod(EventDispatcher eventDispatcher,
                          Uri uri, long durationUs,
                          DataSource.Factory dataSourceFactory,
                          TransferListener mediaTransferListener) {
        this.tracks = new TrackGroupArray(new TrackGroup(format));
        this.sampleStreams = new ArrayList<>();
        this.eventDispatcher = eventDispatcher;
        this.dataSourceFactory = dataSourceFactory;
        this.dataSpec = new DataSpec(uri, DataSpec.FLAG_ALLOW_GZIP);
        this.loader = new Loader("Loader:PicMediaPeriod");
        this.transferListener = mediaTransferListener;
        this.durationUs = durationUs;
    }

    @Override
    public void prepare(Callback callback, long positionUs) {
        Log.d(TAG, "prepare: " + Thread.currentThread().getName());
        callback.onPrepared(this);
    }

    public void release() {
        loader.release();
        eventDispatcher.mediaPeriodReleased();
    }

    @Override
    public void maybeThrowPrepareError() throws IOException {

    }

    @Override
    public TrackGroupArray getTrackGroups() {
        return tracks;
    }

    @Override
    public long selectTracks(TrackSelection[] selections, boolean[] mayRetainStreamFlags, SampleStream[] streams, boolean[] streamResetFlags, long positionUs) {
        Log.d(TAG, "selectTracks: " + Thread.currentThread().getName());
        for (int i = 0; i < selections.length; i++) {
            if (streams[i] != null && (selections[i] == null || !mayRetainStreamFlags[i])) {
                sampleStreams.remove(streams[i]);
                streams[i] = null;
            }
            if (streams[i] == null && selections[i] != null) {
                SampleStreamImpl stream = new SampleStreamImpl();
                sampleStreams.add(stream);
                streams[i] = stream;
                streamResetFlags[i] = true;
            }
        }
        return positionUs;
    }

    @Override
    public void discardBuffer(long positionUs, boolean toKeyframe) {
        Log.d(TAG, "discardBuffer: " + Thread.currentThread().getName());
    }

    @Override
    public long readDiscontinuity() {
        Log.d(TAG, "readDiscontinuity: " + Thread.currentThread().getName());
        return C.TIME_UNSET;
    }

    @Override
    public long seekToUs(long positionUs) {
        Log.d(TAG, "seekToUs: " + Thread.currentThread().getName());
        return positionUs;
    }

    @Override
    public long getAdjustedSeekPositionUs(long positionUs, SeekParameters seekParameters) {
        return positionUs;
    }

    @Override
    public long getBufferedPositionUs() {
        return 0;
    }

    @Override
    public long getNextLoadPositionUs() {
        return 0;
    }

    @Override
    public boolean continueLoading(long positionUs) {
        Log.d(TAG, "continueLoading: " + Thread.currentThread().getName());
        if (loadingFinished || loader.isLoading() || loader.hasFatalError()) {
            return false;
        }
        DataSource dataSource = dataSourceFactory.createDataSource();
        if (transferListener != null) {
            dataSource.addTransferListener(transferListener);
        }
        long elapsedRealtimeMs =
                loader.startLoading(
                        new SourceLoadable(dataSpec, dataSource),
                        /* callback= */ this,
                        0);
        eventDispatcher.loadStarted(
                dataSpec,
                C.DATA_TYPE_MEDIA,
                C.TRACK_TYPE_UNKNOWN,
                format,
                C.SELECTION_REASON_UNKNOWN,
                /* trackSelectionData= */ null,
                /* mediaStartTimeUs= */ 0,
                durationUs,
                elapsedRealtimeMs);
        return true;
    }

    @Override
    public void reevaluateBuffer(long positionUs) {
        Log.d(TAG, "reevaluateBuffer: " + Thread.currentThread().getName());
    }

    @Override
    public void onLoadCompleted(SourceLoadable loadable, long elapsedRealtimeMs, long loadDurationMs) {
        Log.d(TAG, "onLoadCompleted: ");
        sampleSize = (int) loadable.dataSource.getBytesRead();
        sampleData = loadable.sampleData;
        loadingFinished = true;
        loadingSucceeded = true;
        eventDispatcher.loadCompleted(
                loadable.dataSpec,
                loadable.dataSource.getLastOpenedUri(),
                loadable.dataSource.getLastResponseHeaders(),
                C.DATA_TYPE_MEDIA,
                C.TRACK_TYPE_UNKNOWN,
                format,
                C.SELECTION_REASON_UNKNOWN,
                /* trackSelectionData= */ null,
                /* mediaStartTimeUs= */ 0,
                durationUs,
                elapsedRealtimeMs,
                loadDurationMs,
                sampleSize);
    }

    @Override
    public void onLoadCanceled(SourceLoadable loadable, long elapsedRealtimeMs, long loadDurationMs, boolean released) {
        Log.d(TAG, "onLoadCanceled: ");
    }

    @Override
    public Loader.LoadErrorAction onLoadError(SourceLoadable loadable, long elapsedRealtimeMs, long loadDurationMs, IOException error, int errorCount) {
        Log.e(TAG, "onLoadError: ", error);
        return null;
    }

    private class SampleStreamImpl implements SampleStream {


        private static final int STREAM_STATE_SEND_FORMAT = 0;
        private static final int STREAM_STATE_SEND_SAMPLE = 1;
        private static final int STREAM_STATE_END_OF_STREAM = 2;

        private int streamState;
        private boolean notifiedDownstreamFormat;

        public void reset() {
            if (streamState == STREAM_STATE_END_OF_STREAM) {
                streamState = STREAM_STATE_SEND_SAMPLE;
            }
        }

        @Override
        public boolean isReady() {
            Log.d(TAG, "isReady: " + Thread.currentThread().getName());
            return loadingFinished;
        }

        @Override
        public void maybeThrowError() throws IOException {
//            if (!treatLoadErrorsAsEndOfStream) {
//                loader.maybeThrowError();
//            }
        }

        @Override
        public int readData(FormatHolder formatHolder, DecoderInputBuffer buffer,
                            boolean requireFormat) {
            Log.d(TAG, "readData: " + Thread.currentThread().getName());
            if (streamState == STREAM_STATE_END_OF_STREAM) {
                buffer.addFlag(C.BUFFER_FLAG_END_OF_STREAM);
                return C.RESULT_BUFFER_READ;
            } else if (requireFormat || streamState == STREAM_STATE_SEND_FORMAT) {
                formatHolder.format = format;
                streamState = STREAM_STATE_SEND_SAMPLE;
                return C.RESULT_FORMAT_READ;
            } else if (loadingFinished) {
                if (loadingSucceeded) {
                    buffer.addFlag(C.BUFFER_FLAG_KEY_FRAME);
                    buffer.timeUs = 0;
                    if (buffer.isFlagsOnly()) {
                        return C.RESULT_BUFFER_READ;
                    }
                    buffer.ensureSpaceForWrite(sampleSize);
                    buffer.data.put(sampleData, 0, sampleSize);
                } else {
                    buffer.addFlag(C.BUFFER_FLAG_END_OF_STREAM);
                }
                streamState = STREAM_STATE_END_OF_STREAM;
                return C.RESULT_BUFFER_READ;
            }
            return C.RESULT_NOTHING_READ;
        }

        @Override
        public int skipData(long positionUs) {
            Log.d(TAG, "skipData: " + Thread.currentThread().getName());
            if (positionUs > 0 && streamState != STREAM_STATE_END_OF_STREAM) {
                streamState = STREAM_STATE_END_OF_STREAM;
                return 1;
            }
            return 0;
        }

    }

    /* package */ static final class SourceLoadable implements Loader.Loadable {

        /**
         * The initial size of the allocation used to hold the sample data.
         */
        private static final int INITIAL_SAMPLE_SIZE = 1024;

        public final DataSpec dataSpec;

        private final StatsDataSource dataSource;

        private byte[] sampleData;

        public SourceLoadable(DataSpec dataSpec, DataSource dataSource) {
            this.dataSpec = dataSpec;
            this.dataSource = new StatsDataSource(dataSource);
        }

        @Override
        public void cancelLoad() {
            Log.d(TAG, "cancelLoad: " + Thread.currentThread().getName());
            // Never happens.
        }

        @Override
        public void load() throws IOException, InterruptedException {
            Log.d(TAG, "load: " + Thread.currentThread().getName());
            // We always load from the beginning, so reset bytesRead to 0.
            dataSource.resetBytesRead();
            try {
                // Create and open the input.
                dataSource.open(dataSpec);
                // Load the sample data.
                int result = 0;
                while (result != C.RESULT_END_OF_INPUT) {
                    int sampleSize = (int) dataSource.getBytesRead();
                    if (sampleData == null) {
                        sampleData = new byte[INITIAL_SAMPLE_SIZE];
                    } else if (sampleSize == sampleData.length) {
                        sampleData = Arrays.copyOf(sampleData, sampleData.length * 2);
                    }
                    result = dataSource.read(sampleData, sampleSize, sampleData.length - sampleSize);
                }
            } finally {
                Util.closeQuietly(dataSource);
            }
        }

    }
}
